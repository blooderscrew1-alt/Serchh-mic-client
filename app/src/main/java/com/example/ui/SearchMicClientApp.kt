package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Album
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.Floating3dCoverFlowOverlay
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import com.journeyapps.barcodescanner.ScanContract
import com.example.ai.GeminiVoiceService
import com.example.receiver.ScheduledRebootManager
import com.example.receiver.AutostartHelper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import kotlin.math.roundToInt
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClientSettings
import com.example.network.NetworkHelper
import com.example.network.RemoteClientHolder
import com.example.network.QrCodePairingHelper
import com.example.network.ScannedQrConfig
import com.example.ui.theme.AmberYellow
import com.example.ui.theme.AmberYellowGlow
import com.example.ui.theme.CoralRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenGlow
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ZenOledBlack
import com.example.voice.ClientMicState
import com.example.voice.ClientStateHolder
import com.example.voice.RemoteMicForegroundService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchMicClientApp(
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val state by ClientStateHolder.state.collectAsState()
    val clientSettings = remember { ClientSettings(context) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showGeminiAssistantSheet by remember { mutableStateOf(false) }
    var isNightModeActive by remember { mutableStateOf(false) }
    var manualNightModeOverride by remember { mutableStateOf<Boolean?>(null) }

    // Night Mode Schedule Monitor
    LaunchedEffect(
        clientSettings.nightModeScheduleEnabled,
        clientSettings.nightStartHour,
        clientSettings.nightStartMinute,
        clientSettings.nightEndHour,
        clientSettings.nightEndMinute,
        manualNightModeOverride
    ) {
        while (true) {
            val inSchedule = clientSettings.isCurrentlyInNightWindow()
            val targetActive = manualNightModeOverride ?: inSchedule
            if (isNightModeActive != targetActive) {
                isNightModeActive = targetActive
            }
            delay(4000L)
        }
    }

    // Screen Brightness Dimming Manager (including Complete Screen Off / Zero Backlight Mode)
    LaunchedEffect(isNightModeActive, clientSettings.nightDimLevel, clientSettings.nightScreenOffComplete) {
        try {
            if (isNightModeActive) {
                val targetBrightness = if (clientSettings.nightScreenOffComplete) {
                    0.0f
                } else {
                    clientSettings.nightDimLevel.coerceIn(0.00f, 1.0f)
                }
                activity?.window?.attributes = activity?.window?.attributes?.apply {
                    screenBrightness = targetBrightness
                }
            } else {
                activity?.window?.attributes = activity?.window?.attributes?.apply {
                    screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
        } catch (_: Exception) {}
    }

    // Vibrator helper
    fun triggerHaptic(durationMs: Long = 40) {
        if (!clientSettings.hapticFeedback) return
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    val onSetHostVolume: (Int) -> Unit = { newVol ->
        triggerHaptic(30)
        val level = newVol.coerceIn(1, 15)
        val pct = ((level / 15f) * 100f).roundToInt().coerceIn(1, 100)
        ClientStateHolder.setHostVolume(level, showHud = true)
        scope.launch {
            val client = RemoteClientHolder.getClient(context)
            client.sendVoiceCommand(
                commandType = "VOLUME_SET_LEVEL_$level",
                songQuery = "$level",
                rawSpokenText = "Ajustar volumen a nivel $level/15 ($pct%)",
                hostIp = clientSettings.hostIp,
                port = clientSettings.hostPort
            )
        }
    }

    val onAdjustVolumeDelta: (Int) -> Unit = { delta ->
        val newVol = (state.hostVolume + delta).coerceIn(1, 15)
        onSetHostVolume(newVol)
    }

    val onToggleHostMute: () -> Unit = {
        triggerHaptic(35)
        val nextMute = !state.isHostMuted
        ClientStateHolder.setHostMuted(nextMute, showHud = true)
        scope.launch {
            val client = RemoteClientHolder.getClient(context)
            val restoreLvl = state.hostVolume.coerceIn(1, 15)
            client.sendVoiceCommand(
                commandType = if (nextMute) "MUTE" else "VOLUME_SET_$restoreLvl",
                songQuery = if (nextMute) "0" else "$restoreLvl",
                rawSpokenText = if (nextMute) "Silenciar" else "Restaurar volumen a nivel $restoreLvl",
                hostIp = clientSettings.hostIp,
                port = clientSettings.hostPort
            )
        }
    }

    val sendHostCommand: (String, String) -> Unit = { cmdType, label ->
        triggerHaptic(35)
        ClientStateHolder.addLog("[Comando Multimedia] $label ($cmdType)")
        scope.launch {
            val client = RemoteClientHolder.getClient(context)
            client.sendVoiceCommand(
                commandType = cmdType,
                songQuery = "",
                rawSpokenText = label,
                hostIp = clientSettings.hostIp,
                port = clientSettings.hostPort
            )
        }
    }

    val qrScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val rawValue = result.contents
            QrCodePairingHelper.triggerScanHapticFeedback(context)
            val parsedConfig = QrCodePairingHelper.parseQrCodePayload(rawValue)
            if (parsedConfig != null) {
                // 1. Reemplazar y guardar en SharedPreferences la URL y Sala del Satélite
                clientSettings.hostIp = parsedConfig.wsUrl
                clientSettings.satelliteRoom = parsedConfig.room

                // 2. Feedback en pantalla de la UI
                val statusMsg = "Conectando con PC (${parsedConfig.room})..."
                ClientStateHolder.setConnectionState(false, true, statusMsg)
                ClientStateHolder.addLog("QR Escaneado Correctamente: wsUrl=${parsedConfig.wsUrl}, room=${parsedConfig.room}")
                Toast.makeText(context, "¡QR Escaneado! Conectando con ${parsedConfig.room}...", Toast.LENGTH_SHORT).show()

                // 3. Iniciar conexión WebSocket de inmediato enviando el registro inicial con la sala
                val client = RemoteClientHolder.getClient(context)
                client.connect(
                    rawHost = parsedConfig.wsUrl,
                    defaultPort = clientSettings.hostPort,
                    room = parsedConfig.room
                )
            } else {
                Toast.makeText(context, "El código QR no contiene un formato de vinculación válido.", Toast.LENGTH_LONG).show()
                ClientStateHolder.addLog("Error escáner QR: Payload no válido", isError = true)
            }
        }
    }

    val handleScanQr: () -> Unit = {
        triggerHaptic(40)
        try {
            qrScanLauncher.launch(QrCodePairingHelper.createScanOptions())
        } catch (e: Exception) {
            Toast.makeText(context, "Error al abrir la cámara: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    val handleDiscoverOrReconnect: () -> Unit = {
        triggerHaptic(40)
        scope.launch {
            ClientStateHolder.updateState {
                it.copy(
                    isDiscoveringHost = true,
                    statusMessage = "Buscando Host en la red Wi-Fi..."
                )
            }
            ClientStateHolder.addLog("[Autodescubrimiento] Buscando Host PC / Master en la red local...")
            Toast.makeText(context, "Buscando Host en la red local...", Toast.LENGTH_SHORT).show()

            val discovered = NetworkHelper.discoverHostOnLocalNetwork(
                preferredPort = clientSettings.hostPort,
                previousIp = clientSettings.hostIp,
                onProgress = { progress ->
                    ClientStateHolder.updateState { it.copy(statusMessage = progress) }
                }
            )

            ClientStateHolder.updateState { it.copy(isDiscoveringHost = false) }

            if (discovered != null) {
                clientSettings.hostIp = discovered.ip
                clientSettings.hostPort = discovered.port
                ClientStateHolder.updateState {
                    it.copy(
                        hostIp = discovered.ip,
                        hostPort = discovered.port,
                        statusMessage = "Host encontrado en ${discovered.ip}:${discovered.port}"
                    )
                }
                ClientStateHolder.addLog("[Autodescubrimiento] ¡Host encontrado en ${discovered.ip}:${discovered.port} (${discovered.method})! Enlazando...")
                Toast.makeText(context, "¡Host detectado en ${discovered.ip}:${discovered.port}! Conectando...", Toast.LENGTH_LONG).show()
                RemoteClientHolder.connect(context)
            } else {
                ClientStateHolder.updateState {
                    it.copy(statusMessage = "No se detectó Host en la red")
                }
                ClientStateHolder.addLog("[Autodescubrimiento] No se detectó ningún Host en la red local. Si cambió la IP o se reinició el router, asegúrate de que el PC/Host esté encendido en este mismo Wi-Fi.", isError = true)
                Toast.makeText(context, "No se encontró ningún Host en la red Wi-Fi", Toast.LENGTH_LONG).show()
                if (clientSettings.hostIp.isNotBlank()) {
                    RemoteClientHolder.connect(context)
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        // If Night Mode is active with Complete Screen Off (Blackout Mode) enabled
        if (isNightModeActive && clientSettings.nightScreenOffComplete) {
            FullScreenDeepSleepBlackoutView(
                state = state,
                settings = clientSettings,
                onExitNightMode = {
                    triggerHaptic(35)
                    manualNightModeOverride = false
                    isNightModeActive = false
                },
                onToggleContinuous = {
                    triggerHaptic(50)
                    if (state.isServiceRunning) {
                        onStopService()
                    } else {
                        onStartService()
                    }
                },
                onSendCommand = { cmdType, label ->
                    triggerHaptic(35)
                    scope.launch {
                        val client = RemoteClientHolder.getClient(context)
                        client.sendVoiceCommand(
                            commandType = cmdType,
                            songQuery = "",
                            rawSpokenText = label,
                            hostIp = clientSettings.hostIp,
                            port = clientSettings.hostPort
                        )
                    }
                },
                onSetVolume = onSetHostVolume,
                onAdjustVolumeDelta = onAdjustVolumeDelta,
                onToggleMute = onToggleHostMute,
                onOpenSettings = {
                    triggerHaptic(25)
                    showSettingsDialog = true
                }
            )
        } else if (isNightModeActive && clientSettings.nightShowFullScreenClock) {
        FullScreenNightClockView(
            state = state,
            settings = clientSettings,
            onExitNightMode = {
                triggerHaptic(30)
                manualNightModeOverride = false
                isNightModeActive = false
            },
            onToggleContinuous = {
                triggerHaptic(50)
                if (state.isServiceRunning) {
                    onStopService()
                } else {
                    onStartService()
                }
            },
            onSendCommand = { cmdType, label ->
                triggerHaptic(35)
                scope.launch {
                    val client = RemoteClientHolder.getClient(context)
                    client.sendVoiceCommand(
                        commandType = cmdType,
                        songQuery = "",
                        rawSpokenText = label,
                        hostIp = clientSettings.hostIp,
                        port = clientSettings.hostPort
                    )
                }
            },
            onSetVolume = onSetHostVolume,
            onAdjustVolumeDelta = onAdjustVolumeDelta,
            onToggleMute = onToggleHostMute,
            onOpenSettings = {
                triggerHaptic(25)
                showSettingsDialog = true
            }
        )
    } else if (isLandscape) {
        LandscapeFuturisticMicView(
            state = state,
            settings = clientSettings,
            isNightModeActive = isNightModeActive,
            onToggleNightMode = {
                triggerHaptic(35)
                val nextState = !isNightModeActive
                manualNightModeOverride = nextState
                isNightModeActive = nextState
            },
            onToggleContinuous = {
                triggerHaptic(50)
                if (state.isServiceRunning) {
                    onStopService()
                } else {
                    onStartService()
                }
            },
            onPttDown = {
                triggerHaptic(40)
                RemoteMicForegroundService.startManualPushToTalk(context)
            },
            onPttUp = {
                triggerHaptic(30)
                RemoteMicForegroundService.stopManualPushToTalk(context)
            },
            onSendCommand = { cmdType, label ->
                triggerHaptic(35)
                scope.launch {
                    val client = RemoteClientHolder.getClient(context)
                    client.sendVoiceCommand(
                        commandType = cmdType,
                        songQuery = "",
                        rawSpokenText = label,
                        hostIp = clientSettings.hostIp,
                        port = clientSettings.hostPort
                    )
                }
            },
            onSetVolume = onSetHostVolume,
            onAdjustVolumeDelta = onAdjustVolumeDelta,
            onToggleMute = onToggleHostMute,
            onLiberateRam = {
                triggerHaptic(40)
                RemoteMicForegroundService.forceRefresh(context)
            },
            onOpenSettings = {
                triggerHaptic(25)
                showSettingsDialog = true
            },
            onQuickReconnect = handleDiscoverOrReconnect
        )
    } else {
        Scaffold(
            containerColor = ZenOledBlack,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                ClientTopAppBar(
                    state = state,
                    isNightModeActive = isNightModeActive,
                    onToggleNightMode = {
                        triggerHaptic(35)
                        val nextState = !isNightModeActive
                        manualNightModeOverride = nextState
                        isNightModeActive = nextState
                    },
                    onLiberateRam = {
                        triggerHaptic(40)
                        RemoteMicForegroundService.forceRefresh(context)
                    },
                    onOpenSettings = {
                        triggerHaptic(25)
                        showSettingsDialog = true
                    },
                    onQuickReconnect = handleDiscoverOrReconnect,
                    onScanQr = handleScanQr
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 1. Central Interactive Microphone Component
                item {
                    CentralMicrophoneCard(
                        state = state,
                        settings = clientSettings,
                        onToggleContinuous = {
                            triggerHaptic(50)
                            if (state.isServiceRunning) {
                                onStopService()
                            } else {
                                onStartService()
                            }
                        },
                        onPttDown = {
                            triggerHaptic(40)
                            RemoteMicForegroundService.startManualPushToTalk(context)
                        },
                        onPttUp = {
                            triggerHaptic(30)
                            RemoteMicForegroundService.stopManualPushToTalk(context)
                        }
                    )
                }

                // 1.5. Local Whisper ASR Offline 24/7 Engine Status Card
                item {
                    LocalWhisperAsrStatusCard(
                        state = state,
                        onToggleService = {
                            triggerHaptic(50)
                            if (state.isServiceRunning) {
                                onStopService()
                            } else {
                                onStartService()
                            }
                        }
                    )
                }

                // 2. Gemini Live Voice AI Assistant Card
                item {
                    GeminiVoiceAssistantCard(
                        settings = clientSettings,
                        onOpenAssistant = {
                            triggerHaptic(35)
                            showGeminiAssistantSheet = true
                        }
                    )
                }

                // 3. Voice Commands Flexibility & Examples Card
                item {
                    VoiceCommandsGuideCard(
                        wakeWord = clientSettings.wakeWord,
                        directCommandsEnabled = clientSettings.directCommandsEnabled,
                        wakeWindowSeconds = clientSettings.wakeWindowSeconds,
                        onOpenSettings = {
                            triggerHaptic(25)
                            showSettingsDialog = true
                        }
                    )
                }

                // 3. Quick Wi-Fi Connection Card
                item {
                    HostConnectionCard(
                        state = state,
                        settings = clientSettings,
                        onConnect = {
                            triggerHaptic(35)
                            RemoteClientHolder.connect(context)
                        },
                        onDisconnect = {
                            triggerHaptic(35)
                            RemoteClientHolder.disconnect()
                        },
                        onTestPing = {
                            triggerHaptic(30)
                            scope.launch {
                                val client = RemoteClientHolder.getClient(context)
                                client.testPing(clientSettings.hostIp, clientSettings.hostPort)
                            }
                        },
                        onOpenConfig = {
                            triggerHaptic(25)
                            showSettingsDialog = true
                        },
                        onScanQr = handleScanQr
                    )
                }

                // 3. Quick Remote Control Shortcuts with Host Volume Slider
                item {
                    QuickRemoteControlsCard(
                        state = state,
                        onSendCommand = { cmdType, label ->
                            triggerHaptic(35)
                            scope.launch {
                                val client = RemoteClientHolder.getClient(context)
                                client.sendVoiceCommand(
                                    commandType = cmdType,
                                    songQuery = "",
                                    rawSpokenText = label,
                                    hostIp = clientSettings.hostIp,
                                    port = clientSettings.hostPort
                                )
                            }
                        },
                        onSetVolume = onSetHostVolume,
                        onAdjustVolumeDelta = onAdjustVolumeDelta,
                        onToggleMute = onToggleHostMute
                    )
                }

                // 4. Mesh Nodes Harmony & Live Synchronized Music Card
                item {
                    NodeMeshSyncStatusCard(
                        state = state,
                        settings = clientSettings,
                        onLiberateRam = {
                            triggerHaptic(40)
                            RemoteMicForegroundService.forceRefresh(context)
                        },
                        onSendMeshSync = {
                            triggerHaptic(35)
                            scope.launch {
                                val client = RemoteClientHolder.getClient(context)
                                client.sendVoiceCommand(
                                    commandType = "PING",
                                    songQuery = "",
                                    rawSpokenText = "Sincronización Mesh",
                                    hostIp = clientSettings.hostIp,
                                    port = clientSettings.hostPort
                                )
                            }
                        }
                    )
                }

                // 5. Live Transmission & Event Log
                item {
                    LiveActivityLogCard(
                        logs = state.logs,
                        onClear = {
                            triggerHaptic(20)
                            ClientStateHolder.clearLogs()
                        },
                        onRefreshEngine = {
                            triggerHaptic(30)
                            RemoteMicForegroundService.forceRefresh(context)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showGeminiAssistantSheet) {
        GeminiLiveVoiceAssistantSheet(
            state = state,
            settings = clientSettings,
            onDismiss = { showGeminiAssistantSheet = false },
            onSendMusicCommand = { songQuery ->
                triggerHaptic(45)
                scope.launch {
                    val client = RemoteClientHolder.getClient(context)
                    client.sendVoiceCommand(
                        commandType = "SEARCH_PLAY",
                        songQuery = songQuery,
                        rawSpokenText = "Gemini Live: $songQuery",
                        hostIp = clientSettings.hostIp,
                        port = clientSettings.hostPort
                    )
                }
            }
        )
    }

    if (showSettingsDialog) {
        ClientSettingsDialog(
            settings = clientSettings,
            onDismiss = { showSettingsDialog = false },
            onTriggerNightModeNow = {
                manualNightModeOverride = true
                isNightModeActive = true
                showSettingsDialog = false
            },
            onSave = { ip, room, port, alias, wakeWord, autoConnect, muteBeeps, keepScreen,
                       directCmds, wakeWinSecs, autoStartMic,
                       autoRefresh, autoRefreshMins, nodeMeshSync,
                       speechLangMode, phoneticFix,
                       startOnBoot, dailyReboot, rebootH, rebootM,
                       nightSchedule, startH, startM, endH, endM,
                       screenOffComplete, tapToWake, voiceWake,
                       fullScreenClock, dimLevel, burnIn,
                       geminiEnhance, geminiTts, geminiModel, customApiKey ->
                clientSettings.hostIp = ip
                clientSettings.satelliteRoom = room
                clientSettings.hostPort = port
                clientSettings.deviceAlias = alias
                clientSettings.wakeWord = wakeWord
                clientSettings.autoConnectOnLaunch = autoConnect
                clientSettings.muteRecognizerBeeps = muteBeeps
                clientSettings.keepScreenOn = keepScreen
                clientSettings.directCommandsEnabled = directCmds
                clientSettings.wakeWindowSeconds = wakeWinSecs
                clientSettings.autoStartListeningOnLaunch = autoStartMic
                clientSettings.autoRefreshEnabled = autoRefresh
                clientSettings.autoRefreshMinutes = autoRefreshMins
                clientSettings.nodeMeshSyncEnabled = nodeMeshSync
                clientSettings.speechLanguageMode = speechLangMode
                clientSettings.bilingualPhoneticFixEnabled = phoneticFix
                clientSettings.startOnBootEnabled = startOnBoot
                clientSettings.dailyRebootEnabled = dailyReboot
                clientSettings.dailyRebootHour = rebootH
                clientSettings.dailyRebootMinute = rebootM
                clientSettings.nightModeScheduleEnabled = nightSchedule
                clientSettings.nightStartHour = startH
                clientSettings.nightStartMinute = startM
                clientSettings.nightEndHour = endH
                clientSettings.nightEndMinute = endM
                clientSettings.nightScreenOffComplete = screenOffComplete
                clientSettings.nightTapToWake = tapToWake
                clientSettings.nightVoiceWake = voiceWake
                clientSettings.nightShowFullScreenClock = fullScreenClock
                clientSettings.nightDimLevel = dimLevel
                clientSettings.nightClockBurnInProtection = burnIn
                clientSettings.geminiAiVoiceEnhanceEnabled = geminiEnhance
                clientSettings.geminiVoiceTtsEnabled = geminiTts
                clientSettings.geminiModelName = geminiModel
                clientSettings.customGeminiApiKey = customApiKey

                // Update scheduled reboot alarm
                ScheduledRebootManager.scheduleDailyReboot(context)

                // Restart speech recognizer to apply new language parameters immediately if running
                RemoteMicForegroundService.forceRefresh(context)

                ClientStateHolder.updateState {
                    it.copy(
                        hostIp = ip,
                        hostPort = port,
                        deviceAlias = alias,
                        wakeWord = wakeWord
                    )
                }

                RemoteClientHolder.connect(context)
                showSettingsDialog = false
            }
        )
    }

    // Floating RAM & Process Optimizer Banner overlay
    RamOptimizationBannerOverlay(
        state = state,
        onDismiss = { ClientStateHolder.hideOptimizationBanner() },
        modifier = Modifier.align(Alignment.TopCenter)
    )

    // Floating Volume HUD overlay (appears automatically when volume changes or adjusted)
    FloatingVolumeHudOverlay(
        visible = state.showVolumeHud,
        volume = state.hostVolume,
        isMuted = state.isHostMuted,
        lastChangeTime = state.lastVolumeChangeTime,
        onVolumeChange = { ClientStateHolder.setHostVolume(it, showHud = true) },
        onVolumeChangeFinished = onSetHostVolume,
        onAdjustDelta = onAdjustVolumeDelta,
        onToggleMute = onToggleHostMute,
        onDismiss = { ClientStateHolder.hideVolumeHud() },
        modifier = Modifier.align(Alignment.TopCenter)
    )

    // Animated Wake-Word Listening (Loading/Capturing words 🎧) and Success (Giant Wink 😉 + 📨) Popup
    WakeWordListeningPopup(
        state = state,
        onDismiss = { ClientStateHolder.hideWakeWordPopup() },
        onManualCancel = {
            ClientStateHolder.hideWakeWordPopup()
            com.example.voice.RemoteMicForegroundService.cancelManualPushToTalk(context)
        }
    )

    // Mantener la pantalla de la cola de reproducción abierta mientras el host reproduzca música
    var userDismissedSongTitle by remember { mutableStateOf<String?>(null) }
    var previousSongTitle by remember { mutableStateOf("") }
    var previousPlaybackActive by remember { mutableStateOf(false) }

    LaunchedEffect(state.isMusicPlaying, state.currentSongTitle, clientSettings.keepQueueOpenDuringPlayback) {
        val isPlaying = state.isMusicPlaying
        val currentTitle = state.currentSongTitle

        if (clientSettings.keepQueueOpenDuringPlayback && isPlaying) {
            // Si comenzó una nueva canción o se reanudó la música, reseteamos el descarte manual
            if (currentTitle != previousSongTitle || (!previousPlaybackActive && isPlaying)) {
                userDismissedSongTitle = null
            }
            if (userDismissedSongTitle != currentTitle) {
                ClientStateHolder.setShowFloating3dCoverFlow(true)
            }
        }
        previousSongTitle = currentTitle
        previousPlaybackActive = isPlaying
    }

    // Floating 3D Cover Flow Overlay (User's 3D sliding grid layout with covers, queue and playlists)
    if (state.showFloating3dCoverFlow) {
        Floating3dCoverFlowOverlay(
            items = state.allMediaCards,
            currentSongTitle = state.currentSongTitle,
            currentCoverUrl = state.currentSongCoverUrl,
            playbackState = state.playbackState,
            isPlaybackActive = state.isMusicPlaying,
            keepOpenDuringPlayback = clientSettings.keepQueueOpenDuringPlayback,
            hostVolume = state.hostVolume,
            isHostMuted = state.isHostMuted,
            onSetHostVolume = { newVol ->
                onSetHostVolume(newVol)
            },
            onPreviousTrack = {
                sendHostCommand("PREVIOUS", "Anterior canción")
            },
            onTogglePlayPause = {
                val nextPlay = !state.isMusicPlaying
                ClientStateHolder.updateState {
                    it.copy(playbackState = if (nextPlay) "REPRODUCIENDO" else "PAUSADO")
                }
                sendHostCommand(
                    if (state.isMusicPlaying) "PAUSE" else "RESUME",
                    if (state.isMusicPlaying) "Pausar música" else "Reanudar música"
                )
            },
            onNextTrack = {
                sendHostCommand("NEXT", "Siguiente canción")
            },
            onToggleKeepOpenDuringPlayback = { enabled ->
                clientSettings.keepQueueOpenDuringPlayback = enabled
                if (!enabled) {
                    userDismissedSongTitle = null
                }
            },
            onDismiss = {
                userDismissedSongTitle = state.currentSongTitle.ifBlank { "dismissed" }
                ClientStateHolder.setShowFloating3dCoverFlow(false)
            },
            onPlayMedia = { card ->
                triggerHaptic(50)
                ClientStateHolder.addLog("Abriendo canción desde carátula 3D: ${card.title}")
                ClientStateHolder.updateState { it.copy(statusMessage = "Abriendo canción: ${card.title}") }
                scope.launch {
                    val client = RemoteClientHolder.getClient(context)
                    client.sendVoiceCommand(
                        commandType = "SEARCH_PLAY",
                        songQuery = card.commandQuery.ifBlank { card.title },
                        rawSpokenText = "Reproducir ${card.title}",
                        hostIp = clientSettings.hostIp,
                        port = clientSettings.hostPort
                    )
                }
            }
        )
    }
}
}

@Composable
private fun ClientTopAppBar(
    state: com.example.voice.ClientUiState,
    isNightModeActive: Boolean,
    onToggleNightMode: () -> Unit,
    onLiberateRam: () -> Unit,
    onOpenSettings: () -> Unit,
    onQuickReconnect: () -> Unit,
    onScanQr: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onQuickReconnect() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("satellite_mode_header_btn")
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (state.isDiscoveringHost) Color(0x33F59E0B) else GlassSurface)
                    .border(1.dp, if (state.isDiscoveringHost) AmberYellow else DarkBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (state.isDiscoveringHost) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = AmberYellow,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Cast,
                        contentDescription = "Logo",
                        tint = if (state.isConnected) NeonGreen else CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "serch mic client",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.3.sp
                )
                Text(
                    text = if (state.isConnected) "Enlace Wi-Fi activo" else if (state.isDiscoveringHost) "Buscando Host en red..." else "Modo satélite • Toca para buscar Host",
                    fontSize = 11.sp,
                    color = if (state.isConnected) NeonGreen else if (state.isDiscoveringHost) AmberYellow else CyanAccent
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // RAM & Process Cleaner Quick Button
            IconButton(
                onClick = onLiberateRam,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (state.showOptimizationBanner) Color(0x3306B6D4) else GlassSurface)
                    .border(1.dp, if (state.showOptimizationBanner) CyanAccent else DarkBorder, CircleShape)
                    .testTag("ram_refresh_top_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = "Liberar RAM y Procesos",
                    tint = if (state.showOptimizationBanner) CyanAccent else NeonGreen,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Quick Night Mode Toggle
            IconButton(
                onClick = onToggleNightMode,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isNightModeActive) Color(0x33F59E0B) else GlassSurface)
                    .border(1.dp, if (isNightModeActive) AmberYellow.copy(alpha = 0.6f) else DarkBorder, CircleShape)
                    .testTag("night_mode_quick_btn")
            ) {
                Icon(
                    imageVector = if (isNightModeActive) Icons.Default.NightsStay else Icons.Default.Bedtime,
                    contentDescription = "Modo Nocturno",
                    tint = if (isNightModeActive) AmberYellow else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Connection status pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (state.isConnected) Color(0x1F22C55E) else if (state.isConnecting || state.isDiscoveringHost) Color(0x1FF59E0B) else Color(0x1FEF4444),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.isConnected) NeonGreen.copy(alpha = 0.5f) else if (state.isConnecting || state.isDiscoveringHost) AmberYellow.copy(alpha = 0.5f) else CoralRed.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .clickable { onQuickReconnect() }
                    .testTag("status_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (state.isConnected) NeonGreen else if (state.isConnecting || state.isDiscoveringHost) AmberYellow else CoralRed)
                    )
                    Text(
                        text = if (state.isConnected) "Conectado" else if (state.isDiscoveringHost) "Buscando..." else if (state.isConnecting) "Enlazando" else "Buscar Host",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.isConnected) NeonGreen else if (state.isConnecting || state.isDiscoveringHost) AmberYellow else CoralRed
                    )
                }
            }

            IconButton(
                onClick = onScanQr,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x2210B981))
                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), CircleShape)
                    .testTag("scan_qr_top_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Escanear QR de Vinculación",
                    tint = NeonGreen,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(GlassSurface)
                    .border(1.dp, DarkBorder, CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ajustes",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LocalWhisperAsrStatusCard(
    state: com.example.voice.ClientUiState,
    onToggleService: () -> Unit
) {
    val isEngineRunning = state.isServiceRunning || state.micState == ClientMicState.LISTENING_STANDBY
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("whisper_asr_status_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isEngineRunning) NeonGreen.copy(alpha = 0.5f) else DarkBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isEngineRunning) Color(0x2210B981) else Color(0x226B7280)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Motor Whisper Local",
                            tint = if (isEngineRunning) NeonGreen else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Motor Whisper Local (Offline 24/7)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isEngineRunning) NeonGreen else CoralRed)
                            )
                            Text(
                                text = if (isEngineRunning) "ACTIVO • 4 Hilos CPU (Offline)" else "INACTIVO • Toca para iniciar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isEngineRunning) NeonGreen else TextMuted
                            )
                        }
                    }
                }

                Switch(
                    checked = isEngineRunning,
                    onCheckedChange = { onToggleService() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ZenOledBlack,
                        checkedTrackColor = NeonGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkBorder
                    ),
                    modifier = Modifier.testTag("whisper_service_toggle_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Mic Audio Meter & Tech specs banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Zero Cloud",
                            tint = CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "16kHz Mono • VAD Adaptativo RMS: ${state.rmsLevel.toInt()} dB",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Text(
                        text = if (state.rmsLevel > 25f) "VOZ DETECTADA" else "LISTO 24/7",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.rmsLevel > 25f) NeonGreen else CyanAccent
                    )
                }

                // Audio level indicator bar
                val audioMeterProgress = (state.rmsLevel / 80f).coerceIn(0.05f, 1.0f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x336B7280))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(audioMeterProgress)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (state.rmsLevel > 25f) NeonGreen else CyanAccent)
                    )
                }
            }
        }
    }
}

