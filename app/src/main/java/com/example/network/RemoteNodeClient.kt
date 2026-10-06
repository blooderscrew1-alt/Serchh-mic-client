package com.example.network

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.example.voice.ClientMicState
import com.example.voice.ClientStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class RemoteNodeClient(
    private var senderDeviceName: String,
    private val context: Context? = null
) {
    private val TAG = "RemoteNodeClient"
    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var activeWebSocket: WebSocket? = null
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null
    private var meshSyncJob: Job? = null
    private var meshBroadcastJob: Job? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _statusMessage = MutableStateFlow("Desconectado")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _lastAck = MutableStateFlow<String?>(null)
    val lastAck: StateFlow<String?> = _lastAck.asStateFlow()

    private val _pingLatency = MutableStateFlow<Long>(-1)
    val pingLatency: StateFlow<Long> = _pingLatency.asStateFlow()

    @Volatile
    private var lastPingSendTime = 0L

    @Volatile
    private var currentWsUrl: String = ""

    @Volatile
    var currentRoom: String = "serchtube-master"

    // Unique node ID e.g. "android-device-[ID_UNICO]"
    val nodeId: String by lazy {
        val uniquePart = try {
            if (context != null) {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)?.take(12)
            } else null
        } catch (_: Exception) { null } ?: UUID.randomUUID().toString().replace("-", "").take(8)

        "android-device-$uniquePart"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for WebSocket
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    fun updateSenderName(name: String) {
        senderDeviceName = name
    }

    companion object {
        fun buildWebSocketUrl(rawInput: String, defaultPort: Int = 8998): String {
            val input = rawInput.trim()
            if (input.isBlank()) return ""

            var url = input

            // 1. If complete wss:// or ws:// URL
            if (url.startsWith("wss://", ignoreCase = true) || url.startsWith("ws://", ignoreCase = true)) {
                if (!url.contains("/ws") && !url.contains("?")) {
                    url = if (url.endsWith("/")) "${url}ws" else "$url/ws"
                }
                return url
            }

            // 2. If https:// or http:// URL
            if (url.startsWith("https://", ignoreCase = true)) {
                url = "wss://" + url.removePrefix("https://")
            } else if (url.startsWith("http://", ignoreCase = true)) {
                url = "ws://" + url.removePrefix("http://")
            } else {
                // 3. Raw host/IP: Cloud Run / domain vs local IP
                val isDomain = url.contains(".run.app") || url.contains(".com") ||
                               url.contains(".net") || url.contains(".io") ||
                               url.contains(".ngrok") || url.contains(".trycloudflare")

                if (isDomain) {
                    url = "wss://$url"
                } else {
                    if (!url.contains(":")) {
                        url = "ws://$url:$defaultPort"
                    } else {
                        url = "ws://$url"
                    }
                }
            }

            if (!url.contains("/ws") && !url.contains("?")) {
                url = if (url.endsWith("/")) "${url}ws" else "$url/ws"
            }
            return url
        }

        fun parseHost(rawHost: String): String {
            return rawHost.trim()
                .removePrefix("http://")
                .removePrefix("https://")
                .removePrefix("ws://")
                .removePrefix("wss://")
                .substringBefore(":")
                .substringBefore("/")
                .trim()
        }

        fun parsePort(rawHost: String, defaultPort: Int = 8998): Int {
            val cleaned = rawHost.trim()
                .removePrefix("http://")
                .removePrefix("https://")
                .removePrefix("ws://")
                .removePrefix("wss://")
                .substringBefore("/")
            if (cleaned.contains(":")) {
                val portStr = cleaned.substringAfterLast(":").trim()
                return portStr.toIntOrNull() ?: defaultPort
            }
            return defaultPort
        }
    }

    /**
     * Connect to SerchTube Music PC Master server via WebSocket (wss:// or ws://)
     */
    fun connect(rawHost: String, defaultPort: Int = 8998, room: String = "serchtube-master") {
        disconnectInternal(keepConnectingState = true)

        if (room.isNotBlank()) {
            currentRoom = room.trim()
        }

        val wsUrl = buildWebSocketUrl(rawHost, defaultPort)
        if (wsUrl.isBlank()) {
            _statusMessage.value = "Ingresa la URL o IP del Master"
            ClientStateHolder.setConnectionState(false, false, "Falta configurar URL del Master")
            return
        }

        currentWsUrl = wsUrl
        _isConnecting.value = true
        _statusMessage.value = "Conectando con PC ($currentRoom)..."
        ClientStateHolder.setConnectionState(false, true, "Conectando con PC ($currentRoom)...")
        ClientStateHolder.addLog("Iniciando WebSocket hacia: $wsUrl (Sala: $currentRoom)")

        try {
            val request = Request.Builder()
                .url(wsUrl)
                .build()

            activeWebSocket = okHttpClient.newWebSocket(request, createWebSocketListener(wsUrl))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate WebSocket connection to $wsUrl", e)
            val errorMsg = "Error al iniciar WebSocket: ${e.localizedMessage ?: e.message}"
            ClientStateHolder.addLog(errorMsg, isError = true)
            handleConnectionLoss(errorMsg)
        }
    }

    private fun createWebSocketListener(wsUrl: String): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully to $wsUrl")
                _isConnected.value = true
                _isConnecting.value = false

                val nodeAlias = senderDeviceName.ifBlank { "Android Satélite" }
                val statusText = "🟢 ¡Vinculado con éxito! ($wsUrl)"
                _statusMessage.value = statusText
                ClientStateHolder.setConnectionState(true, false, statusText)
                ClientStateHolder.addLog("Conexión WebSocket establecida con Master ($wsUrl)", isIncoming = true)

                // 1. REGLA 1: REGISTRO INICIAL OBLIGATORIO
                val registerMsg = JSONObject().apply {
                    put("type", "register_satellite")
                    put("role", "satellite")
                    put("nodeId", nodeId)
                    put("nodeName", nodeAlias)
                    put("room", currentRoom)
                }
                webSocket.send(registerMsg.toString())
                ClientStateHolder.addLog("Registro enviado: nodeId=$nodeId, name=$nodeAlias, room=$currentRoom")

                // Start 20-30s PING loop (REGLA 4)
                startPingLoop()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WS Received message: $text")
                handleIncomingWsMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WS Closing code=$code reason=$reason")
                _isConnected.value = false
                ClientStateHolder.addLog("WebSocket cerrándose: código $code ($reason)")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WS Closed code=$code reason=$reason")
                val closedMsg = if (reason.isNotBlank()) "Cerrado ($code: $reason)" else "Cerrado ($code)"
                ClientStateHolder.addLog("WebSocket cerrado: $closedMsg", isError = true)
                handleConnectionLoss(closedMsg)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val respCode = response?.code
                val respMsg = response?.message
                val errorDetails = buildString {
                    append(t.localizedMessage ?: t.javaClass.simpleName)
                    if (respCode != null) append(" (HTTP $respCode $respMsg)")
                }
                Log.e(TAG, "WS Failure: $errorDetails", t)
                ClientStateHolder.addLog("Fallo WebSocket: $errorDetails en $wsUrl", isError = true)
                handleConnectionLoss("Error de conexión: $errorDetails")
            }
        }
    }

    /**
     * REGLA 4: PING / PONG mantenimiento de conexión cada 20 segundos
     */
    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive && _isConnected.value) {
                delay(20000L) // Ping every 20 seconds
                val ws = activeWebSocket ?: break
                try {
                    val pingJson = JSONObject().apply {
                        put("type", "ping")
                    }
                    lastPingSendTime = System.currentTimeMillis()
                    ws.send(pingJson.toString())
                } catch (e: Exception) {
                    Log.w(TAG, "Error sending ping: ${e.message}")
                    break
                }
            }
        }
    }

    /**
     * REGLA 3: EVENTO DE ACTIVIDAD DE VOZ (MIC EVENT)
     * Para que el orbe visual del Master reaccione cuando el usuario habla en Android
     */
    fun sendMicEvent(speechActive: Boolean) {
        val ws = activeWebSocket
        if (_isConnected.value && ws != null) {
            scope.launch {
                try {
                    val micJson = JSONObject().apply {
                        put("type", "satellite_mic_event")
                        put("nodeId", nodeId)
                        put("speechActive", speechActive)
                    }
                    ws.send(micJson.toString())
                } catch (e: Exception) {
                    Log.w(TAG, "Error sending satellite_mic_event: ${e.message}")
                }
            }
        }
    }

    fun sendSignalState(
        isReceiving: Boolean,
        details: String = "",
        hostIp: String = "",
        port: Int = 8998
    ) {
        sendMicEvent(isReceiving)
    }

    /**
     * Envía al instante y con cero retardos el comando "activa tu micrófono" al Host
     * en cuanto el teléfono detecta la palabra clave, sin grabar, transcribir ni procesar peticiones locales.
     */
    fun sendInstantHostMicActivation(
        hostIp: String,
        port: Int,
        wakeWord: String = "Música"
    ) {
        val now = System.currentTimeMillis()
        val commandJson = JSONObject().apply {
            put("type", "command")
            put("commandType", "ACTIVATE_MIC")
            put("action", "activate_mic")
            put("songQuery", "activa tu micrófono")
            put("rawSpokenText", "activa tu micrófono")
            put("activateMic", true)
            put("payload", JSONObject().apply {
                put("action", "activate_mic")
                put("command", "activa_tu_microfono")
                put("commandType", "ACTIVATE_MIC")
                put("query", "activa tu micrófono")
                put("rawSpokenText", "activa tu micrófono")
                put("activateMic", true)
                put("hostMicActive", true)
                put("startListening", true)
                put("wakeWord", wakeWord)
            })
            put("nodeId", nodeId)
            put("senderName", senderDeviceName.ifBlank { "Android Satélite" })
            put("timestamp", now)
        }
        val payloadStr = commandJson.toString()

        // 1. Vía ultra-rápida síncrona (0ms latencia) si el WebSocket ya está conectado
        val ws = activeWebSocket
        if (_isConnected.value && ws != null) {
            try {
                val enqueued = ws.send(payloadStr)
                if (enqueued) {
                    _statusMessage.value = "⚡ Enviado al instante: Activa tu micrófono"
                    ClientStateHolder.addLog("⚡ [Instantáneo] Comando 'activa tu micrófono' enviado al Host por WebSocket")
                    MeshNodeCoordinator.onCommandDispatched("activate_mic", "activa tu micrófono")
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fast WS send failed for activate_mic, falling back to IO socket: ${e.message}")
            }
        }

        // 2. Fallback inmediato por socket TCP con tcpNoDelay = true
        scope.launch(Dispatchers.IO) {
            sendVoiceCommand(
                commandType = "ACTIVATE_MIC",
                songQuery = "activa tu micrófono",
                rawSpokenText = "activa tu micrófono",
                hostIp = hostIp,
                port = port
            )
        }
    }

    /**
     * REGLA 2: ENVÍO DE COMANDOS DE VOZ / TÁCTILES AL MASTER
     * Formato JSON obligatorio:
     * {
     *   "type": "command",
     *   "payload": {
     *     "action": "play_query", // o skip, pause, volume, etc.
     *     "query": "Nombre del artista o canción buscada"
     *   }
     * }
     */
    suspend fun sendVoiceCommand(
        commandType: String,
        songQuery: String,
        rawSpokenText: String,
        hostIp: String,
        port: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val currentVol15 = ClientStateHolder.state.value.hostVolume.coerceIn(1, 15)

        val targetVol100: Int?
        val targetVol15: Int?

        when {
            commandType == "VOLUME_MAX" -> {
                targetVol100 = 100
                targetVol15 = 15
            }
            commandType == "VOLUME_HIGH" -> {
                targetVol100 = 80
                targetVol15 = 12
            }
            commandType == "VOLUME_MEDIUM" -> {
                targetVol100 = 50
                targetVol15 = 8
            }
            commandType == "VOLUME_LOW" -> {
                targetVol100 = 20
                targetVol15 = 3
            }
            commandType == "MUTE" -> {
                targetVol100 = 0
                targetVol15 = 0
            }
            commandType == "VOLUME_UP" -> {
                val next15 = (currentVol15 + 1).coerceIn(1, 15)
                targetVol15 = next15
                targetVol100 = ((next15 / 15.0) * 100.0).roundToInt().coerceIn(1, 100)
            }
            commandType == "VOLUME_DOWN" -> {
                val prev15 = (currentVol15 - 1).coerceIn(1, 15)
                targetVol15 = prev15
                targetVol100 = ((prev15 / 15.0) * 100.0).roundToInt().coerceIn(1, 100)
            }
            commandType.startsWith("VOLUME_SET_LEVEL_") -> {
                val lvl = commandType.removePrefix("VOLUME_SET_LEVEL_").toIntOrNull()?.coerceIn(1, 15) ?: 10
                targetVol15 = lvl
                targetVol100 = ((lvl / 15.0) * 100.0).roundToInt().coerceIn(1, 100)
            }
            commandType.startsWith("VOLUME_SET_PCT_") -> {
                val pct = commandType.removePrefix("VOLUME_SET_PCT_").toIntOrNull()?.coerceIn(0, 100) ?: 50
                targetVol100 = pct
                targetVol15 = if (pct == 0) 0 else ((pct / 100.0) * 15.0).roundToInt().coerceIn(1, 15)
            }
            commandType.startsWith("VOLUME_SET_") -> {
                val num = commandType.removePrefix("VOLUME_SET_").toIntOrNull() ?: songQuery.toIntOrNull()
                if (num != null) {
                    if (num in 1..15) {
                        targetVol15 = num
                        targetVol100 = ((num / 15.0) * 100.0).roundToInt().coerceIn(1, 100)
                    } else {
                        val pct = num.coerceIn(0, 100)
                        targetVol100 = pct
                        targetVol15 = if (pct == 0) 0 else ((pct / 100.0) * 15.0).roundToInt().coerceIn(1, 15)
                    }
                } else {
                    targetVol100 = null
                    targetVol15 = null
                }
            }
            songQuery.toIntOrNull() != null -> {
                val num = songQuery.toInt()
                if (num in 1..15) {
                    targetVol15 = num
                    targetVol100 = ((num / 15.0) * 100.0).roundToInt().coerceIn(1, 100)
                } else {
                    val pct = num.coerceIn(0, 100)
                    targetVol100 = pct
                    targetVol15 = if (pct == 0) 0 else ((pct / 100.0) * 15.0).roundToInt().coerceIn(1, 15)
                }
            }
            else -> {
                targetVol100 = null
                targetVol15 = null
            }
        }

        val action = when {
            commandType == "ACTIVATE_MIC" || commandType == "ACTIVATE_HOST_MIC" -> "activate_mic"
            commandType == "SEARCH_PLAY" -> "play_query"
            commandType == "PAUSE" -> "pause"
            commandType == "PLAY" -> if (songQuery.isNotBlank()) "play_query" else "play"
            commandType == "RESUME" -> "resume"
            commandType == "STOP" -> "stop"
            commandType == "NEXT" || commandType == "SKIP" -> "skip"
            commandType == "PREVIOUS" -> "previous"
            commandType == "MUTE" -> "mute"
            commandType == "UNMUTE" -> "unmute"
            commandType == "VOLUME_UP" -> "volume_up"
            commandType == "VOLUME_DOWN" -> "volume_down"
            commandType.startsWith("VOLUME_SET_") || commandType.startsWith("VOLUME_") -> "volume"
            else -> commandType.lowercase()
        }

        val query = when {
            action == "activate_mic" -> "activa tu micrófono"
            action == "play_query" -> songQuery
            action == "volume" -> targetVol100?.toString() ?: songQuery
            action == "volume_up" -> targetVol100?.toString() ?: "up"
            action == "volume_down" -> targetVol100?.toString() ?: "down"
            else -> if (songQuery.isNotBlank()) songQuery else rawSpokenText
        }

        val commandJson = JSONObject().apply {
            put("type", "command")
            if (action == "activate_mic") {
                put("commandType", "ACTIVATE_MIC")
                put("action", "activate_mic")
                put("rawSpokenText", "activa tu micrófono")
                put("activateMic", true)
            }
            put("payload", JSONObject().apply {
                put("action", action)
                put("query", query)
                if (action == "activate_mic") {
                    put("command", "activa_tu_microfono")
                    put("commandType", "ACTIVATE_MIC")
                    put("rawSpokenText", "activa tu micrófono")
                    put("activateMic", true)
                    put("hostMicActive", true)
                    put("startListening", true)
                }
                if (targetVol100 != null) {
                    put("value", targetVol100)
                    put("volume", targetVol100)
                    put("level", targetVol100)
                    put("percent", targetVol100)
                    put("vol100", targetVol100)
                    put("vol15", targetVol15 ?: 10)
                }
                if (action == "mute") put("isMuted", true)
                if (action == "unmute") put("isMuted", false)
            })
            put("nodeId", nodeId)
            put("senderName", senderDeviceName.ifBlank { "Android Satélite" })
            put("timestamp", System.currentTimeMillis())
        }

        val displayDesc = when {
            action == "activate_mic" -> "⚡ Activa tu micrófono (Host)"
            songQuery.isNotBlank() -> "Buscar: '$songQuery'"
            action != "command" -> "Acción: $action (${query.ifBlank { "ejecutar" }})"
            else -> "Voz: '$rawSpokenText'"
        }

        // Send over WebSocket if active
        val ws = activeWebSocket
        if (_isConnected.value && ws != null) {
            try {
                ws.send(commandJson.toString())
                _statusMessage.value = "Enviado: $displayDesc"
                ClientStateHolder.addLog("Comando WebSocket enviado ($action: '$query')")
                MeshNodeCoordinator.onCommandDispatched(action, query)
                return@withContext true
            } catch (e: Exception) {
                Log.w(TAG, "Error sending WS command, fallback to TCP/HTTP: ${e.message}")
            }
        }

        // Fallback: direct connection or TCP/HTTP fallback if WebSocket temporarily disconnected
        val cleanIp = parseHost(hostIp)
        val cleanPort = parsePort(hostIp, port)
        if (cleanIp.isBlank()) {
            val errStr = "Falta configurar la URL o IP del Master"
            _statusMessage.value = errStr
            ClientStateHolder.addLog(errStr, isError = true)
            return@withContext false
        }

        try {
            val socket = Socket()
            socket.tcpNoDelay = true
            socket.soTimeout = 4000
            socket.connect(InetSocketAddress(cleanIp, cleanPort), 3000)

            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))

            writer.println(commandJson.toString())
            val ackLine = reader.readLine()
            try { socket.close() } catch (_: Exception) {}

            _statusMessage.value = "Enviado directo: $displayDesc"
            ClientStateHolder.addLog("Comando enviado vía socket directo a $cleanIp:$cleanPort")
            MeshNodeCoordinator.onCommandDispatched(action, query)
            return@withContext true
        } catch (e: Exception) {
            val errMsg = "Error al conectar con Master ($cleanIp:$cleanPort)"
            _lastAck.value = errMsg
            _statusMessage.value = errMsg
            ClientStateHolder.updateState {
                it.copy(
                    lastAckMessage = errMsg,
                    micState = ClientMicState.COMMAND_ERROR
                )
            }
            ClientStateHolder.addLog(errMsg, isError = true)
            return@withContext false
        }
    }

    suspend fun testPing(hostIp: String, port: Int): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        val ws = activeWebSocket
        if (_isConnected.value && ws != null) {
            val t0 = System.currentTimeMillis()
            lastPingSendTime = t0
            val pingJson = JSONObject().apply { put("type", "ping") }
            ws.send(pingJson.toString())
            delay(300L)
            val latency = if (_pingLatency.value > 0) _pingLatency.value else (System.currentTimeMillis() - t0)
            ClientStateHolder.addLog("Ping WebSocket a Master: ${latency}ms")
            return@withContext Pair(true, latency)
        }

        // Direct ping test fallback
        val cleanIp = parseHost(hostIp)
        val cleanPort = parsePort(hostIp, port)
        if (cleanIp.isBlank()) return@withContext Pair(false, -1L)

        val t0 = System.currentTimeMillis()
        try {
            val socket = Socket()
            socket.tcpNoDelay = true
            socket.soTimeout = 3000
            socket.connect(InetSocketAddress(cleanIp, cleanPort), 2500)
            val latency = System.currentTimeMillis() - t0
            try { socket.close() } catch (_: Exception) {}
            _pingLatency.value = latency
            ClientStateHolder.updateState { it.copy(pingLatencyMs = latency) }
            ClientStateHolder.addLog("Ping TCP a $cleanIp:$cleanPort: ${latency}ms")
            return@withContext Pair(true, latency)
        } catch (e: Exception) {
            ClientStateHolder.addLog("Fallo de ping a $cleanIp:$cleanPort (${e.message})", isError = true)
            return@withContext Pair(false, -1L)
        }
    }

    private fun handleIncomingWsMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type", "")

            when (type) {
                "pong", "PONG" -> {
                    if (lastPingSendTime > 0) {
                        val latency = (System.currentTimeMillis() - lastPingSendTime).coerceAtLeast(0)
                        _pingLatency.value = latency
                        ClientStateHolder.updateState { it.copy(pingLatencyMs = latency) }
                    }
                }
                "ack", "command_ack", "COMMAND_ACK" -> {
                    val msg = json.optString("message", json.optString("status", "Comando recibido por Master"))
                    _lastAck.value = msg
                    _statusMessage.value = msg
                    ClientStateHolder.updateState {
                        it.copy(
                            lastAckMessage = msg,
                            micState = ClientMicState.COMMAND_SUCCESS
                        )
                    }
                    ClientStateHolder.addLog("Respuesta Master: $msg", isIncoming = true)
                }
                "playback_state", "player_state", "playbackState", "media_state", "state_sync", "sync_state", "status_update", "queue_update", "playlist_update", "queue", "upcoming", "now_playing", "nowplaying", "track_change", "media_sync" -> {
                    val payload = json.optJSONObject("payload") ?: json
                    val currentTrackObj = payload.optJSONObject("currentTrack") ?: payload.optJSONObject("track")
                    val track = when {
                        currentTrackObj != null -> {
                            val title = currentTrackObj.optString("title",
                                currentTrackObj.optString("song",
                                    currentTrackObj.optString("name",
                                        currentTrackObj.optString("track",
                                            currentTrackObj.optString("trackName", "")))))
                            val artist = currentTrackObj.optString("artist",
                                currentTrackObj.optString("channel",
                                    currentTrackObj.optString("author", "")))
                            if (artist.isNotBlank() && title.isNotBlank() && !title.contains(artist, ignoreCase = true)) {
                                "$artist - $title"
                            } else {
                                title.ifBlank { artist }
                            }
                        }
                        else -> payload.optString("song",
                            payload.optString("currentSong",
                                payload.optString("title",
                                    payload.optString("track",
                                        payload.optString("name", "")))))
                    }
                    val artist = currentTrackObj?.optString("artist",
                        currentTrackObj.optString("channel",
                            currentTrackObj.optString("author", "")))?.takeIf { it.isNotBlank() }
                        ?: payload.optString("artist",
                            payload.optString("channel",
                                payload.optString("author",
                                    payload.optString("singer", ""))))

                    val cover = currentTrackObj?.let { obj ->
                        obj.optString("coverUrl",
                            obj.optString("cover_url",
                                obj.optString("cover",
                                    obj.optString("thumbnail",
                                        obj.optString("thumbnailUrl",
                                            obj.optString("thumbnail_url",
                                                obj.optString("artwork",
                                                    obj.optString("artworkUrl",
                                                        obj.optString("art",
                                                            obj.optString("image",
                                                                obj.optString("img",
                                                                    obj.optString("poster",
                                                                        obj.optString("caratula", "")))))))))))))
                    }?.takeIf { it.isNotBlank() } ?: payload.optString("coverUrl",
                        payload.optString("cover_url",
                            payload.optString("cover",
                                payload.optString("thumbnail",
                                    payload.optString("thumbnailUrl",
                                        payload.optString("thumbnail_url",
                                            payload.optString("artwork",
                                                payload.optString("artworkUrl",
                                                    payload.optString("art",
                                                        payload.optString("image",
                                                            payload.optString("img",
                                                                payload.optString("poster",
                                                                    payload.optString("caratula", "")))))))))))))

                    val upcoming = when {
                        payload.has("upcomingJson") && !payload.isNull("upcomingJson") -> payload.optString("upcomingJson")
                        payload.optJSONArray("queue") != null -> payload.optJSONArray("queue")?.toString() ?: ""
                        payload.optJSONArray("upcoming") != null -> payload.optJSONArray("upcoming")?.toString() ?: ""
                        payload.optJSONArray("tracks") != null -> payload.optJSONArray("tracks")?.toString() ?: ""
                        payload.optJSONArray("upcomingTracks") != null -> payload.optJSONArray("upcomingTracks")?.toString() ?: ""
                        payload.optJSONArray("songs") != null -> payload.optJSONArray("songs")?.toString() ?: ""
                        payload.optJSONObject("queue") != null -> payload.optJSONObject("queue")?.toString() ?: ""
                        payload.optJSONObject("upcoming") != null -> payload.optJSONObject("upcoming")?.toString() ?: ""
                        else -> payload.optString("upcoming", payload.optString("queue", payload.optString("tracks", "")))
                    }

                    val playlist = when {
                        payload.has("playlistJson") && !payload.isNull("playlistJson") -> payload.optString("playlistJson")
                        payload.optJSONArray("playlist") != null -> payload.optJSONArray("playlist")?.toString() ?: ""
                        payload.optJSONArray("playlists") != null -> payload.optJSONArray("playlists")?.toString() ?: ""
                        payload.optJSONArray("playlistTracks") != null -> payload.optJSONArray("playlistTracks")?.toString() ?: ""
                        payload.optJSONObject("playlist") != null -> payload.optJSONObject("playlist")?.toString() ?: ""
                        else -> payload.optString("playlist", payload.optString("playlists", ""))
                    }

                    val isPlaying = when {
                        payload.has("isPlaying") -> payload.optBoolean("isPlaying")
                        payload.has("playing") -> payload.optBoolean("playing")
                        else -> null
                    }
                    val playback = when {
                        isPlaying == true -> "REPRODUCIENDO"
                        isPlaying == false -> "PAUSADO"
                        else -> payload.optString("playback", payload.optString("playbackState", ""))
                    }
                    val vol = if (payload.has("volume")) payload.optInt("volume") else null
                    val muted = if (payload.has("isMuted")) payload.optBoolean("isMuted") else null

                    ClientStateHolder.syncHostMusicState(
                        song = track.ifBlank { null },
                        artist = artist.ifBlank { null },
                        coverUrl = cover.ifBlank { null },
                        upcomingJson = upcoming.ifBlank { null },
                        playlistJson = playlist.ifBlank { null },
                        playback = playback.ifBlank { null },
                        volume = vol,
                        isMuted = muted,
                        sender = payload.optString("nodeName", payload.optString("sender", "Master PC"))
                    )
                }
                else -> {
                    // Try parsing legacy RemoteNodeMessage
                    val parsed = RemoteNodeMessage.fromJson(text)
                    if (parsed != null) {
                        processIncomingSyncData(parsed)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Non-JSON or legacy message received: ${e.message}")
        }
    }

    private fun processIncomingSyncData(response: RemoteNodeMessage) {
        if (response.volume != null) {
            val vol = response.volume.coerceIn(0, 100)
            val normalizedVol = if (vol > 15) ((vol / 100f) * 15f).roundToInt().coerceIn(0, 15) else vol.coerceIn(0, 15)
            ClientStateHolder.setHostVolume(normalizedVol, fromHost = true, showHud = false)
        }

        if (response.isMuted != null) {
            ClientStateHolder.setHostMuted(response.isMuted, showHud = false)
        }

        var track = response.currentSong
        var playback = response.playbackState

        ClientStateHolder.syncHostMusicState(
            song = track.ifBlank { null },
            coverUrl = response.coverUrl.ifBlank { null },
            upcomingJson = response.upcomingJson.ifBlank { null },
            playlistJson = response.playlistJson.ifBlank { null },
            playback = playback.ifBlank { null },
            volume = response.volume,
            isMuted = response.isMuted,
            sender = response.senderName
        )
    }

    private fun handleConnectionLoss(reason: String) {
        _isConnected.value = false
        _isConnecting.value = false
        pingJob?.cancel()

        val lastUrl = currentWsUrl
        _statusMessage.value = "🟡 Reconectando con Master..."
        ClientStateHolder.setConnectionState(false, false, "🟡 Reconectando con Master...")

        // Schedule auto-reconnect
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(5000L)
            if (isActive && !_isConnected.value && lastUrl.isNotBlank()) {
                connect(lastUrl)
            }
        }
    }

    fun startMeshServices() {
        meshSyncJob?.cancel()
        val ctx = context ?: return
        MeshNodeCoordinator.start(ctx)
    }

    private fun disconnectInternal(keepConnectingState: Boolean = false) {
        pingJob?.cancel()
        pingJob = null
        reconnectJob?.cancel()
        reconnectJob = null

        try {
            activeWebSocket?.close(1000, "Disconnect requested")
        } catch (_: Exception) {}
        activeWebSocket = null

        _isConnected.value = false
        if (!keepConnectingState) {
            _isConnecting.value = false
            _statusMessage.value = "Desconectado"
            _pingLatency.value = -1
            ClientStateHolder.updateState {
                it.copy(
                    isConnected = false,
                    isConnecting = false,
                    statusMessage = "Desconectado",
                    pingLatencyMs = -1
                )
            }
        }
    }

    fun disconnect() {
        meshSyncJob?.cancel()
        meshSyncJob = null
        meshBroadcastJob?.cancel()
        meshBroadcastJob = null
        MeshNodeCoordinator.stop()
        disconnectInternal(keepConnectingState = false)
    }
}
