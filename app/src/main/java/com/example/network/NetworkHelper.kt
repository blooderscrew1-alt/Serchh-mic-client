package com.example.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.Collections

data class DiscoveredHost(
    val ip: String,
    val port: Int = 8998,
    val method: String = "Auto-Discovery",
    val latencyMs: Long = 0L
)

object NetworkHelper {
    private const val TAG = "NetworkHelper"
    const val DEFAULT_PORT_PRIMARY = 8998
    const val DEFAULT_PORT_SECONDARY = 3000
    val DEFAULT_PORTS = listOf(8998, 3000)
    const val DISCOVERY_PORT = 8999
    const val DISCOVERY_PROBE = "VOZMUSICA_DISCOVER_HOST"
    const val DISCOVERY_RESPONSE_PREFIX = "VOZMUSICA_HOST_HERE:"

    /**
     * Gets the actual local IPv4 address of the device on Wi-Fi / Hotspot.
     */
    fun getLocalIpAddress(context: Context? = null): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // First check Wi-Fi (wlan0, wlan1) or hotspot (ap0, softap)
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val name = intf.name.lowercase()
                if (name.contains("wlan") || name.contains("ap") || name.contains("eth") || name.contains("rndis")) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val hostAddress = addr.hostAddress ?: ""
                            if (hostAddress.isNotBlank() && !hostAddress.startsWith("127.")) {
                                return hostAddress
                            }
                        }
                    }
                }
            }

            // Fallback: check all other interfaces
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddress = addr.hostAddress ?: ""
                        if (hostAddress.isNotBlank() && !hostAddress.startsWith("127.")) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting local IP address: ${e.message}")
        }
        return "127.0.0.1"
    }

    /**
     * Retrieves all directed IPv4 subnet broadcast addresses as well as the fallback global 255.255.255.255.
     */
    fun getBroadcastAddresses(): List<InetAddress> {
        val list = mutableListOf<InetAddress>()
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (ia in intf.interfaceAddresses) {
                    val bcast = ia.broadcast
                    if (bcast != null && bcast is Inet4Address) {
                        list.add(bcast)
                    }
                }
            }
        } catch (_: Exception) {}
        try {
            val global = InetAddress.getByName("255.255.255.255")
            if (!list.contains(global)) {
                list.add(global)
            }
        } catch (_: Exception) {}
        return list
    }

    /**
     * Broadcasts UDP beacon so satellites on the same Wi-Fi can automatically discover the Master node without typing IP.
     */
    suspend fun sendBroadcastDiscoveryResponse(hostPort: Int) = withContext(Dispatchers.IO) {
        try {
            val socket = DatagramSocket()
            socket.broadcast = true
            val msg = "$DISCOVERY_RESPONSE_PREFIX$hostPort"
            val data = msg.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(
                data,
                data.size,
                InetAddress.getByName("255.255.255.255"),
                DISCOVERY_PORT
            )
            socket.send(packet)
            socket.close()
        } catch (e: Exception) {
            Log.w(TAG, "Discovery broadcast failed: ${e.message}")
        }
    }

    /**
     * Actively discovers the Master/Host PC or Mobile Host on the local network (Wi-Fi/Hotspot).
     * Ideal when power goes out, router reboots, or DHCP reassigns a new IP address.
     */
    suspend fun discoverHostOnLocalNetwork(
        preferredPort: Int = DEFAULT_PORT_PRIMARY,
        previousIp: String = "",
        onProgress: ((String) -> Unit)? = null
    ): DiscoveredHost? = withContext(Dispatchers.IO) {
        val cleanPreviousIp = previousIp.trim().replace("http://", "").replace("https://", "").replace("ws://", "").replace("wss://", "").split(":").firstOrNull() ?: ""

        // Los puertos por defecto del host son 8998 y 3000.
        // Se busca en ambos puertos por defecto (y el puerto preferido si se ha personalizado).
        val candidatePorts = linkedSetOf<Int>().apply {
            add(preferredPort)
            add(DEFAULT_PORT_PRIMARY) // 8998
            add(DEFAULT_PORT_SECONDARY) // 3000
        }.toList()

        // 1. FAST CHECK: Probar la IP anterior conocida en los puertos por defecto (8998 y 3000)
        if (cleanPreviousIp.isNotBlank() && cleanPreviousIp != "127.0.0.1" && !cleanPreviousIp.contains("localhost")) {
            for (port in candidatePorts) {
                onProgress?.invoke("Probando IP guardada ($cleanPreviousIp:$port)...")
                val t0 = System.currentTimeMillis()
                if (testTcpPort(cleanPreviousIp, port, timeoutMs = 380)) {
                    val latency = System.currentTimeMillis() - t0
                    Log.i(TAG, "Reconnected to previous host IP $cleanPreviousIp:$port in ${latency}ms")
                    return@withContext DiscoveredHost(
                        ip = cleanPreviousIp,
                        port = port,
                        method = "IP Guardada ($port)",
                        latencyMs = latency
                    )
                }
            }
        }

        // 2. UDP BROADCAST DISCOVERY PROBE
        onProgress?.invoke("Enviando baliza UDP de autodescubrimiento...")
        val udpResult = withTimeoutOrNull(750L) {
            probeUdpBroadcast(preferredPort, candidatePorts)
        }
        if (udpResult != null) {
            Log.i(TAG, "Found Host via UDP Broadcast: ${udpResult.ip}:${udpResult.port}")
            return@withContext udpResult
        }

        // 3. PARALLEL SUBNET SCAN (1..254) buscando en los puertos por defecto (8998 y 3000)
        val localIp = getLocalIpAddress()
        if (localIp == "127.0.0.1" || !localIp.contains(".")) {
            Log.w(TAG, "Cannot perform subnet scan without valid local Wi-Fi IP (current: $localIp)")
            return@withContext null
        }

        val lastDotIndex = localIp.lastIndexOf('.')
        val subnetPrefix = localIp.substring(0, lastDotIndex + 1) // e.g. "192.168.1."
        val myLastOctet = localIp.substring(lastDotIndex + 1).toIntOrNull() ?: -1

        onProgress?.invoke("Escaneando subred $subnetPrefix* (puertos 8998 y 3000)...")

        // Prioritize likely host IPs (Gateway, common DHCP pools, then remaining)
        val prioritizedOctets = mutableListOf<Int>()
        val highPriority = listOf(1, 100, 101, 102, 103, 104, 105, 2, 3, 4, 5, 10, 20, 50, 150, 200, 250)
        for (p in highPriority) {
            if (p != myLastOctet && p in 1..254) {
                prioritizedOctets.add(p)
            }
        }
        for (i in 1..254) {
            if (i != myLastOctet && !prioritizedOctets.contains(i)) {
                prioritizedOctets.add(i)
            }
        }

        val foundResult = CompletableDeferred<DiscoveredHost?>()

        coroutineScope {
            // Scan in batches of 32 to avoid thread pool exhaustion
            val chunks = prioritizedOctets.chunked(32)
            for (chunk in chunks) {
                if (foundResult.isCompleted) break

                val jobs = chunk.map { octet ->
                    val candidateIp = "$subnetPrefix$octet"
                    async(Dispatchers.IO) {
                        if (foundResult.isCompleted) return@async
                        // Probar los puertos por defecto (8998 y 3000, más el preferido)
                        for (port in candidatePorts) {
                            if (foundResult.isCompleted) return@async
                            val t0 = System.currentTimeMillis()
                            if (testTcpPort(candidateIp, port, timeoutMs = 280)) {
                                val latency = System.currentTimeMillis() - t0
                                foundResult.complete(
                                    DiscoveredHost(
                                        ip = candidateIp,
                                        port = port,
                                        method = "Escaneo de Red ($port)",
                                        latencyMs = latency
                                    )
                                )
                                return@async
                            }
                        }
                    }
                }
                jobs.awaitAll()
            }
        }

        if (foundResult.isCompleted) {
            val host = foundResult.getCompleted()
            if (host != null) {
                Log.i(TAG, "Discovered Host via Subnet Scan: ${host.ip}:${host.port}")
                return@withContext host
            }
        }

        Log.w(TAG, "No Host found on local network")
        return@withContext null
    }

    private fun testTcpPort(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeoutMs)
            socket.close()
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun probeUdpBroadcast(
        defaultPort: Int,
        candidatePorts: List<Int> = DEFAULT_PORTS
    ): DiscoveredHost? = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = 650

            val probeData = DISCOVERY_PROBE.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(
                probeData,
                probeData.size,
                InetAddress.getByName("255.255.255.255"),
                DISCOVERY_PORT
            )
            val t0 = System.currentTimeMillis()
            socket.send(packet)

            val buffer = ByteArray(1024)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(receivePacket)

            val latency = System.currentTimeMillis() - t0
            val responseText = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8).trim()
            val responderIp = receivePacket.address.hostAddress ?: ""

            if (responderIp.isNotBlank() && responderIp != "127.0.0.1") {
                var detectedPort = defaultPort
                if (responseText.startsWith(DISCOVERY_RESPONSE_PREFIX)) {
                    val parsedPort = responseText.removePrefix(DISCOVERY_RESPONSE_PREFIX).trim().toIntOrNull()
                    if (parsedPort != null && parsedPort > 0) {
                        detectedPort = parsedPort
                    }
                } else {
                    // Verificar cuál de los puertos por defecto (8998 o 3000) responde en el host
                    for (cp in candidatePorts) {
                        if (testTcpPort(responderIp, cp, timeoutMs = 250)) {
                            detectedPort = cp
                            break
                        }
                    }
                }
                return@withContext DiscoveredHost(
                    ip = responderIp,
                    port = detectedPort,
                    method = "Baliza UDP ($detectedPort)",
                    latencyMs = latency
                )
            }
        } catch (_: Exception) {
            // UDP broadcast timeout or unreachable
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
        return@withContext null
    }
}