@Composable
private fun CentralMicrophoneCard(
    state: com.example.voice.ClientUiState,
    settings: ClientSettings,
    onToggleContinuous: () -> Unit,
    onPttDown: () -> Unit,
    onPttUp: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state.isServiceRunning || state.isPushToTalk) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Parpadeo brillante del botón 3D cuando el host envía o tiene información para mostrar
    val mediaBlinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "portraitMediaBlinkAlpha"
    )
    val mediaBlinkScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "portraitMediaBlinkScale"
    )

    val activeColor by animateColorAsState(
        targetValue = when (state.micState) {
            ClientMicState.RECORDING_SPEECH -> AmberYellow
            ClientMicState.TRANSMITTING -> PurpleAccent
            ClientMicState.GEMINI_PROCESSING -> PurpleAccent
            ClientMicState.COMMAND_SUCCESS -> NeonGreen
            ClientMicState.COMMAND_ERROR -> CoralRed
            ClientMicState.LISTENING_STANDBY -> if (state.isServiceRunning) NeonGreen else CyanAccent
            ClientMicState.CONNECTING -> AmberYellow
            ClientMicState.IDLE_DISCONNECTED -> if (state.isServiceRunning) CyanAccent else Color(0xFF4B5563)
        },
        animationSpec = tween(300),
        label = "activeColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mic_main_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Wake word badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x1A38BDF8))
                    .border(1.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Palabra Clave: '${state.wakeWord}'",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = CyanAccent
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fila horizontal: Orbe de micrófono a la izquierda y Botón 3D Cover animado a su lado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 1. Orbe de Micrófono (movido a la izquierda para que ambos quepan)
                Box(
                    modifier = Modifier.size(145.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                        val baseRadius = (size.minDimension / 2f) * 0.72f
                        val rmsBonus = state.rmsLevel * 4.5f

                        // Outer halo ring
                        if (state.isServiceRunning || state.isPushToTalk) {
                            drawCircle(
                                color = activeColor.copy(alpha = 0.12f),
                                radius = (baseRadius + 18f + rmsBonus) * pulseScale,
                                center = canvasCenter
                            )
                            drawCircle(
                                color = activeColor.copy(alpha = 0.25f),
                                radius = (baseRadius + 8f + (rmsBonus * 0.6f)),
                                center = canvasCenter,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Inner border ring
                        drawCircle(
                            color = activeColor.copy(alpha = 0.8f),
                            radius = baseRadius,
                            center = canvasCenter,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Central Interactive Glass Orb
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        activeColor.copy(alpha = 0.9f),
                                        activeColor.copy(alpha = 0.4f),
                                        Color(0xFF1E1E1E)
                                    )
                                )
                            )
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()
                                    var isHoldingPtt = false

                                    try {
                                        onPttDown()
                                        isHoldingPtt = true
                                        val up = waitForUpOrCancellation()
                                        up?.consume()
                                        if (isHoldingPtt) {
                                            onPttUp()
                                        }
                                    } catch (e: Exception) {
                                        if (isHoldingPtt) {
                                            onPttUp()
                                        }
                                    }
                                }
                            }
                            .testTag("mic_center_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isServiceRunning || state.isPushToTalk) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Micrófono",
                            tint = ZenOledBlack,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 2. Orbe de Carátulas 3D Cover Flow (mismo tamaño y orbe de animación que el de micrófono)
                val hasMediaData = state.hasMediaDisplayInfo || state.currentSongTitle.isNotBlank() || state.allMediaCards.isNotEmpty()
                Box(
                    modifier = Modifier
                        .size(145.dp)
                        .clickable {
                            ClientStateHolder.setShowFloating3dCoverFlow(true)
                        }
                        .testTag("portrait_blinking_3d_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                        val baseRadius = (size.minDimension / 2f) * 0.72f

                        // Outer glowing / pulsing halo ring
                        if (hasMediaData) {
                            drawCircle(
                                color = CyanAccent.copy(alpha = 0.15f * mediaBlinkAlpha),
                                radius = (baseRadius + 18f) * mediaBlinkScale,
                                center = canvasCenter
                            )
                            drawCircle(
                                color = CyanAccent.copy(alpha = 0.35f * mediaBlinkAlpha),
                                radius = baseRadius + 8f,
                                center = canvasCenter,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        } else {
                            drawCircle(
                                color = CyanAccent.copy(alpha = 0.08f),
                                radius = (baseRadius + 12f) * pulseScale,
                                center = canvasCenter
                            )
                        }

                        // Inner border ring
                        drawCircle(
                            color = if (hasMediaData) CyanAccent.copy(alpha = mediaBlinkAlpha.coerceAtLeast(0.7f)) else CyanAccent.copy(alpha = 0.5f),
                            radius = baseRadius,
                            center = canvasCenter,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Central Interactive Glass Orb
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = if (hasMediaData) listOf(
                                        CyanAccent.copy(alpha = 0.85f),
                                        CyanAccent.copy(alpha = 0.40f),
                                        Color(0xFF0F1E2E)
                                    ) else listOf(
                                        Color(0xFF334155),
                                        Color(0xFF1E293B),
                                        Color(0xFF090D16)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewCarousel,
                            contentDescription = "Carátulas 3D",
                            tint = if (hasMediaData) ZenOledBlack else Color(0xFFCBD5E1),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Satélite radar indicador en la parte superior derecha
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-12).dp, y = 12.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (hasMediaData) NeonGreen.copy(alpha = mediaBlinkAlpha) else Color(0xFF64748B))
                            .border(2.5.dp, ZenOledBlack, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // State Message Display
            Text(
                text = state.statusMessage,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Wake word active window countdown indicator
            AnimatedVisibility(visible = state.isWakeWordActive && state.wakeWindowRemainingSecs > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33F59E0B))
                        .border(1.dp, AmberYellow, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AmberYellow)
                        )
                        Text(
                            text = "Micrófono abierto (${state.wakeWindowRemainingSecs}s restantes)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberYellow
                        )
                    }
                }
            }

            // Live Speech Recognition partial transcription
            AnimatedVisibility(visible = state.partialText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33F59E0B))
                        .border(1.dp, AmberYellow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "“${state.partialText}”",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AmberYellow,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Control Mode Buttons: Continuous vs PTT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Continuous Toggle Button
                Button(
                    onClick = onToggleContinuous,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("toggle_continuous_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isServiceRunning) NeonGreen else GlassSurface,
                        contentColor = if (state.isServiceRunning) ZenOledBlack else TextPrimary
                    ),
                    border = if (!state.isServiceRunning) androidx.compose.foundation.BorderStroke(1.dp, DarkBorder) else null
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isServiceRunning) Icons.Default.Mic else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (state.isServiceRunning) "Detener Escucha" else "Manos Libres",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Push to Talk Button (Hold to speak)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (state.isPushToTalk) AmberYellow else GlassSurface)
                        .border(
                            1.dp,
                            if (state.isPushToTalk) AmberYellow else DarkBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                var isHolding = false
                                try {
                                    onPttDown()
                                    isHolding = true
                                    val up = waitForUpOrCancellation()
                                    up?.consume()
                                    if (isHolding) {
                                        onPttUp()
                                    }
                                } catch (e: Exception) {
                                    if (isHolding) {
                                        onPttUp()
                                    }
                                }
                            }
                        }
                        .testTag("ptt_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (state.isPushToTalk) ZenOledBlack else TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (state.isPushToTalk) "Soltar para Enviar" else "Mantener p/ Hablar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isPushToTalk) ZenOledBlack else TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HostConnectionCard(
    state: com.example.voice.ClientUiState,
    settings: ClientSettings,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onTestPing: () -> Unit,
    onOpenConfig: () -> Unit,
    onScanQr: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("host_connection_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (state.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = if (state.isConnected) NeonGreen else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Enlace con Reproductor Host",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (state.pingLatencyMs >= 0) {
                    Text(
                        text = "⚡ ${state.pingLatencyMs}ms",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonGreen
                    )
                }
            }

            // QR Code Pairing Action Button (Mandatory Feature)
            Button(
                onClick = onScanQr,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("scan_qr_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = ZenOledBlack
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Escanear QR de Vinculación",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Escanear QR de Vinculación",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // IP Display bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF191919))
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dirección IP / WebSocket e Identificador de Sala",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Text(
                        text = if (settings.hostIp.isNotBlank()) "${settings.hostIp} (${settings.satelliteRoom})" else "No configurado",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = if (settings.hostIp.isNotBlank()) TextPrimary else CoralRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Ping test button
                    OutlinedButton(
                        onClick = onTestPing,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Test Ping",
                            tint = CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ping", fontSize = 11.sp, color = CyanAccent)
                    }

                    // Config button
                    Button(
                        onClick = onOpenConfig,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlassSurface),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Config", fontSize = 11.sp, color = TextPrimary)
                    }
                }
            }

            // Connection Status Feedback Banner
            if (state.connectionMessage.isNotBlank()) {
                val isError = !state.isConnected && !state.isConnecting && (
                    state.connectionMessage.contains("Error", ignoreCase = true) ||
                    state.connectionMessage.contains("Fallo", ignoreCase = true) ||
                    state.connectionMessage.contains("refused", ignoreCase = true) ||
                    state.connectionMessage.contains("timeout", ignoreCase = true)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                state.isConnected -> Color(0x2210B981)
                                state.isConnecting -> Color(0x2206B6D4)
                                isError -> Color(0x33EF4444)
                                else -> Color(0x22374151)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                state.isConnected -> NeonGreen.copy(alpha = 0.4f)
                                state.isConnecting -> CyanAccent.copy(alpha = 0.4f)
                                isError -> CoralRed.copy(alpha = 0.6f)
                                else -> Color.Transparent
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = state.connectionMessage,
                        fontSize = 11.sp,
                        color = when {
                            state.isConnected -> NeonGreen
                            state.isConnecting -> CyanAccent
                            isError -> CoralRed
                            else -> TextSecondary
                        }
                    )
                }
            }

            // Connection action button
            if (!state.isConnected) {
                Button(
                    onClick = onConnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = ZenOledBlack)
                ) {
                    Icon(
                        imageVector = Icons.Default.CastConnected,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Conectar con PC / Host Principal", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.5f))
                ) {
                    Text("Desconectar Enlace", fontSize = 12.sp, color = CoralRed)
                }
            }
        }
    }
}

