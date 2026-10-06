package com.example.voice

import android.app.ActivityManager
import android.content.Context
import com.example.ai.GeminiChatMessage
import com.example.data.MediaCardItem
import com.example.data.MediaDataParser
import com.example.data.MediaItemCategory
import com.example.network.SatelliteDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class WakeWordPopupPhase {
    IDLE,
    LISTENING,   // 🎧 Animación estilo loading de escuchando captando palabras
    SENDING,     // 📡 Enviando comando al host
    SUCCESS      // 😉 Emoji gigante animado de guiño + 📨 Comando enviado con éxito
}

enum class ClientMicState {
    IDLE_DISCONNECTED,   // Gris / Desconectado o detenido
    CONNECTING,          // Ámbar / Intentando enlazar con host
    LISTENING_STANDBY,   // Verde / Escuchando palabra clave activamente
    RECORDING_SPEECH,    // Amarillo / Capturando voz en tiempo real
    TRANSMITTING,        // Cian / Enviando orden al reproductor principal
    COMMAND_SUCCESS,     // Verde Neón / Confirmado por host
    COMMAND_ERROR,       // Rojo / Error de conexión o no reconocido
    GEMINI_PROCESSING    // Púrpura / Gemini AI optimizando reconocimiento
}

data class ClientLogItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val time: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    val text: String,
    val isIncoming: Boolean = false,
    val isError: Boolean = false
)

data class ClientUiState(
    val isServiceRunning: Boolean = false,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isDiscoveringHost: Boolean = false,
    val isPushToTalk: Boolean = false,
    val isWakeWordActive: Boolean = false,
    val wakeWindowRemainingSecs: Int = 0,
    val micState: ClientMicState = ClientMicState.IDLE_DISCONNECTED,
    val rmsLevel: Float = 0f,
    val partialText: String = "",
    val lastRecognizedText: String = "",
    val statusMessage: String = "Listo para escuchar",
    val connectionMessage: String = "Desconectado",
    val hostIp: String = "",
    val hostPort: Int = 8998,
    val deviceAlias: String = "Mic Remoto",
    val wakeWord: String = "Música",
    val lastAckMessage: String = "",
    val pingLatencyMs: Long = -1,
    val logs: List<ClientLogItem> = emptyList(),
    val isGeminiAiEnhancing: Boolean = false,
    val lastGeminiAiFeedback: String = "",
    val geminiChatMessages: List<GeminiChatMessage> = emptyList(),
    val isGeminiChatLoading: Boolean = false,
    val hostVolume: Int = 10,
    val isHostMuted: Boolean = false,
    val showVolumeHud: Boolean = false,
    val lastVolumeChangeTime: Long = 0L,
    // Wake Word Listening & Success Popup State
    val showWakeWordPopup: Boolean = false,
    val wakeWordPopupPhase: WakeWordPopupPhase = WakeWordPopupPhase.IDLE,
    val popupCommandSummary: String = "",
    val lastSuccessTimestamp: Long = 0L,
    // Multi-Node Mesh & Music Sync State
    val peerNodes: List<SatelliteDevice> = emptyList(),
    val currentSongTitle: String = "",
    val currentSongArtist: String = "",
    val currentSongCoverUrl: String = "",
    val upcomingTracks: List<MediaCardItem> = emptyList(),
    val playlistTracks: List<MediaCardItem> = emptyList(),
    val allMediaCards: List<MediaCardItem> = emptyList(),
    val hasMediaDisplayInfo: Boolean = false,
    val showFloating3dCoverFlow: Boolean = false,
    val playbackState: String = "LISTO",
    val lastCommandSender: String = "",
    // RAM & Process Optimizer State
    val isOptimizingRam: Boolean = false,
    val lastFreedRamMb: Int = 0,
    val showOptimizationBanner: Boolean = false,
    val optimizationMessage: String = "",
    val lastOptimizationTime: Long = 0L,
    val deviceTotalRamMb: Long = 0L,
    val deviceAvailRamMb: Long = 0L
) {
    val isMusicPlaying: Boolean
        get() {
            val ps = playbackState.trim().uppercase()
            if (ps in listOf("REPRODUCIENDO", "PLAYING", "PLAY", "REPRODUCE", "ACTIVE")) return true
            if (ps in listOf("PAUSADO", "PAUSED", "EN PAUSA", "STOPPED", "DETENIDO", "LISTO", "DETENER", "PAUSA", "")) return false
            return currentSongTitle.isNotBlank()
        }
}

object ClientStateHolder {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow(ClientUiState())
    val state: StateFlow<ClientUiState> = _state.asStateFlow()

    fun updateState(transform: (ClientUiState) -> ClientUiState) {
        _state.value = transform(_state.value)
    }

