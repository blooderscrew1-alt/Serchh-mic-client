package com.example.network

import android.content.Context
import android.os.BatteryManager
import android.provider.Settings
import android.util.Log
import com.example.data.ClientSettings
import com.example.voice.ClientMicState
import com.example.voice.ClientStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coordinador de Sintonía y Arbitraje de Nodos Mesh.
 *
 * Permite que múltiples satélites (teléfonos Android) distribuidos en la casa:
 * 1. Estén siempre en sintonía mediante descubrimientos y latidos periódicos (UDP 8999).
 * 2. Al escuchar la palabra de activación ("Música"), coordinen en tiempo real quién es
 *    el satélite que mejor escuchó al usuario (mayor nivel de señal RMS o menor latencia).
 * 3. Exactamente UN SOLO nodo capture y envíe la petición al Master, mientras los demás
 *    nodos cancelan automáticamente su captura de voz y regresan a standby.
 */
object MeshNodeCoordinator {
    private const val TAG = "MeshNodeCoordinator"
    const val MESH_PORT = 8999
    private const val ARBITRATION_WINDOW_MS = 2500L
    private const val SUPPRESSION_WINDOW_MS = 3200L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listenerJob: Job? = null
    private var heartbeatJob: Job? = null
    private var activeSocket: DatagramSocket? = null

    private val isRunning = AtomicBoolean(false)
    private var appContext: Context? = null

    @Volatile
    private var localNodeId: String = ""

    @Volatile
    private var localDeviceAlias: String = ""

    data class WakeClaim(
        val claimId: String,
        val nodeId: String,
        val nodeName: String,
        val timestamp: Long,
        val audioEnergy: Float,
        val spokenText: String = ""
    )

    @Volatile
    private var currentLocalClaim: WakeClaim? = null

    @Volatile
    private var lastPeerClaim: WakeClaim? = null

    @Volatile
    private var suppressLocalUntil: Long = 0L

    @Volatile
    private var lastWinnerName: String = ""

    // Callback para notificar al servicio de voz que debe cancelar la captura en curso
    private var onYieldListener: ((winnerName: String, reason: String) -> Unit)? = null

    sealed class ArbitrationDecision {
        data object Proceed : ArbitrationDecision()
        data class Yield(val winnerName: String, val reason: String) : ArbitrationDecision()
    }