@Composable
fun HostVolumeSliderControl(
    volume: Int,
    isMuted: Boolean,
    onVolumeChange: (Int) -> Unit,
    onVolumeChangeFinished: (Int) -> Unit,
    onAdjustDelta: (Int) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember(volume) { mutableFloatStateOf(volume.toFloat().coerceIn(1f, 15f)) }
    val effectiveVol = if (isMuted) 0 else sliderPosition.roundToInt().coerceIn(1, 15)

    val volumeIcon = when {
        isMuted || effectiveVol == 0 -> Icons.Default.VolumeMute
        effectiveVol <= 5 -> Icons.Default.VolumeDown
        else -> Icons.Default.VolumeUp
    }
    val volumeColor = when {
        isMuted || effectiveVol == 0 -> CoralRed
        effectiveVol >= 13 -> AmberYellow
        else -> CyanAccent
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141414))
            .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: Icon + Title + Level Badge (1 a 15)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(volumeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = volumeIcon,
                        contentDescription = "Volumen del Host",
                        tint = volumeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "VOLUMEN DEL HOST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isMuted) "Silenciado temporalmente" else "Escala de 1 a 15 niveles",
                        fontSize = 10.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Level Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = volumeColor.copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(1.dp, volumeColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = if (isMuted) "MUTE" else "Nivel $effectiveVol / 15",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = volumeColor,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Interactive Slider (1f..15f, steps = 13)
        Slider(
            value = sliderPosition.coerceIn(1f, 15f),
            onValueChange = { newVal ->
                sliderPosition = newVal
                onVolumeChange(newVal.roundToInt().coerceIn(1, 15))
            },
            onValueChangeFinished = {
                onVolumeChangeFinished(sliderPosition.roundToInt().coerceIn(1, 15))
            },
            valueRange = 1f..15f,
            steps = 13,
            colors = SliderDefaults.colors(
                thumbColor = volumeColor,
                activeTrackColor = volumeColor,
                inactiveTrackColor = Color(0x334B5563)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("host_volume_slider")
        )

        // Presets & Quick adjustments row (-1, Nivel 8, +1, Nivel 15)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute button
            Surface(
                modifier = Modifier
                    .weight(1.2f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleMute() },
                color = if (isMuted) CoralRed.copy(alpha = 0.25f) else Color(0xFF222222),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isMuted) CoralRed else Color(0x22FFFFFF)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isMuted) "Desmutear" else "Mute",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMuted) CoralRed else TextSecondary
                    )
                }
            }

            // -1 Nivel button
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAdjustDelta(-1) },
                color = Color(0xFF222222),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("-1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            // Nivel 8 Preset button
            Surface(
                modifier = Modifier
                    .weight(1.2f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onVolumeChangeFinished(8) },
                color = if (!isMuted && effectiveVol == 8) CyanAccent.copy(alpha = 0.2f) else Color(0xFF222222),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (!isMuted && effectiveVol == 8) CyanAccent else Color(0x22FFFFFF)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Nivel 8",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isMuted && effectiveVol == 8) CyanAccent else TextPrimary
                    )
                }
            }

            // +1 Nivel button
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAdjustDelta(1) },
                color = Color(0xFF222222),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            // Nivel 15 Preset button
            Surface(
                modifier = Modifier
                    .weight(1.2f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onVolumeChangeFinished(15) },
                color = if (!isMuted && effectiveVol == 15) NeonGreen.copy(alpha = 0.2f) else Color(0xFF222222),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (!isMuted && effectiveVol == 15) NeonGreen else Color(0x22FFFFFF)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Nivel 15",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isMuted && effectiveVol == 15) NeonGreen else TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingVolumeHudOverlay(
    visible: Boolean,
    volume: Int,
    isMuted: Boolean,
    lastChangeTime: Long,
    onVolumeChange: (Int) -> Unit,
    onVolumeChangeFinished: (Int) -> Unit,
    onAdjustDelta: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-dismiss after 3.5 seconds
    LaunchedEffect(visible, lastChangeTime) {
        if (visible) {
            delay(3500L)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("floating_volume_hud")
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF5111827)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanAccent.copy(alpha = 0.7f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val icon = when {
                            isMuted || volume == 0 -> Icons.Default.VolumeMute
                            volume <= 5 -> Icons.Default.VolumeDown
                            else -> Icons.Default.VolumeUp
                        }
                        val tint = if (isMuted || volume == 0) CoralRed else CyanAccent

                        Icon(
                            imageVector = icon,
                            contentDescription = "Volumen",
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isMuted) "Volumen Silenciado" else "Volumen del Host: Nivel $volume / 15",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                contentDescription = "Mute",
                                tint = if (isMuted) NeonGreen else CoralRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                var sliderVal by remember(volume) { mutableFloatStateOf(volume.toFloat().coerceIn(1f, 15f)) }
                Slider(
                    value = sliderVal.coerceIn(1f, 15f),
                    onValueChange = { newVal ->
                        sliderVal = newVal
                        onVolumeChange(newVal.roundToInt().coerceIn(1, 15))
                    },
                    onValueChangeFinished = {
                        onVolumeChangeFinished(sliderVal.roundToInt().coerceIn(1, 15))
                    },
                    valueRange = 1f..15f,
                    steps = 13,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanAccent,
                        activeTrackColor = CyanAccent,
                        inactiveTrackColor = Color(0x334B5563)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickRemoteControlsCard(
    state: com.example.voice.ClientUiState,
    onSendCommand: (cmdType: String, label: String) -> Unit,
    onSetVolume: (Int) -> Unit,
    onAdjustVolumeDelta: (Int) -> Unit,
    onToggleMute: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_controls_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Control Remoto Directo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (state.isConnected) NeonGreen.copy(alpha = 0.15f) else AmberYellow.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (state.isConnected) NeonGreen.copy(alpha = 0.3f) else AmberYellow.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (state.isConnected) "HOST ACTIVO" else "MODO DIRECTO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isConnected) NeonGreen else AmberYellow,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Playback controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.FastRewind,
                    label = "Anterior",
                    modifier = Modifier.weight(1f),
                    onClick = { onSendCommand("PREVIOUS", "Anterior canción") }
                )
                QuickActionButton(
                    icon = Icons.Default.PlayArrow,
                    label = "Play",
                    color = NeonGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onSendCommand("PLAY", "Reanudar música") }
                )
                QuickActionButton(
                    icon = Icons.Default.Pause,
                    label = "Pausa",
                    color = AmberYellow,
                    modifier = Modifier.weight(1f),
                    onClick = { onSendCommand("PAUSE", "Pausar música") }
                )
                QuickActionButton(
                    icon = Icons.Default.FastForward,
                    label = "Siguiente",
                    modifier = Modifier.weight(1f),
                    onClick = { onSendCommand("NEXT", "Siguiente canción") }
                )
            }

            // Volume controls section with slider and quick presets
            HostVolumeSliderControl(
                volume = state.hostVolume,
                isMuted = state.isHostMuted,
                onVolumeChange = { ClientStateHolder.setHostVolume(it, showHud = false) },
                onVolumeChangeFinished = onSetVolume,
                onAdjustDelta = onAdjustVolumeDelta,
                onToggleMute = onToggleMute
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = TextPrimary,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = Color(0xFF1C1C1C),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun LiveActivityLogCard(
    logs: List<com.example.voice.ClientLogItem>,
    onClear: () -> Unit,
    onRefreshEngine: (() -> Unit)? = null
) {
    val listState = rememberLazyListState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activity_log_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Registro de Transmisión",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRefreshEngine != null) {
                        IconButton(
                            onClick = onRefreshEngine,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refrescar motor de voz",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (logs.isNotEmpty()) {
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Limpiar registro",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin actividad reciente. Di la palabra clave o pulsa un botón.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF141414))
                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(logs, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = item.time,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                            Text(
                                text = item.text,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (item.isError) CoralRed else if (item.isIncoming) NeonGreen else TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeminiVoiceAssistantCard(
    settings: ClientSettings,
    onOpenAssistant: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenAssistant() }
            .testTag("gemini_voice_assistant_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13141F)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Asistente Gemini AI Live",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Modelo: ${settings.geminiModelName}",
                            fontSize = 11.sp,
                            color = Color(0xFFA855F7)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x33A855F7),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Abrir Live",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Text(
                text = "Habla con naturalidad para pedir recomendaciones, playlists temáticas o canciones complejas. Gemini interpreta tu voz en tiempo real con IA.",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun VoiceCommandsGuideCard(
    wakeWord: String,
    directCommandsEnabled: Boolean,
    wakeWindowSeconds: Int,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_commands_guide_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Guía de Comandos y Flexibilidad",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ajustar sensibilidad",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Quick Info Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1810B981))
                    .border(1.dp, NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (directCommandsEnabled) {
                        "✨ Detección directa activa: Puedes hablar directamente ('Pon Queen', 'Pausa') o decir '${wakeWord}' y tomarte hasta ${wakeWindowSeconds}s para decidir."
                    } else {
                        "✨ Di '${wakeWord}' y el sistema te esperará hasta ${wakeWindowSeconds}s para que digas tu comando con calma."
                    },
                    fontSize = 11.sp,
                    color = NeonGreen,
                    lineHeight = 16.sp
                )
            }

            // Command Examples Grid
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "🎵 Buscar Canción" to "'Pon Queen', 'Buscar Michael Jackson', 'Reproduce Bohemian Rhapsody'",
                    "⏯️ Reproducción" to "'Pausa', 'Play', 'Reanudar', 'Detener'",
                    "⏭️ Navegación" to "'Siguiente', 'Pasa canción', 'Anterior', 'Repetir'",
                    "🔊 Volumen" to "'Sube volumen', 'Baja volumen', 'Volumen 8', 'Nivel 15', 'Silencio / Mute'"
                ).forEach { (category, examples) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161616))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            modifier = Modifier.width(100.dp)
                        )
                        Text(
                            text = examples,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientSettingsDialog(
    settings: ClientSettings,
    onDismiss: () -> Unit,
    onTriggerNightModeNow: () -> Unit,
    onSave: (
        ip: String,
        room: String,
        port: Int,
        alias: String,
        wakeWord: String,
        autoConnect: Boolean,
        muteBeeps: Boolean,
        keepScreen: Boolean,
        directCmds: Boolean,
        wakeWinSecs: Int,
        autoStartMic: Boolean,
        autoRefresh: Boolean,
        autoRefreshMins: Int,
        nodeMeshSync: Boolean,
        speechLangMode: String,
        phoneticFix: Boolean,
        startOnBoot: Boolean,
        dailyReboot: Boolean,
        rebootH: Int,
        rebootM: Int,
        nightSchedule: Boolean,
        startH: Int,
        startM: Int,
        endH: Int,
        endM: Int,
        screenOffComplete: Boolean,
        tapToWake: Boolean,
        voiceWake: Boolean,
        fullScreenClock: Boolean,
        dimLevel: Float,
        burnIn: Boolean,
        geminiEnhance: Boolean,
        geminiTts: Boolean,
        geminiModel: String,
        customApiKey: String
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var ipInput by remember { mutableStateOf(settings.hostIp) }
    var roomInput by remember { mutableStateOf(settings.satelliteRoom) }
    var portInput by remember { mutableStateOf(settings.hostPort.toString()) }
    var aliasInput by remember { mutableStateOf(settings.deviceAlias) }
    var wakeWordInput by remember { mutableStateOf(settings.wakeWord) }
    var autoConnectInput by remember { mutableStateOf(settings.autoConnectOnLaunch) }
    var muteBeepsInput by remember { mutableStateOf(settings.muteRecognizerBeeps) }
    var keepScreenInput by remember { mutableStateOf(settings.keepScreenOn) }
    var keepQueueOpenDuringPlaybackInput by remember { mutableStateOf(settings.keepQueueOpenDuringPlayback) }
    var directCommandsInput by remember { mutableStateOf(settings.directCommandsEnabled) }
    var wakeWordTriggersHostMicInput by remember { mutableStateOf(settings.wakeWordTriggersHostMicDirectly) }
    var wakeWindowSecsInput by remember { mutableStateOf(settings.wakeWindowSeconds) }
    var autoStartListeningInput by remember { mutableStateOf(settings.autoStartListeningOnLaunch) }
    var autoRefreshInput by remember { mutableStateOf(settings.autoRefreshEnabled) }
    var autoRefreshMinsInput by remember { mutableStateOf(settings.autoRefreshMinutes) }
    var nodeMeshSyncInput by remember { mutableStateOf(settings.nodeMeshSyncEnabled) }

    // Speech Recognition & Language Settings
    var speechLanguageModeInput by remember { mutableStateOf(settings.speechLanguageMode) }
    var bilingualPhoneticFixInput by remember { mutableStateOf(settings.bilingualPhoneticFixEnabled) }
    var isDiscoveringInDialog by remember { mutableStateOf(false) }

    // Boot & Reboot States
    var startOnBootInput by remember { mutableStateOf(settings.startOnBootEnabled) }
    var dailyRebootInput by remember { mutableStateOf(settings.dailyRebootEnabled) }
    var dailyRebootHourInput by remember { mutableIntStateOf(settings.dailyRebootHour) }
    var dailyRebootMinuteInput by remember { mutableIntStateOf(settings.dailyRebootMinute) }
    var isTestingReboot by remember { mutableStateOf(false) }

    // Gemini AI States
    var geminiEnhanceInput by remember { mutableStateOf(settings.geminiAiVoiceEnhanceEnabled) }
    var geminiTtsInput by remember { mutableStateOf(settings.geminiVoiceTtsEnabled) }
    var geminiModelInput by remember { mutableStateOf(settings.geminiModelName) }
    var customApiKeyInput by remember { mutableStateOf(settings.customGeminiApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isTestingGemini by remember { mutableStateOf(false) }
    var geminiTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Night Mode & Deep Sleep States
    var nightScheduleInput by remember { mutableStateOf(settings.nightModeScheduleEnabled) }
    var startHourInput by remember { mutableStateOf(settings.nightStartHour) }
    var startMinInput by remember { mutableStateOf(settings.nightStartMinute) }
    var endHourInput by remember { mutableStateOf(settings.nightEndHour) }
    var endMinInput by remember { mutableStateOf(settings.nightEndMinute) }
    var screenOffCompleteInput by remember { mutableStateOf(settings.nightScreenOffComplete) }
    var tapToWakeInput by remember { mutableStateOf(settings.nightTapToWake) }
    var voiceWakeInput by remember { mutableStateOf(settings.nightVoiceWake) }
    var fullScreenClockInput by remember { mutableStateOf(settings.nightShowFullScreenClock) }
    var dimLevelInput by remember { mutableFloatStateOf(settings.nightDimLevel) }
    var burnInProtectInput by remember { mutableStateOf(settings.nightClockBurnInProtection) }

    val dialogQrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val parsed = QrCodePairingHelper.parseQrCodePayload(result.contents)
            if (parsed != null) {
                QrCodePairingHelper.triggerScanHapticFeedback(context)
                ipInput = parsed.wsUrl
                roomInput = parsed.room
                settings.satelliteRoom = parsed.room
                Toast.makeText(context, "QR Escaneado: ${parsed.wsUrl} (Sala: ${parsed.room})", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Formato QR no válido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceCard,
        title = {
            Text(
                text = "Configuración del Satélite",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        label = { Text("IP / WebSocket del Reproductor Principal (Host)") },
                        placeholder = { Text("ej: 192.168.1.100 ó wss://.../ws") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_host_ip_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isDiscoveringInDialog = true
                                coroutineScope.launch {
                                    Toast.makeText(context, "Buscando Host en la red Wi-Fi...", Toast.LENGTH_SHORT).show()
                                    val discovered = NetworkHelper.discoverHostOnLocalNetwork(
                                        preferredPort = portInput.toIntOrNull() ?: 8998,
                                        previousIp = ipInput
                                    )
                                    isDiscoveringInDialog = false
                                    if (discovered != null) {
                                        ipInput = discovered.ip
                                        portInput = discovered.port.toString()
                                        Toast.makeText(context, "¡Host detectado en ${discovered.ip}:${discovered.port}! (${discovered.method})", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "No se detectó ningún Host en los puertos 8998 ni 3000", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = ZenOledBlack)
                        ) {
                            if (isDiscoveringInDialog) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ZenOledBlack, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buscando...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.Cast, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Autodescubrir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                try {
                                    dialogQrLauncher.launch(QrCodePairingHelper.createScanOptions())
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error al abrir la cámara: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = ZenOledBlack)
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Escanear QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = roomInput,
                        onValueChange = { 
                            roomInput = it 
                            settings.satelliteRoom = it.trim()
                        },
                        label = { Text("Sala / Room (ej: serchtube-master)") },
                        placeholder = { Text("serchtube-master") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Puerto de red (Default: 8998 / 3000)") },
                            placeholder = { Text("8998 o 3000") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        // Selector rápido de los dos puertos por defecto
                        val currentPortInt = portInput.toIntOrNull()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentPortInt == 8998) CyanAccent.copy(alpha = 0.22f) else Color(0x1AFFFFFF),
                                border = BorderStroke(1.dp, if (currentPortInt == 8998) CyanAccent else DarkBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { portInput = "8998" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (currentPortInt == 8998) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        "Puerto 8998",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (currentPortInt == 8998) FontWeight.Bold else FontWeight.Medium,
                                        color = if (currentPortInt == 8998) CyanAccent else TextSecondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentPortInt == 3000) CyanAccent.copy(alpha = 0.22f) else Color(0x1AFFFFFF),
                                border = BorderStroke(1.dp, if (currentPortInt == 3000) CyanAccent else DarkBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { portInput = "3000" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (currentPortInt == 3000) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        "Puerto 3000",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (currentPortInt == 3000) FontWeight.Bold else FontWeight.Medium,
                                        color = if (currentPortInt == 3000) CyanAccent else TextSecondary
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Puertos por defecto: 8998 y 3000. El autodescubrimiento buscará en ambos puertos automáticamente.",
                            fontSize = 10.5.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = aliasInput,
                        onValueChange = { aliasInput = it },
                        label = { Text("Nombre de este dispositivo (Alias)") },
                        placeholder = { Text("ej: Micrófono Dormitorio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                item {
                    OutlinedTextField(
                        value = wakeWordInput,
                        onValueChange = { wakeWordInput = it },
                        label = { Text("Palabra clave de activación") },
                        placeholder = { Text("ej: Música") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                // Instant Host Microphone Trigger Option
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (wakeWordTriggersHostMicInput) Color(0x2806B6D4) else Color(0x1A1E293B))
                            .border(
                                1.dp,
                                if (wakeWordTriggersHostMicInput) CyanAccent else DarkBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (wakeWordTriggersHostMicInput) CyanAccent else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Comando directo: \"Activa tu micrófono\"",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (wakeWordTriggersHostMicInput) CyanAccent else TextPrimary
                                    )
                                    Text(
                                        text = "Al detectar '${wakeWordInput.ifBlank { "Música" }}', ordena al Host activar su micrófono al instante sin transcribir ni procesar peticiones en el teléfono (cero retardos)",
                                        fontSize = 10.5.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            Switch(
                                checked = wakeWordTriggersHostMicInput,
                                onCheckedChange = {
                                    wakeWordTriggersHostMicInput = it
                                    settings.wakeWordTriggersHostMicDirectly = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent),
                                modifier = Modifier.testTag("switch_wake_triggers_host_mic")
                            )
                        }
                    }
                }

                // Flexible Voice Commands Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x2210B981))
                            .border(1.dp, NeonGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Detección Flexible y Comandos Directos",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Habla de forma natural sin trabas ni rigidez",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // Direct commands toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Comandos directos sin palabra clave", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Permite decir directamente 'Pon Queen', 'Pausa', 'Siguiente', 'Sube volumen' sin tener que decir 'Música'", fontSize = 10.sp, color = TextMuted)
                            }
                            Switch(
                                checked = directCommandsInput,
                                onCheckedChange = { directCommandsInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                            )
                        }

                        // Auto-start continuous listening on launch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Escuchar siempre al abrir la app", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("El micrófono inicia activo de inmediato sin quedar en reposo", fontSize = 10.sp, color = TextMuted)
                            }
                            Switch(
                                checked = autoStartListeningInput,
                                onCheckedChange = { autoStartListeningInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                            )
                        }

                        // Wake Window Duration Selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Tiempo de espera tras decir '${wakeWordInput.ifBlank { "Música" }}':", fontSize = 11.sp, color = TextSecondary)
                            Text("Si dices la palabra clave, tienes este tiempo para decir tu canción con calma:", fontSize = 10.sp, color = TextMuted)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(5 to "5 seg", 8 to "8 seg", 10 to "10 seg", 15 to "15 seg").forEach { (secs, label) ->
                                    val isSelected = wakeWindowSecsInput == secs
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) NeonGreen else Color(0x331E1E1E),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonGreen else DarkBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { wakeWindowSecsInput = secs }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) ZenOledBlack else TextPrimary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bilingual Speech Recognition & Pronunciation Engine Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x240284C7))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Idioma y Reconocimiento de Voz",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Distingue y captura con precisión palabras en inglés y español",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // Language Mode Selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Modo de idioma del micrófono:", fontSize = 11.sp, color = TextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "bilingual" to "🌐 Bilingüe (Es + En)",
                                    "es" to "🇲🇽 Español",
                                    "en" to "🇺🇸 English"
                                ).forEach { (modeKey, label) ->
                                    val isSelected = speechLanguageModeInput == modeKey
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0x331E1E1E),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else DarkBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { speechLanguageModeInput = modeKey }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) ZenOledBlack else TextPrimary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Smart Bilingual Phonetic Correction Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Corrección fonética inteligente", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Corrige automáticamente nombres en inglés capturados fonéticamente en español (ej: Queen, Coldplay, Billie Eilish, The Beatles, Guns N' Roses, Starboy, etc.)", fontSize = 10.sp, color = TextMuted)
                            }
                            Switch(
                                checked = bilingualPhoneticFixInput,
                                onCheckedChange = { bilingualPhoneticFixInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8))
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Conectar al iniciar app", fontSize = 13.sp, color = TextPrimary)
                            Text("Enlaza socket Wi-Fi automáticamente", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(
                            checked = autoConnectInput,
                            onCheckedChange = { autoConnectInput = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Silenciar pitidos del sistema", fontSize = 13.sp, color = TextPrimary)
                            Text("Evita ruidos de 'bip' continuos", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(
                            checked = muteBeepsInput,
                            onCheckedChange = { muteBeepsInput = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mantener pantalla activa", fontSize = 13.sp, color = TextPrimary)
                            Text("Ideal para soporte de mesa o dock", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(
                            checked = keepScreenInput,
                            onCheckedChange = { keepScreenInput = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mantener cola abierta en reproducción", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Mantiene la pantalla de la cola y carátulas 3D abierta mientras el host reproduzca música (sin auto-cierre)", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(
                            checked = keepQueueOpenDuringPlaybackInput,
                            onCheckedChange = { keepQueueOpenDuringPlaybackInput = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                        )
                    }
                }

                // Periodic Engine Auto-Refresh & RAM Liberator Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x221E293B))
                            .border(1.dp, CyanAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Liberador de RAM y Refresco Automático", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Limpia la memoria y optimiza los procesos periódicamente para mantener el teléfono siempre rápido", fontSize = 11.sp, color = TextMuted)
                            }
                            Switch(
                                checked = autoRefreshInput,
                                onCheckedChange = { autoRefreshInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                            )
                        }

                        if (autoRefreshInput) {
                            Text("Intervalo de liberación y auto-refresco:", fontSize = 12.sp, color = TextSecondary)
                            // 1st row of intervals: 1m, 3m, 5m, 15m
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1 to "1m", 3 to "3m", 5 to "5m", 15 to "15m").forEach { (mins, label) ->
                                    val isSelected = autoRefreshMinsInput == mins
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) CyanAccent else Color(0x331E1E1E),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyanAccent else DarkBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { autoRefreshMinsInput = mins }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) ZenOledBlack else TextPrimary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            // 2nd row of intervals: 30m, 45m, 60m, 120m
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(30 to "30m", 45 to "45m", 60 to "60m", 120 to "120m").forEach { (mins, label) ->
                                    val isSelected = autoRefreshMinsInput == mins
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) CyanAccent else Color(0x331E1E1E),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyanAccent else DarkBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { autoRefreshMinsInput = mins }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) ZenOledBlack else TextPrimary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                RemoteMicForegroundService.forceRefresh(context)
                                ClientStateHolder.addLog("[Ajustes] Liberador de RAM y procesos ejecutado")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x3306B6D4), contentColor = CyanAccent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Liberar RAM y Procesos Ahora", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Inter-Node Mesh & Music Sync Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x220F172A))
                            .border(1.dp, NeonGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sintonía Mesh entre Teléfonos Satélite", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Mantiene todos los teléfonos en contacto y sincronizados (volumen, comandos, música y estado general del host en tiempo real)", fontSize = 11.sp, color = TextMuted)
                            }
                            Switch(
                                checked = nodeMeshSyncInput,
                                onCheckedChange = { nodeMeshSyncInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                            )
                        }
                    }
                }

                // Auto-Start on Boot & Scheduled Reboot / 24-7 Maintenance Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x220284C7))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Auto-Arranque y Reinicio Programado 24/7",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Operación autónoma permanente sin intervención manual",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // 1. Iniciar con el sistema al encender el teléfono
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Abrir al encender el teléfono (Auto-inicio)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Inicia la app y activa el micrófono satélite automáticamente al prender o reiniciar el teléfono", fontSize = 10.sp, color = TextMuted)
                            }
                            Switch(
                                checked = startOnBootInput,
                                onCheckedChange = { startOnBootInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8))
                            )
                        }

                        if (startOnBootInput) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x22082F49))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val hasOverlay = AutostartHelper.hasOverlayPermission(context)
                                val isIgnoringBattery = AutostartHelper.isIgnoringBatteryOptimizations(context)

                                Text(
                                    "⚙️ Permisos requeridos para Auto-Arranque en Android:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )

                                // Overlay permission check (Required for Android 10+ background activity start)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "1. Mostrar sobre otras apps (Overlay)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (hasOverlay) Color(0xFF10B981) else Color(0xFFFBBF24)
                                        )
                                        Text(
                                            if (hasOverlay) "✓ Concedido (permite abrir la interfaz en segundo plano)" else "⚠️ Necesario para abrir la pantalla automáticamente al encender",
                                            fontSize = 9.5.sp,
                                            color = TextMuted
                                        )
                                    }
                                    if (!hasOverlay) {
                                        Button(
                                            onClick = { AutostartHelper.openOverlaySettings(context) },
                                            modifier = Modifier.height(30.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24), contentColor = Color.Black),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Conceder", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Battery optimization check
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "2. Batería sin restricciones",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isIgnoringBattery) Color(0xFF10B981) else Color(0xFFFBBF24)
                                        )
                                        Text(
                                            if (isIgnoringBattery) "✓ Sin restricciones (el sistema no cerrará el satélite)" else "⚠️ Recomendado para evitar que el sistema cierre la app al suspender",
                                            fontSize = 9.5.sp,
                                            color = TextMuted
                                        )
                                    }
                                    if (!isIgnoringBattery) {
                                        Button(
                                            onClick = { AutostartHelper.requestIgnoreBatteryOptimizations(context) },
                                            modifier = Modifier.height(30.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color.Black),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Ajustar", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // OEM Autostart settings button (Xiaomi, Samsung, Huawei, Oppo, etc.)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val opened = AutostartHelper.openOemAutostartSettings(context)
                                            if (!opened) {
                                                Toast.makeText(context, "Abre los ajustes de la app y activa 'Inicio Automático'", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3338BDF8), contentColor = Color(0xFF38BDF8)),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Ajustes Auto-inicio (MIUI/Samsung)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            AutostartHelper.launchMainActivityFromBoot(context)
                                            Toast.makeText(context, "Prueba de auto-arranque ejecutada", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.height(32.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3310B981), contentColor = Color(0xFF10B981)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("🧪 Probar arranque", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        androidx.compose.material3.HorizontalDivider(color = Color(0x3338BDF8))

                        // 2. Reiniciar el teléfono a una hora específica del día
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Reiniciar a una hora específica del día", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Previene fugas de memoria y mantiene el dispositivo al 100% de rendimiento", fontSize = 10.sp, color = TextMuted)
                            }
                            Switch(
                                checked = dailyRebootInput,
                                onCheckedChange = { dailyRebootInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8))
                            )
                        }

                        if (dailyRebootInput) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x22082F49))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Hora del reinicio diario:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                    Text(
                                        text = ScheduledRebootManager.getNextScheduledTimeFormatted(context),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF38BDF8)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Hour controls
                                    IconButton(
                                        onClick = { dailyRebootHourInput = if (dailyRebootHourInput > 0) dailyRebootHourInput - 1 else 23 },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                    }
                                    Text(
                                        text = "%02d".format(dailyRebootHourInput),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = { dailyRebootHourInput = (dailyRebootHourInput + 1) % 24 },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                    }

                                    Text(":", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), modifier = Modifier.padding(horizontal = 4.dp))

                                    // Minute controls
                                    IconButton(
                                        onClick = { dailyRebootMinuteInput = if (dailyRebootMinuteInput >= 5) dailyRebootMinuteInput - 5 else 55 },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                    }
                                    Text(
                                        text = "%02d".format(dailyRebootMinuteInput),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = { dailyRebootMinuteInput = (dailyRebootMinuteInput + 5) % 60 },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                    }
                                }

                                // Quick presets
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        Triple(4, 0, "04:00 AM (Madrugada)"),
                                        Triple(5, 30, "05:30 AM"),
                                        Triple(3, 0, "03:00 AM")
                                    ).forEach { (h, m, lbl) ->
                                        val isSelected = dailyRebootHourInput == h && dailyRebootMinuteInput == m
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) Color(0xFF38BDF8) else Color(0x331E1E1E),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else DarkBorder),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    dailyRebootHourInput = h
                                                    dailyRebootMinuteInput = m
                                                }
                                        ) {
                                            Text(
                                                text = lbl,
                                                fontSize = 9.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) ZenOledBlack else TextPrimary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 5.dp)
                                            )
                                        }
                                    }
                                }

                                // Intelligent Root & Maintenance Info Box
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x280284C7),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4438BDF8)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                                            Text("Modo Dual Inteligente (Root / Deep Refresh):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        }
                                        Text(
                                            "• Con Root: Reinicia el teléfono completo a nivel sistema operativo.\n• Sin Root: Recicla memoria RAM, reinicia el motor de captura y refresca la app para operar 24/7 sin bloqueos.",
                                            fontSize = 9.sp,
                                            color = TextSecondary,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }

                                // Test Button
                                Button(
                                    onClick = {
                                        val res = ScheduledRebootManager.performRebootOrMaintenance(context)
                                        when (res) {
                                            ScheduledRebootManager.RebootResult.ROOT_SYSTEM_REBOOT -> {
                                                Toast.makeText(context, "Reinicio del teléfono iniciado vía Root", Toast.LENGTH_LONG).show()
                                            }
                                            ScheduledRebootManager.RebootResult.SOFT_MAINTENANCE_REFRESH -> {
                                                Toast.makeText(context, "Mantenimiento profundo completado (RAM y satélite refrescados)", Toast.LENGTH_SHORT).show()
                                            }
                                            ScheduledRebootManager.RebootResult.FAILED -> {
                                                Toast.makeText(context, "Mantenimiento finalizado", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x3338BDF8), contentColor = Color(0xFF38BDF8)),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f))
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Probar Reinicio / Mantenimiento Ahora", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Gemini AI Voice & Natural Language Intelligence Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x282E1065))
                            .border(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFA855F7),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        "Optimizador de Voz Gemini AI",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        "Reconocimiento inteligente de lenguaje natural",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            Switch(
                                checked = geminiEnhanceInput,
                                onCheckedChange = { geminiEnhanceInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFA855F7))
                            )
                        }

                        if (geminiEnhanceInput) {
                            // TTS Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Respuestas por Voz (Sintetizador TTS)", fontSize = 12.sp, color = TextPrimary)
                                    Text("Gemini habla y confirma tus pedidos", fontSize = 10.sp, color = TextMuted)
                                }
                                Switch(
                                    checked = geminiTtsInput,
                                    onCheckedChange = { geminiTtsInput = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFA855F7))
                                )
                            }

                            // Model Selection
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Modelo de IA:", fontSize = 11.sp, color = TextSecondary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "gemini-3.5-flash" to "Flash 3.5",
                                        "gemini-3.1-flash-lite-preview" to "Lite 3.1",
                                        "gemini-3.1-pro-preview" to "Pro 3.1",
                                        "gemini-flash-latest" to "Flash Latest"
                                    ).forEach { (modelKey, label) ->
                                        val isSelected = geminiModelInput == modelKey
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) Color(0xFFA855F7) else Color(0x331E1E1E),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFFA855F7) else DarkBorder),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { geminiModelInput = modelKey }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) ZenOledBlack else TextPrimary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Gemini API Key Input
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("API Key de Google AI Studio:", fontSize = 11.sp, color = TextSecondary)
                                    val isConfigured = GeminiVoiceService.isConfigured(context) || customApiKeyInput.isNotBlank()
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isConfigured) Color(0x3322C55E) else Color(0x33EF4444)
                                    ) {
                                        Text(
                                            text = if (isConfigured) "Configurada" else "No configurada",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isConfigured) Color(0xFF22C55E) else Color(0xFFEF4444),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = customApiKeyInput,
                                    onValueChange = { 
                                        customApiKeyInput = it
                                        geminiTestResult = null 
                                    },
                                    placeholder = { Text("Pega tu API Key (AIzaSy...)", fontSize = 11.sp, color = TextMuted) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = TextPrimary),
                                    visualTransformation = if (isApiKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                            Icon(
                                                imageVector = if (isApiKeyVisible) Icons.Default.LockOpen else Icons.Default.Lock,
                                                contentDescription = "Toggle API Key visibility",
                                                tint = Color(0xFFA855F7),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFA855F7),
                                        unfocusedBorderColor = DarkBorder,
                                        focusedContainerColor = DarkSurfaceCard,
                                        unfocusedContainerColor = DarkSurfaceCard
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    text = "Obtén tu clave gratuita en aistudio.google.com o configúrala en Secrets.",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }

                            // Test Connection Button & Diagnostic Feedback
                            Button(
                                onClick = {
                                    isTestingGemini = true
                                    geminiTestResult = null
                                    coroutineScope.launch {
                                        val result = GeminiVoiceService.testConnection(
                                            explicitKey = customApiKeyInput,
                                            modelName = geminiModelInput,
                                            context = context
                                        )
                                        geminiTestResult = result
                                        isTestingGemini = false
                                    }
                                },
                                enabled = !isTestingGemini,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFA855F7).copy(alpha = 0.25f),
                                    contentColor = Color(0xFFD8B4FE)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.6f))
                            ) {
                                if (isTestingGemini) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFFA855F7), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Probando conexión con Gemini...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("⚡ Probar Conexión con Gemini AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            geminiTestResult?.let { (success, message) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (success) Color(0x2222C55E) else Color(0x22EF4444),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (success) Color(0xFF22C55E) else Color(0xFFEF4444)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = message,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (success) Color(0xFF4ADE80) else Color(0xFFFCA5A5),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Night Mode & Scheduled Screen Dimming Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x281B2234))
                            .border(1.dp, AmberYellow.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = AmberYellow,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        "Modo Nocturno Programado",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        "Oscurece la pantalla en horas seleccionadas",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            Switch(
                                checked = nightScheduleInput,
                                onCheckedChange = { nightScheduleInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = AmberYellow)
                            )
                        }

                        // Start & End Hour Pickers
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x22121212))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Horario de oscurecimiento:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Start Time
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Inicio (Noche)", fontSize = 10.sp, color = TextMuted)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { startHourInput = if (startHourInput > 0) startHourInput - 1 else 23 },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AmberYellow)
                                        }
                                        Text(
                                            text = "%02d:%02d".format(startHourInput, startMinInput),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextPrimary
                                        )
                                        IconButton(
                                            onClick = { startHourInput = (startHourInput + 1) % 24 },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AmberYellow)
                                        }
                                    }
                                }

                                Text("➔", fontSize = 14.sp, color = AmberYellow)

                                // End Time
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Fin (Mañana)", fontSize = 10.sp, color = TextMuted)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { endHourInput = if (endHourInput > 0) endHourInput - 1 else 23 },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AmberYellow)
                                        }
                                        Text(
                                            text = "%02d:%02d".format(endHourInput, endMinInput),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextPrimary
                                        )
                                        IconButton(
                                            onClick = { endHourInput = (endHourInput + 1) % 24 },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AmberYellow)
                                        }
                                    }
                                }
                            }
                        }

                        // Screen Off Completely (LCD/IPS life preservation & lock simulation)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (screenOffCompleteInput) Color(0x33000000) else Color(0x18000000),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (screenOffCompleteInput) AmberYellow.copy(alpha = 0.6f) else DarkBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Apagar Pantalla por Completo",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (screenOffCompleteInput) AmberYellow else TextPrimary
                                        )
                                        Text(
                                            "Simula bloqueo total para cuidar pantallas LCD/IPS que emiten luz aún en negro. El satélite sigue activo.",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                    Switch(
                                        checked = screenOffCompleteInput,
                                        onCheckedChange = { screenOffCompleteInput = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = AmberYellow)
                                    )
                                }

                                if (screenOffCompleteInput) {
                                    androidx.compose.material3.HorizontalDivider(color = Color(0x334B5563))

                                    // Tap to wake sub-option
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Despertar al Tocar (Tap-to-Wake)", fontSize = 11.sp, color = TextPrimary)
                                            Text("Un toque ilumina la pantalla temporalmente para ver la hora y mandos", fontSize = 9.sp, color = TextMuted)
                                        }
                                        Switch(
                                            checked = tapToWakeInput,
                                            onCheckedChange = { tapToWakeInput = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = AmberYellow)
                                        )
                                    }

                                    // Voice wake sub-option
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Despertar con Voz", fontSize = 11.sp, color = TextPrimary)
                                            Text("Al decir '$wakeWordInput' o dar una orden, se ilumina automáticamente", fontSize = 9.sp, color = TextMuted)
                                        }
                                        Switch(
                                            checked = voiceWakeInput,
                                            onCheckedChange = { voiceWakeInput = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = AmberYellow)
                                        )
                                    }
                                }
                            }
                        }

                        if (!screenOffCompleteInput) {
                            // Full Screen Clock Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Reloj Digital OLED Completo", fontSize = 12.sp, color = TextPrimary)
                                    Text("Muestra reloj minimalista tenue al oscurecerse", fontSize = 10.sp, color = TextMuted)
                                }
                                Switch(
                                    checked = fullScreenClockInput,
                                    onCheckedChange = { fullScreenClockInput = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = AmberYellow)
                                )
                            }

                            // Burn-in protection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Protección Anti-Quemado OLED", fontSize = 12.sp, color = TextPrimary)
                                    Text("Desplaza sutilmente el reloj para cuidar la pantalla", fontSize = 10.sp, color = TextMuted)
                                }
                                Switch(
                                    checked = burnInProtectInput,
                                    onCheckedChange = { burnInProtectInput = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = AmberYellow)
                                )
                            }

                            // Dim Level Selector
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Nivel de brillo en modo noche:", fontSize = 11.sp, color = TextSecondary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(0.01f to "1% Mínimo", 0.05f to "5% Nocturno", 0.15f to "15% Suave").forEach { (lvl, lbl) ->
                                        val isSelected = kotlin.math.abs(dimLevelInput - lvl) < 0.02f
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) AmberYellow else Color(0x331E1E1E),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AmberYellow else DarkBorder),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { dimLevelInput = lvl }
                                        ) {
                                            Text(
                                                text = lbl,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) ZenOledBlack else TextPrimary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Test / Activate Night Mode Button
                        Button(
                            onClick = onTriggerNightModeNow,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberYellow.copy(alpha = 0.2f), contentColor = AmberYellow),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberYellow.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                if (screenOffCompleteInput) Icons.Default.Bedtime else Icons.Default.NightsStay,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (screenOffCompleteInput) "Probar Pantalla Apagada Ahora" else "Activar Reloj Nocturno Ahora",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portInput.toIntOrNull() ?: 8998
                    settings.keepQueueOpenDuringPlayback = keepQueueOpenDuringPlaybackInput
                    settings.wakeWordTriggersHostMicDirectly = wakeWordTriggersHostMicInput
                    onSave(
                        ipInput.trim(),
                        roomInput.trim().ifBlank { "serchtube-master" },
                        port,
                        aliasInput.trim().ifBlank { "Mic Remoto" },
                        wakeWordInput.trim().ifBlank { "Música" },
                        autoConnectInput,
                        muteBeepsInput,
                        keepScreenInput,
                        directCommandsInput,
                        wakeWindowSecsInput,
                        autoStartListeningInput,
                        autoRefreshInput,
                        autoRefreshMinsInput,
                        nodeMeshSyncInput,
                        speechLanguageModeInput,
                        bilingualPhoneticFixInput,
                        startOnBootInput,
                        dailyRebootInput,
                        dailyRebootHourInput,
                        dailyRebootMinuteInput,
                        nightScheduleInput,
                        startHourInput,
                        startMinInput,
                        endHourInput,
                        endMinInput,
                        screenOffCompleteInput,
                        tapToWakeInput,
                        voiceWakeInput,
                        fullScreenClockInput,
                        dimLevelInput,
                        burnInProtectInput,
                        geminiEnhanceInput,
                        geminiTtsInput,
                        geminiModelInput,
                        customApiKeyInput.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = ZenOledBlack),
                modifier = Modifier.testTag("save_settings_btn")
            ) {
                Text("Guardar y Enlazar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun LandscapeFuturisticMicView(
    state: com.example.voice.ClientUiState,
    settings: ClientSettings,
    isNightModeActive: Boolean,
    onToggleNightMode: () -> Unit,
    onToggleContinuous: () -> Unit,
    onPttDown: () -> Unit,
    onPttUp: () -> Unit,
    onSendCommand: (cmdType: String, label: String) -> Unit,
    onSetVolume: (Int) -> Unit,
    onAdjustVolumeDelta: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onLiberateRam: () -> Unit,
    onOpenSettings: () -> Unit,
    onQuickReconnect: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "landscape_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state.isServiceRunning || state.isPushToTalk) 1.18f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Parpadeo brillante del botón 3D cuando el host envía o tiene información para mostrar
    val mediaBlinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mediaBlinkAlpha"
    )
    val mediaBlinkScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mediaBlinkScale"
    )

    val activeColor by animateColorAsState(
        targetValue = when (state.micState) {
            ClientMicState.RECORDING_SPEECH -> AmberYellow
            ClientMicState.TRANSMITTING -> PurpleAccent
            ClientMicState.GEMINI_PROCESSING -> PurpleAccent
            ClientMicState.COMMAND_SUCCESS -> NeonGreen
            ClientMicState.COMMAND_ERROR -> CoralRed
            ClientMicState.LISTENING_STANDBY -> if (state.isServiceRunning) NeonGreen else CyanAccent
            ClientMicState.CONNECTING -> AmberYellow
            ClientMicState.IDLE_DISCONNECTED -> if (state.isServiceRunning) CyanAccent else Color(0xFF4B5563)
        },
        animationSpec = tween(300),
        label = "activeColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020713))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("landscape_futuristic_mic_root")
    ) {
        // Atmospheric Futuristic Glow Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.22f, size.height * 0.45f),
                        radius = size.height * 0.75f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00FF9D).copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(size.width * 0.78f, size.height * 0.5f),
                        radius = size.height * 0.75f
                    )
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // TOP MINIMALIST FUTURISTIC HUD BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Capsule: MODO SATÉLITE | BUSCAR HOST (CutCornerShape)
                    Surface(
                        shape = CutCornerShape(12.dp),
                        color = Color(0xFF06182C),
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF00E5FF).copy(alpha = 0.8f),
                                    Color(0xFF0284C7).copy(alpha = 0.5f)
                                )
                            )
                        ),
                        modifier = Modifier
                            .clickable { onQuickReconnect() }
                            .testTag("status_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Pulsing radio broadcast icon ((•))
                            Canvas(modifier = Modifier.size(16.dp)) {
                                val c = Offset(size.width / 2f, size.height / 2f)
                                // Central red dot
                                drawCircle(
                                    color = Color(0xFFFF334B),
                                    radius = 3.5.dp.toPx(),
                                    center = c
                                )
                                drawCircle(
                                    color = Color(0xFFFF334B).copy(alpha = 0.35f * mediaBlinkAlpha),
                                    radius = 5.5.dp.toPx(),
                                    center = c
                                )
                                // Left arc
                                drawArc(
                                    color = Color(0xFFFF5252).copy(alpha = mediaBlinkAlpha.coerceAtLeast(0.4f)),
                                    startAngle = 135f,
                                    sweepAngle = 90f,
                                    useCenter = false,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                                // Right arc
                                drawArc(
                                    color = Color(0xFFFF5252).copy(alpha = mediaBlinkAlpha.coerceAtLeast(0.4f)),
                                    startAngle = -45f,
                                    sweepAngle = 90f,
                                    useCenter = false,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }

                            Text(
                                text = "MODO SATÉLITE",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(14.dp)
                                    .background(Color(0xFF1E3A5F))
                            )

                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = CyanAccent,
                                modifier = Modifier.size(15.dp)
                            )

                            Text(
                                text = if (state.isDiscoveringHost) "BUSCANDO..." else "BUSCAR HOST",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                letterSpacing = 0.8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Center Capsule: PALABRA CLAVE MUSICA (CutCornerShape)
                    Surface(
                        shape = CutCornerShape(12.dp),
                        color = Color(0xFF061A30),
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF0284C7).copy(alpha = 0.5f),
                                    Color(0xFF00E5FF).copy(alpha = 0.8f)
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Cyan waveform visualizer
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )

                            Column {
                                Text(
                                    text = "PALABRA CLAVE",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent.copy(alpha = 0.85f),
                                    letterSpacing = 1.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = state.wakeWord.ifBlank { "MUSICA" }.uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.2.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Right Actions: Satellite icon with green underline, Moon, Settings
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Satellite Action Button with glowing green indicator line
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = onQuickReconnect,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF061A30))
                                    .border(1.2.dp, Color(0xFF0077B6).copy(alpha = 0.7f), CircleShape)
                                    .testTag("landscape_satellite_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cast,
                                    contentDescription = "Satélite",
                                    tint = if (state.isConnected) NeonGreen else CyanAccent,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            // Green indicator underline matching the image
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(if (state.isConnected) NeonGreen else NeonGreen.copy(alpha = 0.6f))
                            )
                        }

                        // Moon / Night Mode Action Button
                        IconButton(
                            onClick = onToggleNightMode,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isNightModeActive) Color(0x33F59E0B) else Color(0xFF061A30))
                                .border(
                                    1.2.dp,
                                    if (isNightModeActive) AmberYellow.copy(alpha = 0.6f) else Color(0xFF0077B6).copy(alpha = 0.7f),
                                    CircleShape
                                )
                                .testTag("landscape_night_mode_btn")
                        ) {
                            Icon(
                                imageVector = if (isNightModeActive) Icons.Default.NightsStay else Icons.Default.Bedtime,
                                contentDescription = "Modo Nocturno",
                                tint = if (isNightModeActive) AmberYellow else Color(0xFFCBD5E1),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Settings Gear Button
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF061A30))
                                .border(1.2.dp, Color(0xFF0077B6).copy(alpha = 0.7f), CircleShape)
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ajustes",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // MAIN CONTENT: LEFT VOICE ORBS & RIGHT FUTURISTIC REMOTE
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT COLUMN: TWO ORBS + ESCUCHANDO CAPSULE + DETENER/PTT BUTTONS
                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Two Orbs Row: Mic Orb + 3D Cover Flow Orb
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Orb 1: Microphone Interactive Glowing Nexus
                            Box(
                                modifier = Modifier
                                    .size(122.dp)
                                    .clickable { onToggleContinuous() },
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                                    val baseRadius = (size.minDimension / 2f) * 0.70f
                                    val rmsBonus = state.rmsLevel * 4.0f

                                    // Multi-ring glowing neon green / cyan halo
                                    drawCircle(
                                        color = NeonGreen.copy(alpha = 0.12f),
                                        radius = (baseRadius + 16f + rmsBonus) * pulseScale,
                                        center = canvasCenter
                                    )
                                    drawCircle(
                                        color = NeonGreen.copy(alpha = 0.28f),
                                        radius = (baseRadius + 8f + (rmsBonus * 0.6f)),
                                        center = canvasCenter,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                    // Main outer green ring
                                    drawCircle(
                                        color = NeonGreen.copy(alpha = 0.95f),
                                        radius = baseRadius,
                                        center = canvasCenter,
                                        style = Stroke(width = 2.8.dp.toPx())
                                    )
                                }

                                // Central Core Glass Orb
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF03331E),
                                                    Color(0xFF021E12),
                                                    Color(0xFF010A07)
                                                )
                                            )
                                        )
                                        .border(2.dp, NeonGreen, CircleShape)
                                        .pointerInput(Unit) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                down.consume()
                                                var isHoldingPtt = false
                                                try {
                                                    onPttDown()
                                                    isHoldingPtt = true
                                                    val up = waitForUpOrCancellation()
                                                    up?.consume()
                                                    if (isHoldingPtt) onPttUp()
                                                } catch (e: Exception) {
                                                    if (isHoldingPtt) onPttUp()
                                                }
                                            }
                                        }
                                        .testTag("mic_center_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (state.isServiceRunning || state.isPushToTalk) Icons.Default.Mic else Icons.Default.MicOff,
                                        contentDescription = "Micrófono",
                                        tint = Color.White,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }

                            // Orb 2: 3D Cover Flow Metallic Blue Orb
                            val hasMediaData = state.hasMediaDisplayInfo || state.currentSongTitle.isNotBlank() || state.allMediaCards.isNotEmpty()
                            Box(
                                modifier = Modifier
                                    .size(122.dp)
                                    .clickable {
                                        ClientStateHolder.setShowFloating3dCoverFlow(true)
                                    }
                                    .testTag("landscape_blinking_3d_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                                    val baseRadius = (size.minDimension / 2f) * 0.70f

                                    // Concentric cyan/blue glowing halos
                                    drawCircle(
                                        color = Color(0xFF0077B6).copy(alpha = 0.15f * mediaBlinkAlpha),
                                        radius = (baseRadius + 16f) * mediaBlinkScale,
                                        center = canvasCenter
                                    )
                                    drawCircle(
                                        color = Color(0xFF00B4D8).copy(alpha = 0.35f * mediaBlinkAlpha),
                                        radius = baseRadius + 8f,
                                        center = canvasCenter,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                    drawCircle(
                                        color = Color(0xFF00B4D8).copy(alpha = 0.95f),
                                        radius = baseRadius,
                                        center = canvasCenter,
                                        style = Stroke(width = 2.8.dp.toPx())
                                    )
                                }

                                // Central Deep Blue Glass Orb with 3D Carousel icon
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF0A2B4E),
                                                    Color(0xFF05172C),
                                                    Color(0xFF020914)
                                                )
                                            )
                                        )
                                        .border(2.dp, Color(0xFF00B4D8), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ViewCarousel,
                                        contentDescription = "Carátulas 3D",
                                        tint = Color.White,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }

                                // Satellite Radar dot indicator in the top right
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-8).dp, y = 8.dp)
                                        .size(15.dp)
                                        .clip(CircleShape)
                                        .background(if (hasMediaData) NeonGreen else Color(0xFF00E5FF))
                                        .border(2.5.dp, Color(0xFF020713), CircleShape)
                                )
                            }
                        }

                        // 2. Middle Capsule: ESCUCHANDO: 'musica' / |||||| (CutCornerShape)
                        Surface(
                            shape = CutCornerShape(12.dp),
                            color = Color(0xFF06182C),
                            border = androidx.compose.foundation.BorderStroke(
                                1.2.dp,
                                Color(0xFF00B4D8).copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ESCUCHANDO:",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent.copy(alpha = 0.8f),
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "'${if (state.lastRecognizedText.isNotBlank()) state.lastRecognizedText.take(16) else state.wakeWord.lowercase()}'",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = "/",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Light,
                                    color = Color(0xFF1E4976)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                // Live Animated Waveform Bars
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val bar1Height = (10 + (state.rmsLevel * 14f).coerceIn(0f, 16f) * pulseScale).dp
                                    val bar2Height = (16 + (state.rmsLevel * 18f).coerceIn(0f, 14f) * mediaBlinkScale).dp
                                    val bar3Height = (22 + (state.rmsLevel * 20f).coerceIn(0f, 12f) * pulseScale).dp
                                    val bar4Height = (15 + (state.rmsLevel * 16f).coerceIn(0f, 15f) * mediaBlinkScale).dp
                                    val bar5Height = (8 + (state.rmsLevel * 12f).coerceIn(0f, 16f) * pulseScale).dp

                                    listOf(bar1Height, bar2Height, bar3Height, bar4Height, bar5Height).forEach { h ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.5.dp)
                                                .height(h)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(CyanAccent)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Bottom Action Buttons: DETENER & PTT (CutCornerShape)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // DETENER Button
                            Surface(
                                shape = CutCornerShape(10.dp),
                                color = Color(0x2200FF9D),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonGreen),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp)
                                    .clickable { onToggleContinuous() }
                                    .testTag("toggle_continuous_btn")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (state.isServiceRunning) "DETENER" else "INICIAR",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NeonGreen,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // PTT Button
                            Surface(
                                shape = CutCornerShape(10.dp),
                                color = Color(0x330C2442),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.2.dp,
                                    Color(0xFF00B4D8).copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .weight(1.0f)
                                    .height(44.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                onPttDown()
                                                tryAwaitRelease()
                                                onPttUp()
                                            }
                                        )
                                    }
                                    .testTag("ptt_button")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (state.isPushToTalk) "HABLANDO" else "PTT",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // RIGHT COLUMN: "CONTROL REMOTO DIRECTO" CYBERNETIC BOX
                    Surface(
                        shape = CutCornerShape(18.dp),
                        color = Color(0xF2030B17),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF00E5FF).copy(alpha = 0.85f),
                                    Color(0xFF0077B6).copy(alpha = 0.65f),
                                    Color(0xFF020713)
                                )
                            )
                        ),
                        modifier = Modifier
                            .weight(1.38f)
                            .fillMaxHeight()
                            .testTag("quick_controls_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 1. Header: Remote Icon + Title + Underline + Vol 10/15 Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Stylized Remote Control icon
                                Canvas(modifier = Modifier.size(16.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    // Remote body
                                    drawRoundRect(
                                        color = Color.White,
                                        topLeft = Offset(w * 0.2f, h * 0.18f),
                                        size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.78f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                                        style = Stroke(width = 1.4.dp.toPx())
                                    )
                                    // Antenna
                                    drawLine(
                                        color = Color.White,
                                        start = Offset(w * 0.4f, h * 0.18f),
                                        end = Offset(w * 0.4f, h * 0.04f),
                                        strokeWidth = 1.4.dp.toPx()
                                    )
                                    // Buttons inside remote
                                    drawCircle(color = CyanAccent, radius = 1.2.dp.toPx(), center = Offset(w * 0.4f, h * 0.4f))
                                    drawCircle(color = CyanAccent, radius = 1.2.dp.toPx(), center = Offset(w * 0.6f, h * 0.4f))
                                    drawCircle(color = CyanAccent, radius = 1.2.dp.toPx(), center = Offset(w * 0.5f, h * 0.65f))
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "CONTROL REMOTO DIRECTO",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 1.2.sp,
                                    fontFamily = FontFamily.Monospace
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                // Sleek cyan accent underline
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFF00B4D8).copy(alpha = 0.7f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Volume Badge (CutCornerShape)
                                Surface(
                                    shape = CutCornerShape(8.dp),
                                    color = Color(0x260077B6),
                                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF00B4D8))
                                ) {
                                    Text(
                                        text = if (state.isHostMuted) "MUTE" else "Vol ${state.hostVolume}/15",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CyanAccent,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // 2. Playback Transport Deck: Previous, Play, Pause, Next (Circular Glowing)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous Button
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    Color(0xFF0A223E),
                                                    Color(0xFF041222)
                                                )
                                            )
                                        )
                                        .border(1.2.dp, Color(0xFF0077B6), CircleShape)
                                        .clickable { onSendCommand("PREVIOUS", "Anterior canción") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastRewind,
                                        contentDescription = "Anterior",
                                        tint = Color(0xFF90E0EF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Play Button (Hero - Neon Green Outer Glow Ring)
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clickable { onSendCommand("PLAY", "Reanudar música") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val c = Offset(size.width / 2f, size.height / 2f)
                                        val r = size.minDimension / 2f - 2.dp.toPx()
                                        // Soft outer neon green halo
                                        drawCircle(
                                            color = NeonGreen.copy(alpha = 0.25f),
                                            radius = r + 4.dp.toPx(),
                                            center = c
                                        )
                                        // Radiant green ring
                                        drawCircle(
                                            color = NeonGreen,
                                            radius = r,
                                            center = c,
                                            style = Stroke(width = 2.5.dp.toPx())
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(
                                                        Color(0xFF063321),
                                                        Color(0xFF02160E)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = NeonGreen,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }

                                // Pause Button (Hero - Warm Orange Outer Glow Ring)
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clickable { onSendCommand("PAUSE", "Pausar música") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val c = Offset(size.width / 2f, size.height / 2f)
                                        val r = size.minDimension / 2f - 2.dp.toPx()
                                        // Soft orange halo
                                        drawCircle(
                                            color = Color(0xFFFF9100).copy(alpha = 0.25f),
                                            radius = r + 3.dp.toPx(),
                                            center = c
                                        )
                                        // Radiant orange ring
                                        drawCircle(
                                            color = Color(0xFFFF9100),
                                            radius = r,
                                            center = c,
                                            style = Stroke(width = 2.2.dp.toPx())
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(
                                                        Color(0xFF381F04),
                                                        Color(0xFF170C01)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Pause,
                                            contentDescription = "Pausa",
                                            tint = Color(0xFFFF9100),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // Next Button
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    Color(0xFF0A223E),
                                                    Color(0xFF041222)
                                                )
                                            )
                                        )
                                        .border(1.2.dp, Color(0xFF0077B6), CircleShape)
                                        .clickable { onSendCommand("NEXT", "Siguiente canción") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = "Siguiente",
                                        tint = Color(0xFF90E0EF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // 3. Volume Slider Deck with 5-dot LED Indicator
                            var landscapeVolSlider by remember(state.hostVolume) {
                                mutableFloatStateOf(state.hostVolume.toFloat().coerceIn(1f, 15f))
                            }
                            val volIcon = if (state.isHostMuted || state.hostVolume == 0) {
                                Icons.Default.VolumeMute
                            } else if (state.hostVolume <= 5) {
                                Icons.Default.VolumeDown
                            } else {
                                Icons.Default.VolumeUp
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = volIcon,
                                    contentDescription = "Volumen",
                                    tint = CyanAccent,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable { onToggleMute() }
                                )

                                Slider(
                                    value = landscapeVolSlider.coerceIn(1f, 15f),
                                    onValueChange = { newVal ->
                                        landscapeVolSlider = newVal
                                        ClientStateHolder.setHostVolume(newVal.roundToInt().coerceIn(1, 15), showHud = false)
                                    },
                                    onValueChangeFinished = {
                                        onSetVolume(landscapeVolSlider.roundToInt().coerceIn(1, 15))
                                    },
                                    valueRange = 1f..15f,
                                    steps = 13,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF90E0EF),
                                        activeTrackColor = CyanAccent,
                                        inactiveTrackColor = Color(0xFF0F263F)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(24.dp)
                                )

                                // Vertical Divider
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(Color(0xFF1E3A5F))
                                )

                                // 5-Dot LED Level Indicator
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF051222),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0E2D4F))
                                ) {
                                    val litCount = if (state.isHostMuted || state.hostVolume == 0) {
                                        0
                                    } else {
                                        ((state.hostVolume.toFloat() / 15f) * 5f).roundToInt().coerceIn(1, 5)
                                    }

                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (i in 1..5) {
                                            val isLit = i <= litCount
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isLit) NeonGreen else Color(0xFF142436)
                                                    )
                                                    .then(
                                                        if (isLit) {
                                                            Modifier.border(0.5.dp, Color(0xFF00FF9D).copy(alpha = 0.8f), CircleShape)
                                                        } else Modifier
                                                    )
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. Quick Volume Presets: Mute, -1, Vol 8, +1, Vol 15 (CutCornerShape)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mute button
                                Surface(
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(32.dp)
                                        .clickable { onToggleMute() },
                                    color = if (state.isHostMuted) CoralRed.copy(alpha = 0.25f) else Color(0xFF06182C),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (state.isHostMuted) CoralRed else Color(0xFF0077B6).copy(alpha = 0.6f)
                                    ),
                                    shape = CutCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeOff,
                                            contentDescription = null,
                                            tint = if (state.isHostMuted) CoralRed else Color(0xFFCBD5E1),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Mute",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state.isHostMuted) CoralRed else Color.White
                                        )
                                    }
                                }

                                // -1 button
                                Surface(
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(32.dp)
                                        .clickable { onAdjustVolumeDelta(-1) },
                                    color = Color(0xFF06182C),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0077B6).copy(alpha = 0.6f)),
                                    shape = CutCornerShape(6.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("-1", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                // Vol 8 preset
                                Surface(
                                    modifier = Modifier
                                        .weight(1.0f)
                                        .height(32.dp)
                                        .clickable { onSetVolume(8) },
                                    color = if (state.hostVolume == 8 && !state.isHostMuted) Color(0x3300E5FF) else Color(0xFF06182C),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (state.hostVolume == 8 && !state.isHostMuted) CyanAccent else Color(0xFF0077B6).copy(alpha = 0.6f)
                                    ),
                                    shape = CutCornerShape(6.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Vol 8",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state.hostVolume == 8 && !state.isHostMuted) CyanAccent else Color.White
                                        )
                                    }
                                }

                                // +1 button
                                Surface(
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(32.dp)
                                        .clickable { onAdjustVolumeDelta(1) },
                                    color = Color(0xFF06182C),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0077B6).copy(alpha = 0.6f)),
                                    shape = CutCornerShape(6.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("+1", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                // Vol 15 preset
                                Surface(
                                    modifier = Modifier
                                        .weight(1.0f)
                                        .height(32.dp)
                                        .clickable { onSetVolume(15) },
                                    color = if (state.hostVolume == 15 && !state.isHostMuted) Color(0x3300E5FF) else Color(0xFF06182C),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (state.hostVolume == 15 && !state.isHostMuted) CyanAccent else Color(0xFF0077B6).copy(alpha = 0.6f)
                                    ),
                                    shape = CutCornerShape(6.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Vol 15",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state.hostVolume == 15 && !state.isHostMuted) CyanAccent else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FuturisticControlButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = CyanAccent,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable { onClick() },
        color = Color(0x331C1C1C),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun FuturisticVolumePill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        color = Color(0x33181818),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (accentColor == CoralRed) CoralRed.copy(alpha = 0.6f) else DarkBorder
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun FullScreenNightClockView(
    state: com.example.voice.ClientUiState,
    settings: ClientSettings,
    onExitNightMode: () -> Unit,
    onToggleContinuous: () -> Unit,
    onSendCommand: (cmdType: String, label: String) -> Unit,
    onSetVolume: (Int) -> Unit,
    onAdjustVolumeDelta: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var currentTimeStr by remember { mutableStateOf("") }
    var currentSecStr by remember { mutableStateOf("") }
    var currentDateStr by remember { mutableStateOf("") }

    // Live clock ticker
    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val secFmt = SimpleDateFormat(":ss", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
        while (true) {
            val now = Date()
            currentTimeStr = timeFmt.format(now)
            currentSecStr = secFmt.format(now)
            currentDateStr = dateFmt.format(now).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            delay(1000L)
        }
    }

    // OLED Burn-in protector subtle offset
    var burnInOffsetX by remember { mutableStateOf(0) }
    var burnInOffsetY by remember { mutableStateOf(0) }

    LaunchedEffect(settings.nightClockBurnInProtection) {
        if (settings.nightClockBurnInProtection) {
            while (true) {
                delay(120_000L) // every 2 minutes
                burnInOffsetX = Random.nextInt(-16, 17)
                burnInOffsetY = Random.nextInt(-10, 11)
            }
        } else {
            burnInOffsetX = 0
            burnInOffsetY = 0
        }
    }

    // Controls visibility on tap
    var showControls by remember { mutableStateOf(false) }
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(6000L)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                showControls = !showControls
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("fullscreen_night_clock_view")
    ) {
        // Main Centered Digital Clock
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(burnInOffsetX, burnInOffsetY) }
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Big Digital Time Display
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentTimeStr.ifEmpty { "--:--" },
                    fontSize = if (isLandscape) 92.sp else 74.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace,
                    color = AmberYellow.copy(alpha = 0.55f),
                    letterSpacing = 2.sp
                )
                Text(
                    text = currentSecStr,
                    fontSize = if (isLandscape) 30.sp else 24.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    color = AmberYellow.copy(alpha = 0.35f),
                    modifier = Modifier.padding(bottom = if (isLandscape) 14.dp else 10.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Date text
            Text(
                text = currentDateStr,
                fontSize = if (isLandscape) 15.sp else 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8).copy(alpha = 0.45f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Voice Status HUD Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x15FFFFFF),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.isServiceRunning) NeonGreen.copy(alpha = 0.25f) else Color(0x20FFFFFF)
                ),
                modifier = Modifier.clickable { onToggleContinuous() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                when (state.micState) {
                                    ClientMicState.RECORDING_SPEECH -> AmberYellow.copy(alpha = 0.8f)
                                    ClientMicState.TRANSMITTING -> PurpleAccent.copy(alpha = 0.8f)
                                    ClientMicState.COMMAND_SUCCESS -> NeonGreen.copy(alpha = 0.8f)
                                    ClientMicState.COMMAND_ERROR -> CoralRed.copy(alpha = 0.8f)
                                    ClientMicState.LISTENING_STANDBY -> if (state.isServiceRunning) NeonGreen.copy(alpha = 0.6f) else Color(0xFF6B7280)
                                    else -> if (state.isConnected) NeonGreen.copy(alpha = 0.6f) else Color(0xFF4B5563)
                                }
                            )
                    )

                    Text(
                        text = if (state.partialText.isNotBlank()) {
                            "\"${state.partialText}\""
                        } else if (state.isServiceRunning) {
                            "Escuchando: \"${state.wakeWord}\""
                        } else {
                            "Micrófono en pausa"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFCBD5E1).copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Tap hint at bottom when controls hidden
        AnimatedVisibility(
            visible = !showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Toca la pantalla para controles o salir",
                fontSize = 10.sp,
                color = Color(0xFF64748B).copy(alpha = 0.35f)
            )
        }

        // Floating Overlay Controls when tapped
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x334B5563))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Quick Remote Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onSendCommand("PAUSE", "Pausa") },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pausar", tint = AmberYellow)
                        }

                        IconButton(
                            onClick = { onSendCommand("RESUME", "Reanudar") },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = NeonGreen)
                        }

                        IconButton(
                            onClick = { onSendCommand("NEXT", "Siguiente") },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.FastForward, contentDescription = "Siguiente", tint = CyanAccent)
                        }

                        IconButton(
                            onClick = { onAdjustVolumeDelta(-1) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.VolumeDown, contentDescription = "Vol -", tint = TextSecondary)
                        }

                        IconButton(
                            onClick = { onAdjustVolumeDelta(1) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Vol +", tint = TextSecondary)
                        }
                    }

                    // Night Volume Slider Row (1 a 15)
                    var nightSliderVal by remember(state.hostVolume) { mutableFloatStateOf(state.hostVolume.toFloat().coerceIn(1f, 15f)) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isHostMuted || state.hostVolume == 0) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Volumen",
                            tint = if (state.isHostMuted) CoralRed else CyanAccent,
                            modifier = Modifier.size(16.dp).clickable { onToggleMute() }
                        )
                        Slider(
                            value = nightSliderVal.coerceIn(1f, 15f),
                            onValueChange = { newVal ->
                                nightSliderVal = newVal
                                ClientStateHolder.setHostVolume(newVal.roundToInt().coerceIn(1, 15), showHud = false)
                            },
                            onValueChangeFinished = {
                                onSetVolume(nightSliderVal.roundToInt().coerceIn(1, 15))
                            },
                            valueRange = 1f..15f,
                            steps = 13,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanAccent,
                                activeTrackColor = CyanAccent,
                                inactiveTrackColor = Color(0x334B5563)
                            ),
                            modifier = Modifier.weight(1f).height(24.dp)
                        )
                        Text(
                            text = if (state.isHostMuted) "MUTE" else "Nivel ${state.hostVolume}/15",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isHostMuted) CoralRed else CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Action Buttons Row (Exit night mode, mic toggle, settings)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onExitNightMode,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33F59E0B), contentColor = AmberYellow),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salir del Modo Noche", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = onToggleContinuous,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (state.isServiceRunning) Color(0x2222C55E) else Color(0x22EF4444))
                            ) {
                                Icon(
                                    imageVector = if (state.isServiceRunning) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = "Micrófono",
                                    tint = if (state.isServiceRunning) NeonGreen else CoralRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22FFFFFF))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Ajustes",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenDeepSleepBlackoutView(
    state: com.example.voice.ClientUiState,
    settings: ClientSettings,
    onExitNightMode: () -> Unit,
    onToggleContinuous: () -> Unit,
    onSendCommand: (cmdType: String, label: String) -> Unit,
    onSetVolume: (Int) -> Unit,
    onAdjustVolumeDelta: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var currentTimeStr by remember { mutableStateOf("") }
    var currentDateStr by remember { mutableStateOf("") }

    // Live clock ticker
    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
        while (true) {
            val now = Date()
            currentTimeStr = timeFmt.format(now)
            currentDateStr = dateFmt.format(now).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            delay(1000L)
        }
    }

    // Wake HUD visibility state (on tap or on voice)
    var isAwakeHUDVisible by remember { mutableStateOf(false) }
    var wakeDismissTimerJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun showAwakeHudTemporarily(durationMs: Long = 6500L) {
        isAwakeHUDVisible = true
        wakeDismissTimerJob?.cancel()
        wakeDismissTimerJob = coroutineScope.launch {
            delay(durationMs)
            isAwakeHUDVisible = false
        }
    }

    // Dynamic hardware screen brightness:
    // When awake HUD is shown, temporarily raise brightness slightly (0.08f or user dim level)
    // When sleeping (blackout), keep hardware brightness at 0.0f to completely extinguish LCD backlight
    LaunchedEffect(isAwakeHUDVisible) {
        try {
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = if (isAwakeHUDVisible) {
                    settings.nightDimLevel.coerceAtLeast(0.08f)
                } else {
                    0.0f
                }
            }
        } catch (_: Exception) {}
    }

    // Auto wake on voice recognition if configured
    LaunchedEffect(state.micState, state.partialText, state.lastRecognizedText) {
        if (settings.nightVoiceWake) {
            val isVoiceActive = state.micState == ClientMicState.RECORDING_SPEECH ||
                    state.micState == ClientMicState.TRANSMITTING ||
                    state.micState == ClientMicState.COMMAND_SUCCESS ||
                    state.micState == ClientMicState.GEMINI_PROCESSING ||
                    state.partialText.isNotBlank()
            if (isVoiceActive) {
                showAwakeHudTemporarily(8000L)
            }
        }
    }

    // Vibrator helper
    fun triggerHaptic(durationMs: Long = 40) {
        if (!settings.hapticFeedback) return
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .testTag("fullscreen_deep_sleep_blackout_view")
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (settings.nightTapToWake) {
                    triggerHaptic(35)
                    showAwakeHudTemporarily()
                }
            }
    ) {
        // Standby Blackout: Pure solid black (0% light output)
        // If HUD is visible (Tap-to-Wake or Voice Wake), show the minimal wake overlay
        AnimatedVisibility(
            visible = isAwakeHUDVisible,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(400)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF2000000))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                // Top status bar with Mode badge & Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x22F59E0B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberYellow.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = AmberYellow,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                "Modo Sueño • Pantalla Apagada",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                triggerHaptic(25)
                                onOpenSettings()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }

                        // Exit sleep mode (Turn on display completely)
                        Button(
                            onClick = {
                                triggerHaptic(40)
                                onExitNightMode()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = ZenOledBlack
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Encender Pantalla", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Center Clock & Voice Status
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = currentTimeStr.ifEmpty { "--:--" },
                        fontSize = if (isLandscape) 76.sp else 60.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        color = AmberYellow.copy(alpha = 0.85f),
                        letterSpacing = 2.sp
                    )

                    Text(
                        text = currentDateStr,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8).copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Voice recognition state capsule
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0x221E293B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (state.isServiceRunning) NeonGreen.copy(alpha = 0.4f) else Color(0x30FFFFFF)
                        ),
                        modifier = Modifier.clickable {
                            triggerHaptic(40)
                            onToggleContinuous()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (state.micState) {
                                            ClientMicState.RECORDING_SPEECH -> AmberYellow
                                            ClientMicState.TRANSMITTING -> PurpleAccent
                                            ClientMicState.COMMAND_SUCCESS -> NeonGreen
                                            ClientMicState.COMMAND_ERROR -> CoralRed
                                            ClientMicState.GEMINI_PROCESSING -> CyanAccent
                                            else -> if (state.isServiceRunning) NeonGreen else Color(0xFF6B7280)
                                        }
                                    )
                            )

                            Text(
                                text = if (state.partialText.isNotBlank()) {
                                    "\"${state.partialText}\""
                                } else if (state.lastRecognizedText.isNotBlank() && state.micState != ClientMicState.LISTENING_STANDBY) {
                                    state.lastRecognizedText
                                } else if (state.isServiceRunning) {
                                    "Escuchando en 2do plano: \"${settings.wakeWord}\""
                                } else {
                                    "Micrófono en pausa (Toca para activar)"
                                },
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Bottom floating quick media controls + Back to deep sleep button
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xF0111827)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x404B5563)),
                        modifier = Modifier.fillMaxWidth(if (isLandscape) 0.65f else 0.98f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                triggerHaptic(30)
                                onSendCommand("PAUSE", "Pausa")
                            }) {
                                Icon(Icons.Default.Pause, contentDescription = "Pausar", tint = AmberYellow)
                            }
                            IconButton(onClick = {
                                triggerHaptic(30)
                                onSendCommand("RESUME", "Reanudar")
                            }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = NeonGreen)
                            }
                            IconButton(onClick = {
                                triggerHaptic(30)
                                onSendCommand("NEXT", "Siguiente")
                            }) {
                                Icon(Icons.Default.FastForward, contentDescription = "Siguiente", tint = CyanAccent)
                            }
                            IconButton(onClick = {
                                triggerHaptic(25)
                                onAdjustVolumeDelta(-1)
                            }) {
                                Icon(Icons.Default.VolumeDown, contentDescription = "Vol -", tint = TextSecondary)
                            }
                            IconButton(onClick = {
                                triggerHaptic(25)
                                onAdjustVolumeDelta(1)
                            }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Vol +", tint = TextSecondary)
                            }
                            IconButton(onClick = {
                                triggerHaptic(35)
                                onToggleMute()
                            }) {
                                Icon(
                                    Icons.Default.VolumeMute,
                                    contentDescription = "Mute",
                                    tint = if (state.isHostMuted) CoralRed else TextMuted
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            triggerHaptic(25)
                            isAwakeHUDVisible = false
                        }
                    ) {
                        Icon(Icons.Default.Bedtime, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Volver a Apagar Pantalla", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeMeshSyncStatusCard(
    state: com.example.voice.ClientUiState,
    settings: ClientSettings,
    onLiberateRam: () -> Unit,
    onSendMeshSync: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("node_mesh_sync_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (state.peerNodes.isNotEmpty()) CyanAccent.copy(alpha = 0.4f) else DarkBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x2206B6D4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Sintonía de Nodos y Música",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (state.peerNodes.isEmpty()) "1 satélite en línea (Este nodo)" else "${state.peerNodes.size + 1} nodos en sintonía simultánea",
                            fontSize = 11.sp,
                            color = if (state.peerNodes.isNotEmpty()) NeonGreen else TextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (state.playbackState.uppercase()) {
                        "REPRODUCIENDO" -> Color(0x2222C55E)
                        "EN PAUSA" -> Color(0x22F59E0B)
                        else -> Color(0x221E293B)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (state.playbackState.uppercase()) {
                            "REPRODUCIENDO" -> NeonGreen.copy(alpha = 0.5f)
                            "EN PAUSA" -> AmberYellow.copy(alpha = 0.5f)
                            else -> DarkBorder
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (state.playbackState.uppercase()) {
                                "REPRODUCIENDO" -> Icons.Default.MusicNote
                                "EN PAUSA" -> Icons.Default.Pause
                                else -> Icons.Default.GraphicEq
                            },
                            contentDescription = null,
                            tint = when (state.playbackState.uppercase()) {
                                "REPRODUCIENDO" -> NeonGreen
                                "EN PAUSA" -> AmberYellow
                                else -> TextSecondary
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = state.playbackState.ifBlank { "LISTO" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (state.playbackState.uppercase()) {
                                "REPRODUCIENDO" -> NeonGreen
                                "EN PAUSA" -> AmberYellow
                                else -> TextSecondary
                            }
                        )
                    }
                }
            }

            // Synced Track & Host Info Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x330F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ESTADO SINCRONIZADO DEL HOST",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanAccent,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (state.isHostMuted) "MUTE" else "Volumen: Nivel ${state.hostVolume}/15",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isHostMuted) CoralRed else NeonGreen
                        )
                    }

                    Text(
                        text = state.currentSongTitle.ifBlank { "Ninguna pista activa transmitida" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (state.currentSongTitle.isNotBlank()) TextPrimary else TextMuted,
                        maxLines = 1
                    )

                    if (state.lastCommandSender.isNotBlank()) {
                        Text(
                            text = "Última acción por: ${state.lastCommandSender}",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }

                    // Botón para desplegar galería flotante 3D
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x3306B6D4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { ClientStateHolder.setShowFloating3dCoverFlow(true) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewCarousel,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VER CARÁTULAS Y LISTAS EN 3D",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Sintonía y Arbitraje de Activación
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0x1A06B6D4),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Sintonía Mesh: Solo 1 nodo envía la orden; el resto cancela la captura al activarse",
                        fontSize = 10.5.sp,
                        color = TextSecondary
                    )
                }
            }

            // Peer Devices (Nodes) Chips if any exist
            if (state.peerNodes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Teléfonos y Nodos Conectados al Host:",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.peerNodes.forEach { peer ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x3310B981),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(NeonGreen)
                                    )
                                    Text(
                                        text = peer.name.ifBlank { "Nodo ${peer.id.take(4)}" },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action row: Liberar RAM + Sintonizar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onLiberateRam,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("liberate_ram_card_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Liberar RAM", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSendMeshSync,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mesh_sync_broadcast_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sincronizar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RamOptimizationBannerOverlay(
    state: com.example.voice.ClientUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-dismiss banner after 4 seconds
    LaunchedEffect(state.showOptimizationBanner) {
        if (state.showOptimizationBanner) {
            delay(4000L)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = state.showOptimizationBanner,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 64.dp, start = 16.dp, end = 16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFA0F172A),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanAccent),
            shadowElevation = 8.dp,
            modifier = Modifier.testTag("ram_optimization_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x3306B6D4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Liberador de RAM & Procesos",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                    Text(
                        text = state.optimizationMessage.ifBlank { "Memoria liberada y procesos de audio optimizados" },
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