    fun setHostVolume(volume: Int, fromHost: Boolean = false, showHud: Boolean = false) {
        val converted = if (volume > 15) {
            ((volume / 100f) * 15f).roundToInt().coerceIn(0, 15)
        } else {
            volume.coerceIn(0, 15)
        }
        val oldVol = _state.value.hostVolume
        val wasMuted = _state.value.isHostMuted
        val newMuted = (converted == 0)
        val newVol = if (converted > 0) converted else oldVol.coerceIn(1, 15)
        val changed = (oldVol != newVol) || (wasMuted != newMuted)
        _state.value = _state.value.copy(
            hostVolume = newVol,
            isHostMuted = newMuted,
            showVolumeHud = if (showHud) true else _state.value.showVolumeHud,
            lastVolumeChangeTime = if (showHud) System.currentTimeMillis() else _state.value.lastVolumeChangeTime
        )
        if (fromHost && changed) {
            addLog("Volumen del host sincronizado a nivel $newVol/15" + if (newMuted) " (Silenciado)" else "")
        }
    }

    fun adjustHostVolume(delta: Int, showHud: Boolean = true) {
        val current = _state.value.hostVolume.coerceIn(1, 15)
        val newVol = (current + delta).coerceIn(1, 15)
        setHostVolume(newVol, fromHost = false, showHud = showHud)
    }

    fun setHostMuted(isMuted: Boolean, showHud: Boolean = false) {
        _state.value = _state.value.copy(
            isHostMuted = isMuted,
            showVolumeHud = if (showHud) true else _state.value.showVolumeHud,
            lastVolumeChangeTime = if (showHud) System.currentTimeMillis() else _state.value.lastVolumeChangeTime
        )
    }

    fun hideVolumeHud() {
        _state.value = _state.value.copy(showVolumeHud = false)
    }

    fun showVolumeHud() {
        _state.value = _state.value.copy(
            showVolumeHud = true,
            lastVolumeChangeTime = System.currentTimeMillis()
        )
    }

    fun setShowFloating3dCoverFlow(show: Boolean) {
        _state.value = _state.value.copy(showFloating3dCoverFlow = show)
    }