    /**
     * Inicializa y arranca los servicios de malla (listener UDP y heartbeats).
     */
    fun start(context: Context) {
        appContext = context.applicationContext
        val settings = ClientSettings(context)
        localDeviceAlias = settings.deviceAlias

        if (localNodeId.isBlank()) {
            val androidId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)?.take(12)
            } catch (_: Exception) { null }
            localNodeId = "android-device-" + (androidId ?: UUID.randomUUID().toString().replace("-", "").take(8))
        }

        if (isRunning.getAndSet(true)) {
            Log.d(TAG, "Mesh services already active")
            return
        }

        Log.i(TAG, "Starting Mesh Node Coordinator on UDP port $MESH_PORT (nodeId=$localNodeId, alias=$localDeviceAlias)")
        startUdpListener()
        startPeriodicHeartbeats()
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return
        Log.i(TAG, "Stopping Mesh Node Coordinator")
        heartbeatJob?.cancel()
        heartbeatJob = null
        listenerJob?.cancel()
        listenerJob = null
        try {
            activeSocket?.close()
        } catch (_: Exception) {}
        activeSocket = null
        currentLocalClaim = null
        lastPeerClaim = null
        suppressLocalUntil = 0L
    }

    fun registerYieldListener(listener: (winnerName: String, reason: String) -> Unit) {
        this.onYieldListener = listener
    }

    fun unregisterYieldListener() {
        this.onYieldListener = null
    }

    /**
     * Llamado cuando este nodo detecta la palabra de activación ("Música", etc.).
     * Evalúa si este nodo debe proceder a capturar o si debe ceder ante otro satélite.
     */
    fun claimWakeWord(audioRms: Float, spokenText: String = ""): ArbitrationDecision {
        val context = appContext
        val settings = context?.let { ClientSettings(it) }
        val meshSyncEnabled = settings?.nodeMeshSyncEnabled ?: true

        if (!meshSyncEnabled) {
            // Sintonía mesh desactivada por usuario: procede siempre sin arbitraje
            return ArbitrationDecision.Proceed
        }

        val now = System.currentTimeMillis()

        // 1. Verificar si hay supresión activa debido a que otro nodo ya ganó recientemente
        val peerClaim = lastPeerClaim
        if (peerClaim != null && now < suppressLocalUntil && peerClaim.nodeId != localNodeId) {
            val reason = "Satélite '${peerClaim.nodeName}' ya está atendiendo la petición."
            Log.i(TAG, "Arbitration yield (active suppression): $reason")
            return ArbitrationDecision.Yield(peerClaim.nodeName, reason)
        }

        // 2. Si hay un reclamo reciente de un par dentro de la ventana de arbitraje (2.5s)
        if (peerClaim != null && (now - peerClaim.timestamp) < ARBITRATION_WINDOW_MS && peerClaim.nodeId != localNodeId) {
            val peerWins = shouldPeerWin(
                localEnergy = audioRms,
                localTimestamp = now,
                localId = localNodeId,
                peerEnergy = peerClaim.audioEnergy,
                peerTimestamp = peerClaim.timestamp,
                peerId = peerClaim.nodeId
            )

            if (peerWins) {
                suppressLocalUntil = now + SUPPRESSION_WINDOW_MS
                lastWinnerName = peerClaim.nodeName
                val reason = "Mayor intensidad/prioridad de señal en '${peerClaim.nodeName}'"
                Log.i(TAG, "Arbitration yield to recent peer: $reason")
                return ArbitrationDecision.Yield(peerClaim.nodeName, reason)
            }
        }

        // 3. Este nodo reclama la activación
        val claimId = "$localNodeId-$now"
        val localClaim = WakeClaim(
            claimId = claimId,
            nodeId = localNodeId,
            nodeName = localDeviceAlias.ifBlank { "Satélite Local" },
            timestamp = now,
            audioEnergy = audioRms.coerceAtLeast(1.0f),
            spokenText = spokenText
        )
        currentLocalClaim = localClaim

        Log.i(TAG, "NODE_WAKE_CLAIM broadcast: localNodeId=$localNodeId, rms=$audioRms")

        // Transmitir reclamo inmediato a todos los nodos de la malla
        broadcastClaim(localClaim)

        return ArbitrationDecision.Proceed
    }

    /**
     * Llamado cuando este nodo envía con éxito el comando al Host PC.
     * Notifica a la malla para liberar la palabra clave y sincronizar estado.
     */
    fun onCommandDispatched(commandType: String, songQuery: String) {
        val claim = currentLocalClaim ?: return
        currentLocalClaim = null

        val releaseMsg = RemoteNodeMessage(
            type = RemoteMessageType.NODE_WAKE_RELEASE,
            senderName = localDeviceAlias.ifBlank { "Satélite Local" },
            commandType = commandType,
            songQuery = songQuery,
            senderNodeId = localNodeId,
            claimId = claim.claimId,
            timestamp = System.currentTimeMillis()
        )
        sendUdpBroadcast(releaseMsg)
    }

    /**
     * Llamado si la captura en este nodo se canceló o expiró sin audio.
     */
    fun onCaptureCancelled() {
        val claim = currentLocalClaim ?: return
        currentLocalClaim = null

        val cancelMsg = RemoteNodeMessage(
            type = RemoteMessageType.NODE_WAKE_CANCEL,
            senderName = localDeviceAlias.ifBlank { "Satélite Local" },
            senderNodeId = localNodeId,
            claimId = claim.claimId,
            timestamp = System.currentTimeMillis()
        )
        sendUdpBroadcast(cancelMsg)
    }

    /**
     * Regla de arbitraje determinista entre dos satélites que escucharon la palabra clave:
     * 1. Nivel de energía de audio (RMS): el teléfono más cercano al usuario tiene señal más fuerte.
     *    Si la diferencia es > 15% o > 25 unidades RMS, gana el de mayor señal.
     * 2. Si los niveles son similares (dentro del 15% y 25 unidades): gana el satélite que lo detectó antes (> 40ms).
     * 3. Desempate determinista idéntico en ambos lados: comparación lexicográfica de nodeId.
     */
    private fun shouldPeerWin(
        localEnergy: Float,
        localTimestamp: Long,
        localId: String,
        peerEnergy: Float,
        peerTimestamp: Long,
        peerId: String
    ): Boolean {
        val energyDiff = peerEnergy - localEnergy
        val maxEnergy = maxOf(localEnergy, peerEnergy).coerceAtLeast(1.0f)
        val relativeDiff = kotlin.math.abs(energyDiff) / maxEnergy

        // 1. Dominancia clara de energía (usuario más cerca del otro satélite)
        if (relativeDiff > 0.15f || kotlin.math.abs(energyDiff) > 25f) {
            return energyDiff > 0f
        }

        // 2. Si energía es comparable, gana el que detectó primero
        val timeDiff = localTimestamp - peerTimestamp
        if (kotlin.math.abs(timeDiff) > 40L) {
            return timeDiff > 0L // peerTimestamp es menor (ocurrió antes)
        }

        // 3. Desempate idéntico en ambos nodos
        return peerId > localId
    }

    /**
     * Procesa un mensaje de red recibido desde otro satélite o el master.
     */
    fun handleIncomingMessage(msg: RemoteNodeMessage, senderIp: String) {
        if (msg.senderNodeId == localNodeId && localNodeId.isNotBlank()) {
            return // Ignorar eco propio
        }

        when (msg.type) {
            RemoteMessageType.NODE_WAKE_CLAIM -> {
                handlePeerWakeClaim(msg, senderIp)
            }

            RemoteMessageType.NODE_WAKE_CANCEL -> {
                if (lastPeerClaim?.claimId == msg.claimId || lastPeerClaim?.nodeId == msg.senderNodeId) {
                    lastPeerClaim = null
                    suppressLocalUntil = 0L
                    Log.d(TAG, "Peer ${msg.senderName} cancelled wake claim")
                }
            }

            RemoteMessageType.NODE_WAKE_RELEASE -> {
                if (lastPeerClaim?.claimId == msg.claimId || lastPeerClaim?.nodeId == msg.senderNodeId) {
                    lastPeerClaim = null
                    suppressLocalUntil = 0L
                }
                val info = if (msg.songQuery.isNotBlank()) "${msg.commandType}: ${msg.songQuery}" else msg.commandType
                ClientStateHolder.addLog("[Sintonía Mesh] '${msg.senderName}' completó petición al Master ($info)")
            }

            RemoteMessageType.PEER_HEARTBEAT -> {
                handlePeerHeartbeat(msg, senderIp)
            }

            RemoteMessageType.NODE_SYNC_STATE, RemoteMessageType.HOST_STATUS_UPDATE -> {
                syncPlaybackData(msg)
            }

            else -> {
                // Otros mensajes de control
            }
        }
    }

    private fun handlePeerWakeClaim(msg: RemoteNodeMessage, senderIp: String) {
        val now = System.currentTimeMillis()
        val peerClaim = WakeClaim(
            claimId = msg.claimId,
            nodeId = msg.senderNodeId.ifBlank { senderIp },
            nodeName = msg.senderName.ifBlank { "Satélite $senderIp" },
            timestamp = msg.timestamp,
            audioEnergy = msg.audioEnergy,
            spokenText = msg.rawSpokenText
        )
        lastPeerClaim = peerClaim

        val localClaim = currentLocalClaim

        if (localClaim != null && (now - localClaim.timestamp) < ARBITRATION_WINDOW_MS) {
            // Conflicto: AMBOS nodos detectaron la palabra casi al mismo tiempo!
            val peerWins = shouldPeerWin(
                localEnergy = localClaim.audioEnergy,
                localTimestamp = localClaim.timestamp,
                localId = localClaim.nodeId,
                peerEnergy = peerClaim.audioEnergy,
                peerTimestamp = peerClaim.timestamp,
                peerId = peerClaim.nodeId
            )

            if (peerWins) {
                // ESTE NODO PIERDE EL ARBITRAJE -> Cancelar captura inmediatamente!
                Log.i(TAG, "ARBITRATION LOST: Peer '${peerClaim.nodeName}' won (peerRMS=${peerClaim.audioEnergy} vs localRMS=${localClaim.audioEnergy}). Cancelling capture!")
                currentLocalClaim = null
                suppressLocalUntil = now + SUPPRESSION_WINDOW_MS
                lastWinnerName = peerClaim.nodeName

                ClientStateHolder.addLog("[Sintonía Mesh] Satélite '${peerClaim.nodeName}' tiene prioridad de audio (${peerClaim.audioEnergy.toInt()} vs ${localClaim.audioEnergy.toInt()}). Captura cancelada en este nodo.")
                ClientStateHolder.setMicState(ClientMicState.LISTENING_STANDBY, "🛰️ Sintonía Mesh: Cediendo a ${peerClaim.nodeName}")

                // Notificar al servicio de voz para abortar AudioRecord y Whisper
                onYieldListener?.invoke(peerClaim.nodeName, "Mayor señal en ${peerClaim.nodeName}")
            } else {
                // ESTE NODO GANA -> Re-transmitir reclamo para asegurar que el otro nodo cancele
                Log.i(TAG, "ARBITRATION WON: Local node won against '${peerClaim.nodeName}'! Re-broadcasting claim.")
                broadcastClaim(localClaim)
            }
        } else {
            // Este nodo estaba en standby o no tenía reclamo activo:
            // Activar supresión para no interrumpir al nodo ganador
            suppressLocalUntil = now + SUPPRESSION_WINDOW_MS
            lastWinnerName = peerClaim.nodeName
            Log.d(TAG, "Peer '${peerClaim.nodeName}' is handling wake word. Suppressing local wake word for ${SUPPRESSION_WINDOW_MS}ms")
        }
    }

    private fun handlePeerHeartbeat(msg: RemoteNodeMessage, senderIp: String) {
        val peerId = msg.senderNodeId.ifBlank { senderIp }
        val peerDevice = SatelliteDevice(
            id = peerId,
            name = msg.senderName.ifBlank { "Satélite $senderIp" },
            ip = senderIp,
            lastSeen = System.currentTimeMillis(),
            batteryPercent = msg.batteryPercent,
            isOnline = true
        )
        ClientStateHolder.updatePeerNode(peerDevice)
    }

    private fun syncPlaybackData(msg: RemoteNodeMessage) {
        if (msg.volume != null) {
            ClientStateHolder.setHostVolume(msg.volume)
        }
        if (msg.currentSong.isNotBlank() || msg.playbackState.isNotBlank() || msg.coverUrl.isNotBlank() || msg.upcomingJson.isNotBlank() || msg.playlistJson.isNotBlank()) {
            ClientStateHolder.syncHostMusicState(
                song = msg.currentSong.ifBlank { null },
                coverUrl = msg.coverUrl.ifBlank { null },
                upcomingJson = msg.upcomingJson.ifBlank { null },
                playlistJson = msg.playlistJson.ifBlank { null },
                playback = msg.playbackState.ifBlank { null },
                volume = msg.volume,
                isMuted = msg.isMuted,
                sender = msg.senderName
            )
        }
    }

    private fun broadcastClaim(claim: WakeClaim) {
        val claimMsg = RemoteNodeMessage(
            type = RemoteMessageType.NODE_WAKE_CLAIM,
            senderName = claim.nodeName,
            rawSpokenText = claim.spokenText,
            senderNodeId = claim.nodeId,
            claimId = claim.claimId,
            timestamp = claim.timestamp,
            audioEnergy = claim.audioEnergy
        )
        sendUdpBroadcast(claimMsg)
    }

    fun broadcastSyncNow(context: Context) {
        appContext = context.applicationContext
        val settings = ClientSettings(context)
        localDeviceAlias = settings.deviceAlias

        val battery = getBatteryPercentage(context)
        val heartbeatMsg = RemoteNodeMessage(
            type = RemoteMessageType.PEER_HEARTBEAT,
            senderName = localDeviceAlias.ifBlank { "Satélite Local" },
            senderNodeId = localNodeId,
            batteryPercent = battery,
            timestamp = System.currentTimeMillis()
        )
        sendUdpBroadcast(heartbeatMsg)
        ClientStateHolder.addLog("[Sintonía Mesh] Beacon de sincronización emitido a la red local")
    }

    private fun startPeriodicHeartbeats() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isRunning.get()) {
                val ctx = appContext
                if (ctx != null) {
                    val settings = ClientSettings(ctx)
                    localDeviceAlias = settings.deviceAlias
                    if (settings.nodeMeshSyncEnabled) {
                        val battery = getBatteryPercentage(ctx)
                        val heartbeat = RemoteNodeMessage(
                            type = RemoteMessageType.PEER_HEARTBEAT,
                            senderName = localDeviceAlias.ifBlank { "Satélite Local" },
                            senderNodeId = localNodeId,
                            batteryPercent = battery,
                            timestamp = System.currentTimeMillis()
                        )
                        sendUdpBroadcast(heartbeat)
                    }
                }
                delay(6000L) // cada 6 segundos
            }
        }
    }

    private fun startUdpListener() {
        listenerJob?.cancel()
        listenerJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(MESH_PORT))
                }
                activeSocket = socket
                val buffer = ByteArray(4096)
                val packet = DatagramPacket(buffer, buffer.size)

                Log.i(TAG, "UDP listener bound successfully to port $MESH_PORT")

                while (isActive && isRunning.get() && !socket.isClosed) {
                    socket.receive(packet)
                    val senderIp = packet.address?.hostAddress ?: ""
                    val rawJson = String(packet.data, 0, packet.length, Charsets.UTF_8)

                    val msg = RemoteNodeMessage.fromJson(rawJson)
                    if (msg != null) {
                        handleIncomingMessage(msg, senderIp)
                    }
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    Log.w(TAG, "UDP listener exception on port $MESH_PORT: ${e.message}")
                }
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }
        }
    }

    private fun sendUdpBroadcast(message: RemoteNodeMessage) {
        scope.launch {
            try {
                val jsonString = message.toJson()
                val data = jsonString.toByteArray(Charsets.UTF_8)
                val broadcastSocket = DatagramSocket().apply {
                    broadcast = true
                }

                // 1. Enviar a todas las direcciones de broadcast de subred
                val broadcastAddrs = NetworkHelper.getBroadcastAddresses()
                for (addr in broadcastAddrs) {
                    try {
                        val packet = DatagramPacket(data, data.size, addr, MESH_PORT)
                        broadcastSocket.send(packet)
                    } catch (e: Exception) {
                        Log.d(TAG, "Failed sending to broadcast addr $addr: ${e.message}")
                    }
                }

                // 2. Enviar unicast a IPs de pares conocidos (por si el router aísla broadcast)
                val knownPeers = ClientStateHolder.state.value.peerNodes
                for (peer in knownPeers) {
                    if (peer.ip.isNotBlank() && peer.ip != "127.0.0.1" && peer.ip != "0.0.0.0") {
                        try {
                            val peerAddr = InetAddress.getByName(peer.ip)
                            val unicastPacket = DatagramPacket(data, data.size, peerAddr, MESH_PORT)
                            broadcastSocket.send(unicastPacket)
                        } catch (_: Exception) {}
                    }
                }

                broadcastSocket.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error sending UDP mesh packet: ${e.message}")
            }
        }
    }

    private fun getBatteryPercentage(context: Context): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        } catch (_: Exception) {
            -1
        }
    }
}