    fun syncHostMusicState(
        song: String? = null,
        artist: String? = null,
        coverUrl: String? = null,
        upcomingJson: String? = null,
        playlistJson: String? = null,
        upcomingList: List<MediaCardItem>? = null,
        playlistList: List<MediaCardItem>? = null,
        playback: String? = null,
        volume: Int? = null,
        isMuted: Boolean? = null,
        sender: String? = null
    ) {
        val convertedVol = volume?.let {
            if (it > 15) ((it / 100f) * 15f).roundToInt().coerceIn(0, 15) else it.coerceIn(0, 15)
        }
        val currentVol = _state.value.hostVolume.coerceIn(1, 15)
        val currentMuted = _state.value.isHostMuted
        val newMuted = when {
            isMuted != null -> isMuted
            convertedVol != null -> (convertedVol == 0)
            else -> currentMuted
        }
        val newVol = when {
            convertedVol != null && convertedVol > 0 -> convertedVol
            else -> currentVol
        }

        val hostIp = _state.value.hostIp
        val hostPort = _state.value.hostPort

        val activeSong = if (!song.isNullOrBlank()) song.trim() else _state.value.currentSongTitle
        val activeArtist = if (!artist.isNullOrBlank()) artist.trim() else _state.value.currentSongArtist
        val rawCover = if (!coverUrl.isNullOrBlank()) coverUrl.trim() else _state.value.currentSongCoverUrl
        var activeCover = MediaDataParser.normalizeCoverUrl(rawCover, hostIp, hostPort)

        val parsedUpcoming = when {
            upcomingList != null -> upcomingList
            !upcomingJson.isNullOrBlank() -> MediaDataParser.parseUpcomingList(upcomingJson, hostIp, hostPort)
            else -> _state.value.upcomingTracks
        }

        val parsedPlaylist = when {
            playlistList != null -> playlistList
            !playlistJson.isNullOrBlank() -> MediaDataParser.parsePlaylistList(playlistJson, hostIp, hostPort)
            else -> _state.value.playlistTracks
        }

        // Si activeCover está vacía pero la canción está presente en upcoming o playlist con carátula válida, la rescatamos
        if (activeCover.isBlank() && activeSong.isNotBlank()) {
            val matchingCard = parsedUpcoming.firstOrNull {
                MediaDataParser.isSameTrack(it.title, it.artist, activeSong, activeArtist) && it.coverUrl.isNotBlank()
            } ?: parsedPlaylist.firstOrNull {
                MediaDataParser.isSameTrack(it.title, it.artist, activeSong, activeArtist) && it.coverUrl.isNotBlank()
            }
            if (matchingCard != null) {
                activeCover = matchingCard.coverUrl
            }
        }

        // FILTRADO ESTRICTO ANTI-DUPLICADOS:
        // La canción que se está reproduciendo se añade como cabecera (CURRENT) una sola vez.
        // Filtramos de upcoming y playlist cualquier elemento que corresponda a la canción actual,
        // garantizando que la canción en reproducción NUNCA se visualice doblemente en la cola.
        val filteredUpcoming = if (activeSong.isNotBlank()) {
            parsedUpcoming.filterNot { item ->
                MediaDataParser.isSameTrack(item.title, item.artist, activeSong, activeArtist)
            }
        } else {
            parsedUpcoming
        }

        val filteredPlaylist = if (activeSong.isNotBlank()) {
            parsedPlaylist.filterNot { item ->
                MediaDataParser.isSameTrack(item.title, item.artist, activeSong, activeArtist)
            }
        } else {
            parsedPlaylist
        }

        val combinedCards = mutableListOf<MediaCardItem>()
        if (activeSong.isNotBlank()) {
            combinedCards.add(
                MediaCardItem(
                    id = "current-track-${activeSong.hashCode()}",
                    title = activeSong,
                    artist = activeArtist.ifBlank { "Reproducción actual" },
                    coverUrl = activeCover,
                    category = MediaItemCategory.CURRENT,
                    commandQuery = if (activeArtist.isNotBlank() && !activeSong.contains(activeArtist, ignoreCase = true)) "$activeSong $activeArtist" else activeSong,
                    isSample = false
                )
            )
        }
        combinedCards.addAll(filteredUpcoming)
        combinedCards.addAll(filteredPlaylist)

        // Deduplicación general de seguridad: evitar duplicados idénticos en la cola
        val seenKeys = mutableSetOf<String>()
        val finalCards = mutableListOf<MediaCardItem>()
        for (card in combinedCards) {
            val key = if (activeSong.isNotBlank() && MediaDataParser.isSameTrack(card.title, card.artist, activeSong, activeArtist)) {
                "CURRENT_PLAYING_TRACK"
            } else {
                "${card.title.trim().lowercase()}|${card.artist.trim().lowercase()}"
            }
            if (seenKeys.add(key)) {
                finalCards.add(card)
            }
        }

        val hasInfo = activeSong.isNotBlank() || filteredUpcoming.isNotEmpty() || filteredPlaylist.isNotEmpty()

        _state.value = _state.value.copy(
            currentSongTitle = activeSong,
            currentSongArtist = activeArtist,
            currentSongCoverUrl = activeCover,
            upcomingTracks = filteredUpcoming,
            playlistTracks = filteredPlaylist,
            allMediaCards = finalCards,
            hasMediaDisplayInfo = hasInfo,
            playbackState = if (!playback.isNullOrBlank()) playback else _state.value.playbackState,
            hostVolume = newVol,
            isHostMuted = newMuted,
            lastCommandSender = if (!sender.isNullOrBlank()) sender else _state.value.lastCommandSender
        )
    }

    fun updatePeerNode(device: SatelliteDevice) {
        val currentList = _state.value.peerNodes.toMutableList()
        val index = currentList.indexOfFirst { it.id == device.id || it.ip == device.ip }
        if (index >= 0) {
            currentList[index] = device
        } else {
            currentList.add(device)
        }
        // Filter out devices not seen in last 45s
        val cutoff = System.currentTimeMillis() - 45000L
        val activeList = currentList.filter { it.lastSeen >= cutoff }
        _state.value = _state.value.copy(peerNodes = activeList)
    }

    fun updatePeerNodes(devices: List<SatelliteDevice>) {
        _state.value = _state.value.copy(peerNodes = devices)
    }

    /**
     * Executes deep RAM liberation, process caching cleanup, and engine refreshment.
     */
    fun performRamAndProcessOptimization(context: Context, isAuto: Boolean = false): Int {
        val runtime = Runtime.getRuntime()
        val usedBeforeBytes = runtime.totalMemory() - runtime.freeMemory()

        // 1. Trim logs if they are too long
        val currentLogs = _state.value.logs
        if (currentLogs.size > 15) {
            _state.value = _state.value.copy(logs = currentLogs.take(15))
        }

        // 2. Invoke System Garbage Collector and memory trimming
        try {
            System.gc()
            System.runFinalization()
            System.gc()
        } catch (_: Exception) {}

        val usedAfterBytes = runtime.totalMemory() - runtime.freeMemory()
        val freedMb = ((usedBeforeBytes - usedAfterBytes) / (1024 * 1024)).toInt().coerceAtLeast(1)

        // Read overall device RAM info
        var totalMb = 0L
        var availMb = 0L
        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            totalMb = memInfo.totalMem / (1024 * 1024)
            availMb = memInfo.availMem / (1024 * 1024)
        } catch (_: Exception) {}

        val feedbackMsg = if (isAuto) {
            "⚡ Auto-liberación: ${freedMb}MB de RAM liberada y procesos optimizados"
        } else {
            "🚀 Liberación completada: ${freedMb}MB liberados • RAM disponible: ${availMb}MB"
        }

        _state.value = _state.value.copy(
            isOptimizingRam = false,
            lastFreedRamMb = freedMb,
            showOptimizationBanner = true,
            optimizationMessage = feedbackMsg,
            lastOptimizationTime = System.currentTimeMillis(),
            deviceTotalRamMb = totalMb,
            deviceAvailRamMb = availMb
        )

        addLog(feedbackMsg)

        // Auto hide banner after 4.5 seconds
        scope.launch {
            delay(4500L)
            if (System.currentTimeMillis() - _state.value.lastOptimizationTime >= 4000L) {
                _state.value = _state.value.copy(showOptimizationBanner = false)
            }
        }

        return freedMb
    }

    fun hideOptimizationBanner() {
        _state.value = _state.value.copy(showOptimizationBanner = false)
    }

    fun setMicState(state: ClientMicState, message: String? = null) {
        _state.value = _state.value.copy(
            micState = state,
            statusMessage = message ?: _state.value.statusMessage
        )
    }

    fun setGeminiAiFeedback(feedback: String) {
        _state.value = _state.value.copy(
            lastGeminiAiFeedback = feedback
        )
    }

    fun addGeminiMessage(role: String, text: String) {
        val newMsg = GeminiChatMessage(role = role, text = text)
        _state.value = _state.value.copy(
            geminiChatMessages = _state.value.geminiChatMessages + newMsg
        )
    }

    fun clearGeminiMessages() {
        _state.value = _state.value.copy(
            geminiChatMessages = emptyList(),
            lastGeminiAiFeedback = ""
        )
    }

    fun setConnectionState(isConnected: Boolean, isConnecting: Boolean, message: String) {
        _state.value = _state.value.copy(
            isConnected = isConnected,
            isConnecting = isConnecting,
            connectionMessage = message
        )
    }

    fun setWakeWindowState(isActive: Boolean, remainingSecs: Int = 0) {
        _state.value = _state.value.copy(
            isWakeWordActive = isActive,
            wakeWindowRemainingSecs = remainingSecs
        )
    }

    fun setRms(rms: Float) {
        _state.value = _state.value.copy(rmsLevel = rms.coerceIn(0f, 10f))
    }

    fun setPartialText(text: String) {
        _state.value = _state.value.copy(partialText = text)
    }

    fun showWakeWordPopup(isListening: Boolean = true) {
        _state.value = _state.value.copy(
            showWakeWordPopup = isListening,
            wakeWordPopupPhase = if (isListening) WakeWordPopupPhase.LISTENING else WakeWordPopupPhase.IDLE,
            popupCommandSummary = if (isListening) "" else _state.value.popupCommandSummary
        )
    }

    fun notifyCommandSending(summary: String) {
        _state.value = _state.value.copy(
            showWakeWordPopup = true,
            wakeWordPopupPhase = WakeWordPopupPhase.SENDING,
            popupCommandSummary = summary
        )
    }

    fun notifyCommandSuccess(commandSummary: String) {
        val now = System.currentTimeMillis()
        _state.value = _state.value.copy(
            showWakeWordPopup = true,
            wakeWordPopupPhase = WakeWordPopupPhase.SUCCESS,
            popupCommandSummary = commandSummary,
            lastSuccessTimestamp = now,
            lastRecognizedText = commandSummary
        )

        // Auto dismiss success popup and reset mic state after 2.4s
        scope.launch {
            delay(2400L)
            if (_state.value.lastSuccessTimestamp == now) {
                _state.value = _state.value.copy(
                    showWakeWordPopup = false,
                    isWakeWordActive = false,
                    wakeWordPopupPhase = WakeWordPopupPhase.IDLE,
                    popupCommandSummary = "",
                    partialText = "",
                    micState = if (_state.value.isServiceRunning) ClientMicState.LISTENING_STANDBY else ClientMicState.IDLE_DISCONNECTED
                )
            }
        }
    }

    fun hideWakeWordPopup() {
        _state.value = _state.value.copy(
            showWakeWordPopup = false,
            isWakeWordActive = false,
            wakeWordPopupPhase = WakeWordPopupPhase.IDLE,
            popupCommandSummary = ""
        )
    }

    fun addLog(text: String, isIncoming: Boolean = false, isError: Boolean = false) {
        val item = ClientLogItem(text = text, isIncoming = isIncoming, isError = isError)
        _state.value = _state.value.copy(
            logs = listOf(item) + _state.value.logs.take(50)
        )
    }

    fun clearLogs() {
        _state.value = _state.value.copy(logs = emptyList())
    }
}
