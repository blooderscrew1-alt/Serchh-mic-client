package com.example.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ai.GeminiVoiceService
import com.example.ai.TtsSpeaker
import com.example.data.ClientSettings
import com.example.network.MeshNodeCoordinator
import com.example.network.RemoteClientHolder
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RemoteMicForegroundService : Service() {

    private val TAG = "RemoteMicService"
    private val NOTIFICATION_ID = 5050
    private val CHANNEL_ID = "serch_mic_client_channel"

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var audioManager: AudioManager
    private lateinit var settings: ClientSettings

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isListeningLoopActive = false
    private var isCurrentlyRecognizing = false
    private var isManualPushToTalk = false
    private var isPhysicalPttHolding = false
    private var accumulatedPttText = ""
    private var wasContinuousActiveBeforeManual = false
    private var isProcessingCommand = false
    private var pttFallbackRunnable: Runnable? = null

    @Volatile
    private var isCaptureCancelledByMesh = false
    @Volatile
    private var meshWinnerNodeName = ""
    @Volatile
    private var lastObservedRms = 0f

    // ─────────────────────────────────────────────────────────────────────────
    // DIAGNÓSTICO Y AUTORREPARACIÓN DEL MOTOR DE VOZ DEL SISTEMA
    // Muchos fabricantes (MIUI/HyperOS, EMUI, ColorOS...) traen por defecto un
    // servicio de reconocimiento propio que no devuelve texto con los extras
    // que usamos, mientras que la onda del micrófono SÍ se mueve (onRmsChanged).
    // Estas variables permiten detectar ese escenario, reportarlo en el registro
    // en pantalla y degradar el intento automáticamente.
    // ─────────────────────────────────────────────────────────────────────────
    /** Componente del motor de reconocimiento elegido (null = predeterminado del sistema). */
    private var recognizerComponent: ComponentName? = null

    /** Etiqueta legible del motor en uso para los registros. */
    private var recognizerEngineLabel: String = "predeterminado del sistema"

    /** 0 = intento completo, 1 = intento mínimo, 2 = intento mínimo + preferir offline. */
    private var recognizerProfile = 0

    /** Fallos consecutivos reales del motor (no cuentan los cierres por silencio). */
    private var recognizerFailureStreak = 0

    /** Total de fallos reales del motor desde el arranque de la escucha. */
    private var recognizerEngineFailures = 0

    /** Total de sesiones cerradas por silencio/ruido (funcionamiento normal). */
    private var recognizerSilentSessions = 0

    /** Nº de errores onError recibidos (para el registro). */
    private var recognizerErrorCount = 0

    /** Nº de eventos con texto devueltos por el motor. */
    private var recognizerTextEventCount = 0

    /** Marca de tiempo del último texto devuelto por el motor. */
    @Volatile
    private var lastRecognizerTextAt = 0L

    /** Marca de tiempo del último sonido relevante visto por el medidor del reconocedor. */
    @Volatile
    private var lastSpeechEnergyAt = 0L

    /** Momento en el que se pidió escucha al motor actual. */
    private var recognizerListeningSince = 0L

    /** Evita repetir el aviso de "motor sin respuesta" en bucle. */
    private var engineUnresponsiveWarned = false

    /** Indica si el chequeo periódico de salud del motor está programado. */
    private var engineHealthScheduled = false

    /** Si es true, se intenta primero el motor de Google (el más fiable en MIUI/HyperOS). */
    private var preferPreferredRecognizer = true

    private val preferredRecognizerPackages = listOf(
        "com.google.android.googlequicksearchbox",
        "com.google.android.tts",
        "com.google.android.apps.googleassistant"
    )

    private val engineHealthRunnable = object : Runnable {
        override fun run() {
            if (isListeningLoopActive && !isManualPushToTalk) {
                val now = System.currentTimeMillis()
                val reference = maxOf(lastRecognizerTextAt, recognizerListeningSince)
                val noTextForMs = if (reference > 0L) now - reference else 0L
                val hasRecentVoice = now - lastSpeechEnergyAt < 6000L

                if (!engineUnresponsiveWarned && hasRecentVoice && noTextForMs > 20000L) {
                    engineUnresponsiveWarned = true
                    val seconds = noTextForMs / 1000L
                    val diagnosis =
                        "⚠️ DIAGNÓSTICO DE VOZ: el micrófono capta sonido (la onda se mueve) pero el motor de voz " +
                        "'$recognizerEngineLabel' lleva ${seconds}s sin devolver texto (fallos=$recognizerEngineFailures, " +
                        "errores=$recognizerErrorCount, silencios=$recognizerSilentSessions, Android ${Build.VERSION.RELEASE}). " +
                        "En MIUI/HyperOS revisa: 1) Ajustes → Aplicaciones → Administrar aplicaciones → ⋮ → Aplicaciones predeterminadas " +
                        "→ Asistencia y entrada de voz → Entrada de voz = Google; 2) que 'Speech Services by Google' y la app 'Google' " +
                        "estén habilitadas y actualizadas, con el paquete de idioma español descargado; " +
                        "3) Ahorro de batería SIN restricciones + inicio automático + datos en segundo plano permitidos " +
                        "para esta app y para Google."
                    Log.w(TAG, diagnosis)
                    ClientStateHolder.addLog(diagnosis, isError = true)
                    ClientStateHolder.setMicState(
                        ClientMicState.LISTENING_STANDBY,
                        "⚠️ El motor de voz no responde en este teléfono (ver registro)"
                    )
                }
                mainHandler.postDelayed(this, 5000L)
            } else {
                mainHandler.postDelayed(this, 5000L)
            }
        }
    }

    private fun cancelActiveCaptureForMesh(winnerName: String, reason: String) {
        serviceScope.launch(Dispatchers.Main) {
            Log.i(TAG, "cancelActiveCaptureForMesh: Yielding capture to '$winnerName' ($reason)")
            isCaptureCancelledByMesh = true
            meshWinnerNodeName = winnerName
            isProcessingCommand = false

            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}

            cancelWakeWordWindow(revertToStandby = true)
            ClientStateHolder.setPartialText("")
            ClientStateHolder.setRms(0f)
            ClientStateHolder.hideWakeWordPopup()
            ClientStateHolder.setMicState(
                ClientMicState.LISTENING_STANDBY,
                "🛰️ Sintonía Mesh: Cediendo a $winnerName"
            )
            ClientStateHolder.addLog("[Sintonía Mesh] Captura cancelada en este nodo (atendiendo '$winnerName')")
            scheduleNextRecognition(400L)
        }
    }

    // Wake word active window (configurable seconds to receive command after wake word)
    private var isWakeWordActiveWindow = false
    private var wakeWordWindowExpiryTime = 0L

    private val wakeWindowTickerRunnable: Runnable = object : Runnable {
        override fun run() {
            if (isWakeWordActiveWindow && isListeningLoopActive) {
                val now = System.currentTimeMillis()
                if (now < wakeWordWindowExpiryTime) {
                    val remainingSecs = (((wakeWordWindowExpiryTime - now) + 999L) / 1000L).toInt().coerceAtLeast(1)
                    ClientStateHolder.setWakeWindowState(true, remainingSecs)
                    ClientStateHolder.setMicState(
                        ClientMicState.RECORDING_SPEECH,
                        "✨ Te escucho... Di tu orden (ej: 'Pon Queen', 'Pausa') [${remainingSecs}s]"
                    )
                    mainHandler.postDelayed(this, 1000L)
                } else {
                    cancelWakeWordWindow(revertToStandby = true)
                }
            }
        }
    }

    private val wakeWindowTimeoutRunnable = Runnable {
        if (isWakeWordActiveWindow) {
            cancelWakeWordWindow(revertToStandby = true)
        }
    }

    private fun cancelWakeWordWindow(revertToStandby: Boolean = true) {
        isWakeWordActiveWindow = false
        wakeWordWindowExpiryTime = 0L
        mainHandler.removeCallbacks(wakeWindowTimeoutRunnable)
        mainHandler.removeCallbacks(wakeWindowTickerRunnable)
        abandonTransientAudioFocus()
        ClientStateHolder.setWakeWindowState(false, 0)
        if (ClientStateHolder.state.value.wakeWordPopupPhase != WakeWordPopupPhase.SUCCESS) {
            ClientStateHolder.hideWakeWordPopup()
        }
        notifySignalState(false)
        ClientStateHolder.setRms(0f)
        ClientStateHolder.setPartialText("")

        if (revertToStandby && isListeningLoopActive && !isManualPushToTalk) {
            val defaultMsg = if (settings.directCommandsEnabled) {
                "🎤 Escuchando... ('${settings.wakeWord}' o comandos directos)"
            } else {
                "🎤 Escuchando: '${settings.wakeWord}'"
            }
            ClientStateHolder.setMicState(
                ClientMicState.LISTENING_STANDBY,
                defaultMsg
            )
        }
    }

    // Safety Watchdog to prevent SpeechRecognizer from hanging indefinitely
    private val WATCHDOG_TIMEOUT_MS = 20000L
    private val watchdogRunnable = Runnable {
        if (isListeningLoopActive && !isManualPushToTalk && !isWakeWordActiveWindow) {
            Log.w(TAG, "Watchdog triggered: restarting stalled SpeechRecognizer session")
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            isCurrentlyRecognizing = false
            startSpeechRecognitionSafely()
        }
    }

    // Periodic Auto-Refresh / Keep-Alive cycle to prevent memory leaks and speech recognition lags over hours of use
    private val autoRefreshRunnable = object : Runnable {
        override fun run() {
            if (isListeningLoopActive && !isManualPushToTalk && !isWakeWordActiveWindow) {
                performEngineAutoRefresh("Auto-refresco programado (${settings.autoRefreshMinutes}m)", isAuto = true)
            }
            scheduleNextAutoRefresh()
        }
    }

    private fun scheduleNextAutoRefresh() {
        mainHandler.removeCallbacks(autoRefreshRunnable)
        if (settings.autoRefreshEnabled && settings.autoRefreshMinutes > 0 && isListeningLoopActive) {
            val intervalMs = settings.autoRefreshMinutes * 60 * 1000L
            Log.d(TAG, "Scheduling next engine auto-refresh in ${settings.autoRefreshMinutes} minutes ($intervalMs ms)")
            mainHandler.postDelayed(autoRefreshRunnable, intervalMs)
        }
    }

    private fun performEngineAutoRefresh(reason: String, isAuto: Boolean = false) {
        Log.i(TAG, "Performing speech engine and RAM refresh: $reason")
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null

        // Execute deep RAM and process optimization
        ClientStateHolder.performRamAndProcessOptimization(this, isAuto = isAuto)

        if (isListeningLoopActive && !isManualPushToTalk) {
            initSpeechRecognizer()
            scheduleNextRecognition(250L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RemoteMicForegroundService onCreate")
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        settings = ClientSettings(this)

        configureAudioRouting()
        applyBeepSuppression(true)
        createNotificationChannel()
        TtsSpeaker.init(this)

        // Auto connect network client if enabled
        if (settings.autoConnectOnLaunch && settings.hostIp.isNotBlank()) {
            RemoteClientHolder.connect(this)
        }

        MeshNodeCoordinator.start(this)
        MeshNodeCoordinator.registerYieldListener { winnerName, reason ->
            cancelActiveCaptureForMesh(winnerName, reason)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action=$action")

        when (action) {
            ACTION_STOP_SERVICE -> {
                stopListeningService()
                return START_NOT_STICKY
            }
            ACTION_START_MANUAL_PTT -> {
                startForegroundWithMicrophoneType()
                handleStartManualPtt()
                return START_STICKY
            }
            ACTION_STOP_MANUAL_PTT -> {
                handleStopManualPtt()
                return START_STICKY
            }
            ACTION_CANCEL_MANUAL_PTT -> {
                handleCancelManualPtt()
                return START_STICKY
            }
            ACTION_FORCE_REFRESH_ENGINE -> {
                performEngineAutoRefresh("Refresco manual de optimización")
                return START_STICKY
            }
        }

        startForegroundWithMicrophoneType()
        isListeningLoopActive = true
        isManualPushToTalk = false
        isWakeWordActiveWindow = false
        applyBeepSuppression(true)
        resetRecognizerDiagnostics()

        val standbyMsg = if (settings.directCommandsEnabled) {
            "🎤 Escuchando... ('${settings.wakeWord}' o comandos directos)"
        } else {
            "🎤 Escuchando: '${settings.wakeWord}'"
        }

        ClientStateHolder.updateState {
            it.copy(
                isServiceRunning = true,
                isPushToTalk = false,
                micState = ClientMicState.LISTENING_STANDBY,
                statusMessage = standbyMsg,
                wakeWord = settings.wakeWord,
                hostIp = settings.hostIp,
                hostPort = settings.hostPort,
                deviceAlias = settings.deviceAlias
            )
        }

        scheduleNextAutoRefresh()

        mainHandler.post {
            initSpeechRecognizer()
            startSpeechRecognitionSafely()
        }

        return START_STICKY
    }

    private fun handleStartManualPtt() {
        if (!isManualPushToTalk) {
            wasContinuousActiveBeforeManual = isListeningLoopActive
        }
        isManualPushToTalk = true
        isPhysicalPttHolding = true
        accumulatedPttText = ""
        isWakeWordActiveWindow = false
        mainHandler.removeCallbacks(wakeWindowTimeoutRunnable)
        mainHandler.removeCallbacks(wakeWindowTickerRunnable)
        resetWatchdog()
        requestTransientAudioFocus()

        ClientStateHolder.updateState {
            it.copy(
                isPushToTalk = true,
                micState = ClientMicState.RECORDING_SPEECH,
                statusMessage = "🎙️ Mantén pulsado y habla...",
                partialText = "",
                wakeWord = settings.wakeWord
            )
        }

        notifySignalState(true, "Entrada manual PTT")
        startSpeechRecognitionSafely()
    }

    private fun handleStopManualPtt() {
        isPhysicalPttHolding = false
        ClientStateHolder.updateState {
            it.copy(
                statusMessage = "⚡ Procesando orden de voz..."
            )
        }

        // Give Android SpeechRecognizer up to 650ms to complete acoustic decoding and emit onResults
        pttFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
        val fallback = Runnable {
            if (isManualPushToTalk && !isPhysicalPttHolding && !isProcessingCommand) {
                if (accumulatedPttText.isNotBlank()) {
                    Log.d(TAG, "Processing accumulated PTT text upon release fallback: '$accumulatedPttText'")
                    handleRecognizedMatches(listOf(accumulatedPttText))
                } else {
                    finishManualPttNoMatch()
                }
            }
        }
        pttFallbackRunnable = fallback
        mainHandler.postDelayed(fallback, 650L)

        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping manual PTT: ${e.message}")
            if (accumulatedPttText.isNotBlank() && !isProcessingCommand) {
                handleRecognizedMatches(listOf(accumulatedPttText))
            } else if (!isProcessingCommand) {
                finishManualPttNoMatch()
            }
        }
    }

    private fun finishManualPttNoMatch() {
        pttFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
        isManualPushToTalk = false
        isPhysicalPttHolding = false
        accumulatedPttText = ""
        notifySignalState(false)
        ClientStateHolder.updateState {
            it.copy(
                isPushToTalk = false,
                rmsLevel = 0f,
                partialText = "",
                statusMessage = "Sin voz detectada"
            )
        }

        if (wasContinuousActiveBeforeManual) {
            isListeningLoopActive = true
            val defMsg = if (settings.directCommandsEnabled) {
                "🎤 Escuchando... ('${settings.wakeWord}' o comandos directos)"
            } else {
                "🎤 Escuchando: '${settings.wakeWord}'"
            }
            ClientStateHolder.setMicState(
                ClientMicState.LISTENING_STANDBY,
                defMsg
            )
            scheduleNextRecognition(250L)
        } else {
            ClientStateHolder.setMicState(ClientMicState.IDLE_DISCONNECTED, "⏸️ Escucha en pausa")
        }
    }

    private fun handleCancelManualPtt() {
        pttFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
        isManualPushToTalk = false
        isPhysicalPttHolding = false
        accumulatedPttText = ""
        isProcessingCommand = false
        resetWatchdog()
        notifySignalState(false)
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}

        ClientStateHolder.updateState {
            it.copy(
                isPushToTalk = false,
                rmsLevel = 0f,
                partialText = ""
            )
        }

        if (wasContinuousActiveBeforeManual) {
            isListeningLoopActive = true
            ClientStateHolder.setMicState(
                ClientMicState.LISTENING_STANDBY,
                "Escuchando: '${settings.wakeWord}'"
            )
            scheduleNextRecognition(150L)
        } else {
            ClientStateHolder.setMicState(ClientMicState.IDLE_DISCONNECTED, "Entrada cancelada")
        }
    }

    private fun configureAudioRouting() {
        try {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
            if (audioManager.isBluetoothScoOn) {
                audioManager.isBluetoothScoOn = false
                audioManager.stopBluetoothSco()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio routing config error: ${e.message}")
        }
    }

    private fun applyBeepSuppression(mute: Boolean) {
        if (!settings.muteRecognizerBeeps) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val mode = if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE
                audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, mode, 0)
                audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, mode, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Beep suppression error: ${e.message}")
        }
    }

    private fun startForegroundWithMicrophoneType() {
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } catch (e: Exception) {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, RemoteMicForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val wakeWord = settings.wakeWord
        val hostInfo = if (settings.hostIp.isNotBlank()) "Host: ${settings.hostIp}" else "Host no configurado"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("serch mic client activo")
            .setContentText("Palabra: '$wakeWord' • $hostInfo")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingOpenApp)
            .addAction(android.R.drawable.ic_media_pause, "Detener", pendingStop)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Micrófono Remoto Cliente",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene la captura de micrófono en segundo plano"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Enumera los servicios de reconocimiento de voz visibles para la app.
     * El manifiesto ya declara <queries> para "android.speech.RecognitionService",
     * por lo que en Android 11+ estos paquetes son visibles.
     */
    private fun listRecognitionServices(): List<ComponentName> {
        return try {
            val pm = packageManager
            val intent = Intent("android.speech.RecognitionService")
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentServices(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentServices(intent, PackageManager.MATCH_ALL)
            }
            resolved.mapNotNull { info ->
                val si = info.serviceInfo ?: return@mapNotNull null
                ComponentName(si.packageName, si.name)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudieron enumerar los servicios de reconocimiento: ${e.message}")
            emptyList()
        }
    }

    private fun pickPreferredRecognizerComponent(services: List<ComponentName>): ComponentName? {
        for (pkg in preferredRecognizerPackages) {
            services.firstOrNull { it.packageName == pkg }?.let { return it }
        }
        return null
    }

    private fun recognizerProfileName(profile: Int): String = when (profile) {
        0 -> "completo (idioma principal + idiomas adicionales)"
        1 -> "mínimo (solo idioma principal)"
        else -> "mínimo + preferir reconocimiento offline"
    }

    private fun recognizerErrorName(code: Int): String = when (code) {
        1 -> "ERROR_NETWORK_TIMEOUT"
        2 -> "ERROR_NETWORK (el motor no tiene red)"
        3 -> "ERROR_AUDIO (fallo de captura del reconocedor)"
        4 -> "ERROR_SERVER (el servicio de voz rechazó la petición)"
        5 -> "ERROR_CLIENT (sesión inválida o permiso)"
        6 -> "ERROR_SPEECH_TIMEOUT (fin por silencio)"
        7 -> "ERROR_NO_MATCH (fin por silencio/ruido)"
        8 -> "ERROR_RECOGNIZER_BUSY"
        9 -> "ERROR_INSUFFICIENT_PERMISSIONS"
        12 -> "ERROR_LANGUAGE_NOT_SUPPORTED"
        13 -> "ERROR_LANGUAGE_UNAVAILABLE"
        else -> "ERROR_CODE_$code"
    }

    /**
     * Construye la intención de reconocimiento según el perfil adaptativo actual.
     */
    private fun buildRecognizerIntent(): Intent {
        val langMode = settings.speechLanguageMode
        val primaryLanguage = when (langMode) {
            "en" -> "en-US"
            else -> "es-ES"
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 6)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, applicationContext.packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLanguage)
        }

        if (recognizerProfile == 0) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, primaryLanguage)
            when (langMode) {
                "en" -> intent.putExtra(
                    "android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES",
                    arrayOf("en-US", "en-GB")
                )
                "es" -> intent.putExtra(
                    "android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES",
                    arrayOf("es-419", "es-ES", "es-US", "es-MX")
                )
                else -> intent.putExtra(
                    "android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES",
                    arrayOf("es-419", "es-US", "es-MX", "en-US")
                )
            }
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
        }

        if (recognizerProfile >= 2) {
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        return intent
    }

    /**
     * Si el motor acumula fallos seguidos, se simplifica el intento y, como último
     * recurso, se vuelve al motor predeterminado del sistema.
     */
    private fun escalateRecognizerProfile(reason: String) {
        if (recognizerProfile < 2) {
            recognizerProfile++
            recognizerFailureStreak = 0
            val message = "🔧 Motor de voz: reintentando en modo '${recognizerProfileName(recognizerProfile)}' por $reason"
            Log.w(TAG, message)
            ClientStateHolder.addLog(message)
            return
        }

        if (recognizerComponent != null) {
            recognizerComponent = null
            recognizerProfile = 0
            recognizerFailureStreak = 0
            preferPreferredRecognizer = false
            val message = "🔧 Motor de voz: volviendo al motor predeterminado del sistema por $reason"
            Log.w(TAG, message)
            ClientStateHolder.addLog(message)
            initSpeechRecognizer()
        }
    }

    /**
     * Registra que el motor SÍ está devolviendo texto (funciona) y lo refleja en el log en pantalla.
     */
    private fun registerRecognizerText(text: String, kind: String) {
        lastRecognizerTextAt = System.currentTimeMillis()
        recognizerTextEventCount++
        engineUnresponsiveWarned = false
        if (recognizerTextEventCount <= 5 || recognizerTextEventCount % 50 == 0) {
            ClientStateHolder.addLog("🗣️ Texto del motor ($kind): \"$text\"")
        }
    }

    private fun resetRecognizerDiagnostics() {
        recognizerFailureStreak = 0
        recognizerEngineFailures = 0
        recognizerSilentSessions = 0
        recognizerErrorCount = 0
        recognizerTextEventCount = 0
        lastRecognizerTextAt = 0L
        engineUnresponsiveWarned = false
        preferPreferredRecognizer = true
    }

    private fun scheduleEngineHealthCheck() {
        if (engineHealthScheduled) return
        engineHealthScheduled = true
        mainHandler.postDelayed(engineHealthRunnable, 5000L)
    }

    private fun initSpeechRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null

            if (!SpeechRecognizer.isRecognitionAvailable(applicationContext)) {
                ClientStateHolder.setMicState(
                    ClientMicState.COMMAND_ERROR,
                    "Reconocedor de voz no disponible en el sistema"
                )
                val missingMsg =
                    "❌ Este dispositivo no expone ningún servicio de reconocimiento de voz " +
                    "(Android ${Build.VERSION.RELEASE}, ${Build.MANUFACTURER} ${Build.MODEL}). " +
                    "Instala o habilita 'Speech Services by Google' y selecciónalo en " +
                    "Ajustes → Sistema → Idiomas e introducción → Entrada de voz."
                Log.e(TAG, missingMsg)
                ClientStateHolder.addLog(missingMsg, isError = true)
                return
            }

            val services = listRecognitionServices()
            val chosen = if (preferPreferredRecognizer) pickPreferredRecognizerComponent(services) else null
            val availableList = if (services.isEmpty()) {
                "no enumerables"
            } else {
                services.joinToString(", ") { it.packageName }
            }

            var created: SpeechRecognizer? = null
            if (chosen != null) {
                try {
                    created = SpeechRecognizer.createSpeechRecognizer(applicationContext, chosen)
                    recognizerComponent = chosen
                    recognizerEngineLabel = chosen.packageName
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo usar el motor ${chosen.packageName}: ${e.message}")
                    created = null
                }
            }

            if (created == null) {
                recognizerComponent = null
                recognizerEngineLabel = services.firstOrNull()?.packageName ?: "predeterminado del sistema"
                created = SpeechRecognizer.createSpeechRecognizer(applicationContext)
            }

            speechRecognizer = created.apply { setRecognitionListener(createRecognitionListener()) }
            scheduleEngineHealthCheck()

            val engineMsg = "🎙️ Motor de voz en uso: $recognizerEngineLabel (disponibles: $availableList)"
            Log.i(TAG, engineMsg)
            ClientStateHolder.addLog(engineMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create SpeechRecognizer", e)
        }
    }

    private fun requestTransientAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { focusChange ->
                        Log.d(TAG, "Audio focus change: $focusChange")
                    }
                    .build()
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
            Log.d(TAG, "Requested Transient Audio Focus (Media Ducking Active)")
        } catch (e: Exception) {
            Log.w(TAG, "Error requesting audio focus: ${e.message}")
        }
    }

    private fun abandonTransientAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).build()
                audioManager.abandonAudioFocusRequest(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            Log.d(TAG, "Abandoned Audio Focus (Media Volume Restored)")
        } catch (e: Exception) {
            Log.w(TAG, "Error abandoning audio focus: ${e.message}")
        }
    }

    private fun startSpeechRecognitionSafely() {
        if (!isListeningLoopActive && !isManualPushToTalk) return

        Log.i(TAG, "VOICE_STATE: WAKE_LISTENING")
        Log.i(TAG, "MIC_OWNER: SpeechRecognizer")

        try {
            if (speechRecognizer == null) {
                initSpeechRecognizer()
            }

            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}

            // Intención adaptativa: si el motor falla de forma persistente en este
            // dispositivo, se simplifica automáticamente (ver escalateRecognizerProfile).
            val intent = buildRecognizerIntent()
            recognizerListeningSince = System.currentTimeMillis()

            resetWatchdog()
            isCurrentlyRecognizing = true
            // FIX PTT: Android 12+ a veces IGNORA startListening() inmediato tras cancel()
            // (carrera interna del motor): el usuario mantiene presionado y nunca escucha.
            // Dar ~80ms para que el motor libere la sesion anterior antes de arrancar.
            mainHandler.postDelayed({
                if (!isListeningLoopActive && !isManualPushToTalk) {
                    isCurrentlyRecognizing = false
                    return@postDelayed
                }
                try {
                    speechRecognizer?.startListening(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting speech recognition (delayed)", e)
                    isCurrentlyRecognizing = false
                    scheduleNextRecognition(400L)
                }
            }, 80L)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            isCurrentlyRecognizing = false
            scheduleNextRecognition(400L)
        }
    }

    private fun resetWatchdog() {
        mainHandler.removeCallbacks(watchdogRunnable)
        if (isListeningLoopActive && !isManualPushToTalk) {
            mainHandler.postDelayed(watchdogRunnable, WATCHDOG_TIMEOUT_MS)
        }
    }

    private fun scheduleNextRecognition(delayMillis: Long = 100L) {
        if (!isListeningLoopActive || isManualPushToTalk) return
        mainHandler.removeCallbacks(watchdogRunnable)
        mainHandler.postDelayed({
            if (isListeningLoopActive && !isManualPushToTalk) {
                startSpeechRecognitionSafely()
            }
        }, delayMillis)
    }

    private fun notifySignalState(isReceiving: Boolean, details: String = "") {
        val client = RemoteClientHolder.getClient(this)
        client.sendSignalState(
            isReceiving = isReceiving,
            details = details,
            hostIp = settings.hostIp,
            port = settings.hostPort
        )
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isCurrentlyRecognizing = true
                resetWatchdog()

                if (isWakeWordActiveWindow && System.currentTimeMillis() < wakeWordWindowExpiryTime) {
                    val remaining = (((wakeWordWindowExpiryTime - System.currentTimeMillis()) + 999L) / 1000L).toInt().coerceAtLeast(1)
                    ClientStateHolder.setWakeWindowState(true, remaining)
                    ClientStateHolder.setMicState(
                        ClientMicState.RECORDING_SPEECH,
                        "✨ Te escucho... Di tu orden (ej: 'Pon Queen', 'Pausa') [${remaining}s]"
                    )
                    return
                }

                val stateMessage = when {
                    isManualPushToTalk -> "🎤 Habla ahora..."
                    settings.wakeWordTriggersHostMicDirectly -> "⚡ Escuchando '${settings.wakeWord}' → Activar mic del Host"
                    settings.directCommandsEnabled -> "🎤 Escuchando... ('${settings.wakeWord}' o comandos directos)"
                    else -> "🎤 Escuchando: '${settings.wakeWord}'"
                }

                ClientStateHolder.setMicState(
                    if (isManualPushToTalk) ClientMicState.RECORDING_SPEECH else ClientMicState.LISTENING_STANDBY,
                    stateMessage
                )
            }

            override fun onBeginningOfSpeech() {
                resetWatchdog()
                if (isWakeWordActiveWindow) {
                    val extensionMs = maxOf(settings.wakeWindowSeconds * 1000L, 6000L)
                    wakeWordWindowExpiryTime = maxOf(wakeWordWindowExpiryTime, System.currentTimeMillis() + extensionMs)
                    mainHandler.removeCallbacks(wakeWindowTimeoutRunnable)
                    mainHandler.postDelayed(wakeWindowTimeoutRunnable, wakeWordWindowExpiryTime - System.currentTimeMillis())
                }
                if (isManualPushToTalk || isWakeWordActiveWindow) {
                    notifySignalState(true, if (isManualPushToTalk) "Entrada manual" else "Escuchando orden")
                    ClientStateHolder.setMicState(
                        ClientMicState.RECORDING_SPEECH,
                        "Capturando voz..."
                    )
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (rmsdB > 0.5f) {
                    resetWatchdog()
                }
                // Señal de que el micrófono está captando sonido de verdad (la onda se mueve)
                if (rmsdB > 1.0f) {
                    lastSpeechEnergyAt = System.currentTimeMillis()
                }
                val normalized = ((rmsdB + 2f) / 1.2f).coerceIn(0f, 10f)
                lastObservedRms = normalized
                ClientStateHolder.setRms(normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                if (isManualPushToTalk || isWakeWordActiveWindow) {
                    ClientStateHolder.setMicState(ClientMicState.RECORDING_SPEECH, "Procesando...")
                }
            }

            override fun onError(error: Int) {
                isCurrentlyRecognizing = false
                resetWatchdog()

                // If user is STILL holding the manual PTT button, DO NOT ABORT! Seamlessly restart recognition so user's speech isn't cut off
                if (isManualPushToTalk && isPhysicalPttHolding) {
                    Log.d(TAG, "PTT recognizer pause/error ($error) while holding: seamlessly resuming recognition")
                    mainHandler.postDelayed({
                        if (isManualPushToTalk && isPhysicalPttHolding) {
                            startSpeechRecognitionSafely()
                        }
                    }, 150L)
                    return
                }

                // If user released the manual PTT button and recognizer finished with error:
                if (isManualPushToTalk && !isPhysicalPttHolding) {
                    if (accumulatedPttText.isNotBlank()) {
                        handleRecognizedMatches(listOf(accumulatedPttText))
                    } else {
                        finishManualPttNoMatch()
                    }
                    return
                }

                // If in active wake word window and time remains, restart seamlessly so user is not cut off and mic stays OPEN
                if (isWakeWordActiveWindow && System.currentTimeMillis() < wakeWordWindowExpiryTime) {
                    ClientStateHolder.updateState {
                        it.copy(rmsLevel = 0f, partialText = "")
                    }
                    if (isListeningLoopActive) {
                        scheduleNextRecognition(40L)
                    }
                    return
                }

                if (!isWakeWordActiveWindow && !isManualPushToTalk) {
                    notifySignalState(false)
                }

                ClientStateHolder.updateState {
                    it.copy(
                        rmsLevel = 0f,
                        partialText = ""
                    )
                }

                // ── Diagnóstico del motor de voz ───────────────────────────────
                // Los cierres por silencio (NO_MATCH / SPEECH_TIMEOUT) son normales
                // en escucha continua; solo los errores reales de motor indican
                // que el reconocedor del dispositivo no está funcionando.
                val isSilenceEnd = error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                recognizerErrorCount++

                if (isSilenceEnd) {
                    recognizerSilentSessions++
                    if (recognizerSilentSessions <= 3) {
                        ClientStateHolder.addLog(
                            "🔇 Sesión cerrada por silencio (${recognizerErrorName(error)}) — el motor responde con normalidad."
                        )
                    }
                } else {
                    recognizerEngineFailures++
                    recognizerFailureStreak++
                    if (recognizerEngineFailures <= 10 || recognizerEngineFailures % 50 == 0) {
                        ClientStateHolder.addLog(
                            "⚠️ Fallo del motor de voz: ${recognizerErrorName(error)} " +
                            "(motor=$recognizerEngineLabel, modo=${recognizerProfileName(recognizerProfile)}, " +
                            "fallos seguidos=$recognizerFailureStreak)"
                        )
                    }
                    if (recognizerFailureStreak >= 5) {
                        val reason = recognizerErrorName(error)
                        recognizerFailureStreak = 0
                        escalateRecognizerProfile(reason)
                    }
                }

                if (isListeningLoopActive) {
                    // Backoff progresivo: no martillear el motor con reinicios de 80 ms
                    // cuando está devolviendo errores reales.
                    val retryDelay = if (isSilenceEnd) 80L else (200L * recognizerFailureStreak.coerceAtLeast(1)).coerceAtMost(2500L)
                    if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                        initSpeechRecognizer()
                        scheduleNextRecognition(maxOf(300L, retryDelay))
                    } else {
                        scheduleNextRecognition(retryDelay)
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                isCurrentlyRecognizing = false
                resetWatchdog()
                pttFallbackRunnable?.let { mainHandler.removeCallbacks(it) }

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                val text = matches.firstOrNull()?.trim() ?: ""
                if (text.isNotBlank()) {
                    registerRecognizerText(text, "final")
                    accumulatedPttText = text
                }

                if (isManualPushToTalk) {
                    if (isPhysicalPttHolding) {
                        // User is still holding the button: keep recognition active to let them complete speech
                        mainHandler.postDelayed({
                            if (isManualPushToTalk && isPhysicalPttHolding) {
                                startSpeechRecognitionSafely()
                            }
                        }, 150L)
                        return
                    } else {
                        // User has released the button: process results now
                        val candidateList = if (matches.isNotEmpty()) matches else if (accumulatedPttText.isNotBlank()) listOf(accumulatedPttText) else emptyList()
                        handleRecognizedMatches(candidateList)
                        return
                    }
                }

                handleRecognizedMatches(matches)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                resetWatchdog()
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                if (text.isNotBlank()) {
                    registerRecognizerText(text, "parcial")
                    if (isManualPushToTalk) {
                        accumulatedPttText = text
                    }

                    val isWakeDetected = VoiceCommandEngine.isWakeWordDetected(text, settings.wakeWord)

                    // MODO INSTANTÁNEO DE ACTIVACIÓN DE MICRÓFONO DEL HOST:
                    // Si la opción está habilitada, al detectar la palabra clave enviamos de inmediato "activa tu micrófono"
                    // al Host sin grabar, transcribir ni procesar peticiones locales.
                    if (settings.wakeWordTriggersHostMicDirectly && !isManualPushToTalk) {
                        if (isWakeDetected) {
                            triggerInstantHostMicOnWakeWord(settings.wakeWord, text)
                        }
                        return
                    }

                    // 1. WAKE-WORD WINDOW TRIGGER:
                    // If wake word is heard in partial speech, duck/mute multimedia audio, open listening popup and keep window open
                    if (isWakeDetected && !isWakeWordActiveWindow) {
                        ClientStateHolder.addLog(
                            "✅ Palabra clave '${settings.wakeWord}' detectada en: \"$text\"",
                            isIncoming = true
                        )
                        isWakeWordActiveWindow = true
                        requestTransientAudioFocus()
                        val windowMs = settings.wakeWindowSeconds * 1000L
                        wakeWordWindowExpiryTime = System.currentTimeMillis() + windowMs
                        mainHandler.removeCallbacks(wakeWindowTimeoutRunnable)
                        mainHandler.removeCallbacks(wakeWindowTickerRunnable)
                        mainHandler.postDelayed(wakeWindowTimeoutRunnable, windowMs)
                        mainHandler.post(wakeWindowTickerRunnable)

                        ClientStateHolder.setWakeWindowState(true, settings.wakeWindowSeconds)
                        ClientStateHolder.showWakeWordPopup(isListening = true)
                        notifySignalState(true, "Palabra '${settings.wakeWord}' detectada")

                        if (settings.geminiAiVoiceEnhanceEnabled) {
                            GeminiVoiceService.prewarm(this@RemoteMicForegroundService)
                        }
                    }

                    if (isWakeWordActiveWindow) {
                        val extensionMs = maxOf(settings.wakeWindowSeconds * 1000L, 6000L)
                        wakeWordWindowExpiryTime = maxOf(wakeWordWindowExpiryTime, System.currentTimeMillis() + extensionMs)
                        mainHandler.removeCallbacks(wakeWindowTimeoutRunnable)
                        mainHandler.postDelayed(wakeWindowTimeoutRunnable, wakeWordWindowExpiryTime - System.currentTimeMillis())
                    }

                    // 2. ONLY UPDATE UI AND TEXT PREVIEW - DO NOT EXECUTE COMMANDS FROM PARTIAL SPEECH!
                    if (isManualPushToTalk || isWakeWordActiveWindow || isWakeDetected || settings.directCommandsEnabled) {
                        if (isManualPushToTalk && !ClientStateHolder.state.value.showWakeWordPopup) {
                            ClientStateHolder.showWakeWordPopup(isListening = true)
                        }
                        ClientStateHolder.setPartialText(text)
                        notifySignalState(true, text)
                        ClientStateHolder.setMicState(
                            ClientMicState.RECORDING_SPEECH,
                            if (isManualPushToTalk) "🎙️ \"$text\"" else "Capturando: \"$text\"..."
                        )
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private var lastInstantHostMicTriggerTime = 0L

    /**
     * Dispara al instante y sin retardos el comando "activa tu micrófono" hacia el Host
     * en cuanto se detecta la palabra clave, omitiendo cualquier grabación, transcripción o petición posterior.
     */
    private fun triggerInstantHostMicOnWakeWord(spokenWakeWord: String, rawText: String) {
        val now = System.currentTimeMillis()
        // 🛡️ Anti-falso-positivo 1: letras de canciones / frases largas que INICIAN con la
        // palabra clave no son comandos ("musica es mi companion de viaje..."). Los comandos
        // reales tras la palabra clave son cortos (<= 8 palabras).
        val wordCount = rawText.trim().split(Regex("\s+")).size
        if (wordCount > 8) {
            Log.i(TAG, "ANTI_FP: Texto de ${wordCount} palabras tras la palabra clave: probable letra/frase, ignorado.")
            return
        }
        // 🛡️ Anti-falso-positivo 2: periodo refractario de 8s (antes 1.8s) para que la
        // propia musica con la palabra en la letra no reactive el micro del host en bucle.
        if (now - lastInstantHostMicTriggerTime < 8000L) {
            return
        }
        lastInstantHostMicTriggerTime = now

        val audioEnergy = lastObservedRms.coerceAtLeast(1.0f)
        val decision = MeshNodeCoordinator.claimWakeWord(audioEnergy, rawText)
        if (decision is MeshNodeCoordinator.ArbitrationDecision.Yield) {
            Log.i(TAG, "MESH_SYNC: Yielding instant host mic wake to ${decision.winnerName} (${decision.reason})")
            ClientStateHolder.addLog("[Sintonía Mesh] Palabra detectada pero cediendo a '${decision.winnerName}'.")
            return
        }

        // 1. Envío inmediato (0ms latencia por WebSocket / TCP NoDelay) del comando "activa tu micrófono" al Host
        val client = RemoteClientHolder.getClient(this)
        client.sendInstantHostMicActivation(
            hostIp = settings.hostIp,
            port = settings.hostPort,
            wakeWord = spokenWakeWord
        )

        // 2. Pitido corto de confirmación en hilo secundario sin bloquear
        serviceScope.launch(Dispatchers.Default) {
            playWakeConfirmationTone()
        }

        // 3. Actualizar interfaz inmediatamente sin abrir ventana de grabación ni transcribir
        isWakeWordActiveWindow = false
        wakeWordWindowExpiryTime = 0L
        mainHandler.removeCallbacks(wakeWindowTimeoutRunnable)
        mainHandler.removeCallbacks(wakeWindowTickerRunnable)
        ClientStateHolder.setPartialText("")
        ClientStateHolder.setWakeWindowState(false, 0)
        ClientStateHolder.notifyCommandSuccess("⚡ Host: Activa tu micrófono")
        ClientStateHolder.updateState {
            it.copy(
                lastRecognizedText = spokenWakeWord,
                micState = ClientMicState.COMMAND_SUCCESS,
                statusMessage = "⚡ Comando instantáneo enviado: Activa tu micrófono"
            )
        }

        // 4. Cancelar sesión actual del reconocedor y volver a escucha de palabra clave
        // para que el teléfono no procese ni transcriba lo que el usuario le diga al micrófono del Host
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        isCurrentlyRecognizing = false
        if (isListeningLoopActive) {
            scheduleNextRecognition(350L)
        }
    }

    private fun handleRecognizedMatches(matches: List<String>) {
        if (isProcessingCommand) {
            Log.d(TAG, "Command already processing, ignoring duplicate match event")
            return
        }

        if (matches.isEmpty()) {
            if (isWakeWordActiveWindow && System.currentTimeMillis() < wakeWordWindowExpiryTime) {
                // Keep listening in wake window
                scheduleNextRecognition(40L)
                return
            }
            scheduleNextRecognition(60L)
            return
        }

        val wakeWord = settings.wakeWord

        // Si el ajuste opcional de activar micrófono del Host al instante está activo:
        // No procesamos, no transcribimos ni enviamos peticiones de búsqueda; solo enviamos "activa tu micrófono" al detectar la palabra clave.
        if (settings.wakeWordTriggersHostMicDirectly && !isManualPushToTalk) {
            val wakeDetected = matches.any { candidate ->
                VoiceCommandEngine.isWakeWordDetected(candidate, wakeWord)
            }
            if (wakeDetected) {
                triggerInstantHostMicOnWakeWord(wakeWord, matches.first())
            } else {
                ClientStateHolder.setPartialText("")
                scheduleNextRecognition(60L)
            }
            return
        }
        val (pickedOriginal, pickedCorrected) = if (settings.bilingualPhoneticFixEnabled) {
            BilingualSpeechCorrector.pickBestMatch(matches)
        } else {
            Pair(matches.first(), matches.first())
        }

        val primaryCandidate = if (pickedCorrected.isNotBlank()) pickedCorrected else matches.first()

        // 1. Precise candidate evaluation with bilingual phonetic fallback
        val finalResult: ParseResult
        val bestRawText: String
        if (isManualPushToTalk || isWakeWordActiveWindow) {
            var foundResult: ParseResult? = null
            var matchedCandidate = primaryCandidate

            // Check scored primary first, then all matches and their phonetic corrections
            val candidatesToTry = listOf(primaryCandidate, pickedOriginal).plus(matches).distinct()
            for (candidate in candidatesToTry) {
                val res = VoiceCommandEngine.parseDirect(candidate)
                if (res.command !is VoiceCommand.None) {
                    foundResult = res
                    matchedCandidate = candidate
                    break
                }
            }
            finalResult = foundResult ?: VoiceCommandEngine.parseDirect(primaryCandidate)
            bestRawText = matchedCandidate
        } else {
            // Standby: Evaluate primary candidate, fallback to bilingually corrected candidates if wake word is spoken
            var res = VoiceCommandEngine.parse(primaryCandidate, wakeWord, settings.directCommandsEnabled)
            var chosenText = primaryCandidate
            if (res.command is VoiceCommand.None && matches.size > 1) {
                for (candidate in matches) {
                    val candidateRes = VoiceCommandEngine.parse(candidate, wakeWord, settings.directCommandsEnabled)
                    if (candidateRes.command !is VoiceCommand.None) {
                        res = candidateRes
                        chosenText = candidate
                        break
                    }
                }
            }
            finalResult = res
            bestRawText = chosenText
        }

        val command = finalResult.command

        // Wake Word Only (e.g. user said "Música") -> Transition to AudioRecord & Whisper with full pre-roll & post-roll
        if (command is VoiceCommand.WakeWordOnly || (finalResult.matchedMasterWord && (command is VoiceCommand.None || command is VoiceCommand.WakeWordOnly))) {
            val audioEnergy = lastObservedRms.coerceAtLeast(1.0f)
            val decision = MeshNodeCoordinator.claimWakeWord(audioEnergy, bestRawText)
            if (decision is MeshNodeCoordinator.ArbitrationDecision.Yield) {
                Log.i(TAG, "MESH_SYNC: Yielding wake word to ${decision.winnerName} (${decision.reason})")
                ClientStateHolder.addLog("[Sintonía Mesh] Palabra detectada pero cediendo a '${decision.winnerName}'. Captura cancelada en este nodo.")
                ClientStateHolder.setMicState(ClientMicState.LISTENING_STANDBY, "🛰️ Sintonía Mesh: Cediendo a ${decision.winnerName}")
                cancelWakeWordWindow(revertToStandby = true)
                scheduleNextRecognition(350L)
                return
            }
            transitionToCommandCaptureAndWhisper(settings.wakeWord, bestRawText)
            return
        }

        val isAuthorized = isManualPushToTalk || isWakeWordActiveWindow || settings.directCommandsEnabled || finalResult.matchedMasterWord

        if (finalResult.matchedMasterWord && !isManualPushToTalk) {
            val audioEnergy = lastObservedRms.coerceAtLeast(1.0f)
            val decision = MeshNodeCoordinator.claimWakeWord(audioEnergy, bestRawText)
            if (decision is MeshNodeCoordinator.ArbitrationDecision.Yield) {
                Log.i(TAG, "MESH_SYNC: Yielding direct command to ${decision.winnerName} (${decision.reason})")
                ClientStateHolder.addLog("[Sintonía Mesh] Comando detectado pero cediendo a '${decision.winnerName}'. Cancelando envío.")
                ClientStateHolder.setMicState(ClientMicState.LISTENING_STANDBY, "🛰️ Sintonía Mesh: Cediendo a ${decision.winnerName}")
                cancelWakeWordWindow(revertToStandby = true)
                scheduleNextRecognition(350L)
                return
            }
        }

        // Check if we should process via Gemini AI for natural language / song search enhancement
        val shouldUseGemini = isAuthorized &&
                !VoiceCommandEngine.isIncompleteOrNoise(bestRawText) &&
                settings.geminiAiVoiceEnhanceEnabled &&
                GeminiVoiceService.isConfigured(this@RemoteMicForegroundService) &&
                bestRawText.trim().length >= 3 &&
                (command is VoiceCommand.None || !finalResult.isClearLocalCommand)

        if (shouldUseGemini) {
            isProcessingCommand = true
            val isFromManualPtt = isManualPushToTalk
            if (isManualPushToTalk) {
                isManualPushToTalk = false
                isPhysicalPttHolding = false
                accumulatedPttText = ""
                ClientStateHolder.updateState { it.copy(isPushToTalk = false) }
            }

            serviceScope.launch {
                ClientStateHolder.setMicState(
                    ClientMicState.GEMINI_PROCESSING,
                    "✨ Gemini AI optimizando reconocimiento de voz..."
                )
                
                // Fast execution with 2.2-second timeout to ensure prompt user response
                val aiResult = try {
                    kotlinx.coroutines.withTimeoutOrNull(2200L) {
                        GeminiVoiceService.enhanceVoiceCommand(
                            spokenText = bestRawText,
                            configuredWakeWord = settings.wakeWord,
                            modelName = settings.geminiModelName,
                            context = this@RemoteMicForegroundService
                        )
                    }
                } catch (e: Exception) {
                    null
                }

                if (aiResult != null && aiResult.command !is VoiceCommand.None) {
                    cancelWakeWordWindow(revertToStandby = false)
                    ClientStateHolder.setPartialText("")
                    ClientStateHolder.setGeminiAiFeedback("✨ Gemini: ${aiResult.intent} -> ${aiResult.songQuery.ifBlank { aiResult.spokenFeedback }}")
                    ClientStateHolder.addLog("[Gemini AI] Voz optimizada: ${aiResult.command.toDisplayAction()}")

                    if (settings.geminiVoiceTtsEnabled && aiResult.spokenFeedback.isNotBlank()) {
                        TtsSpeaker.speak(aiResult.spokenFeedback)
                    }

                    ClientStateHolder.updateState {
                        it.copy(
                            lastRecognizedText = bestRawText,
                            micState = ClientMicState.TRANSMITTING,
                            statusMessage = "✨ Gemini: ${aiResult.command.toDisplayAction()}"
                        )
                    }

                    dispatchCommandToHost(aiResult.command, bestRawText, isFromManualPtt)
                    return@launch
                }

                // If Gemini didn't find a new command but local parser had SearchAndPlay, fallback to local command
                if (command is VoiceCommand.SearchAndPlay) {
                    cancelWakeWordWindow(revertToStandby = false)
                    ClientStateHolder.setPartialText("")
                    ClientStateHolder.updateState {
                        it.copy(
                            lastRecognizedText = bestRawText,
                            micState = ClientMicState.TRANSMITTING,
                            statusMessage = "Enviando: ${command.toDisplayAction()}"
                        )
                    }
                    dispatchCommandToHost(command, bestRawText, isFromManualPtt)
                    return@launch
                }

                isProcessingCommand = false

                // If no command recognized and still in wake window, keep listening
                if (isWakeWordActiveWindow && System.currentTimeMillis() < wakeWordWindowExpiryTime) {
                    ClientStateHolder.setPartialText("")
                    scheduleNextRecognition(40L)
                    return@launch
                }

                ClientStateHolder.setPartialText("")
                notifySignalState(false)
                cancelWakeWordWindow(revertToStandby = true)
                scheduleNextRecognition(60L)
            }
            return
        }

        // If no command recognized
        if (command is VoiceCommand.None) {
            // IF STILL IN WAKE WINDOW: KEEP OPEN! DO NOT TOGGLE OR ABORT!
            if (isWakeWordActiveWindow && System.currentTimeMillis() < wakeWordWindowExpiryTime) {
                ClientStateHolder.setPartialText("")
                scheduleNextRecognition(40L)
                return
            }

            if (isManualPushToTalk) {
                isManualPushToTalk = false
                isPhysicalPttHolding = false
                accumulatedPttText = ""
                ClientStateHolder.updateState { it.copy(isPushToTalk = false, partialText = "") }
                cancelWakeWordWindow(revertToStandby = true)
                if (wasContinuousActiveBeforeManual) {
                    isListeningLoopActive = true
                    scheduleNextRecognition(150L)
                } else {
                    isListeningLoopActive = false
                }
                return
            }

            // Normal standby listening: casual speech/noise without wake word is safely discarded
            ClientStateHolder.setPartialText("")
            notifySignalState(false)
            cancelWakeWordWindow(revertToStandby = true)
            scheduleNextRecognition(60L)
            return
        }

        // Direct local command recognized (e.g. Pause, Volume, Next Track)! Close wake window and send to host
        isProcessingCommand = true
        cancelWakeWordWindow(revertToStandby = false)

        val isFromManualPtt = isManualPushToTalk
        if (isManualPushToTalk) {
            isManualPushToTalk = false
            isPhysicalPttHolding = false
            accumulatedPttText = ""
            ClientStateHolder.updateState { it.copy(isPushToTalk = false) }
        }

        ClientStateHolder.setPartialText("")
        ClientStateHolder.updateState {
            it.copy(
                lastRecognizedText = bestRawText,
                micState = ClientMicState.TRANSMITTING,
                statusMessage = "Enviando: ${command.toDisplayAction()}"
            )
        }

        serviceScope.launch {
            dispatchCommandToHost(command, bestRawText, isFromManualPtt)
        }
    }

    private suspend fun dispatchCommandToHost(command: VoiceCommand, rawText: String, isFromManualPtt: Boolean = false) {
        try {
            val songQuery = when (command) {
                is VoiceCommand.SearchAndPlay -> command.songQuery
                else -> ""
            }
            val cmdType = when (command) {
                is VoiceCommand.SearchAndPlay -> "SEARCH_PLAY"
                is VoiceCommand.Resume -> "PLAY"
                is VoiceCommand.Pause -> "PAUSE"
                is VoiceCommand.Stop -> "STOP"
                is VoiceCommand.NextTrack -> "NEXT"
                is VoiceCommand.PreviousTrack -> "PREVIOUS"
                is VoiceCommand.SetVolume -> if (command.level > 15) "VOLUME_SET_PCT_${command.level}" else "VOLUME_SET_LEVEL_${command.level}"
                is VoiceCommand.VolumeUp -> "VOLUME_UP"
                is VoiceCommand.VolumeDown -> "VOLUME_DOWN"
                is VoiceCommand.VolumeMedium -> "VOLUME_MEDIUM"
                is VoiceCommand.VolumeLow -> "VOLUME_LOW"
                is VoiceCommand.VolumeHigh -> "VOLUME_HIGH"
                is VoiceCommand.VolumeMax -> "VOLUME_MAX"
                is VoiceCommand.Mute -> "MUTE"
                is VoiceCommand.RepeatTrack -> "REPEAT"
                else -> "SEARCH_PLAY"
            }

            when (command) {
                is VoiceCommand.SetVolume -> {
                    val level = if (command.level > 15) ((command.level / 100.0) * 15.0).roundToInt().coerceIn(1, 15) else command.level.coerceIn(1, 15)
                    ClientStateHolder.setHostVolume(level, showHud = true)
                }
                is VoiceCommand.VolumeUp -> ClientStateHolder.adjustHostVolume(1, showHud = true)
                is VoiceCommand.VolumeDown -> ClientStateHolder.adjustHostVolume(-1, showHud = true)
                is VoiceCommand.VolumeMax -> ClientStateHolder.setHostVolume(15, showHud = true)
                is VoiceCommand.VolumeMedium -> ClientStateHolder.setHostVolume(8, showHud = true)
                is VoiceCommand.VolumeLow -> ClientStateHolder.setHostVolume(3, showHud = true)
                is VoiceCommand.VolumeHigh -> ClientStateHolder.setHostVolume(12, showHud = true)
                is VoiceCommand.Mute -> ClientStateHolder.setHostMuted(!ClientStateHolder.state.value.isHostMuted, showHud = true)
                else -> {}
            }

            val summaryAction = command.toDisplayAction().ifBlank { rawText }
            ClientStateHolder.notifyCommandSending(summaryAction)

            val client = RemoteClientHolder.getClient(this)
            val success = client.sendVoiceCommand(
                commandType = cmdType,
                songQuery = songQuery,
                rawSpokenText = rawText,
                hostIp = settings.hostIp,
                port = settings.hostPort
            )

            val msg = client.lastAck.value ?: if (success) "Orden confirmada por Host" else "Fallo al enviar a ${settings.hostIp}"
            if (success) {
                ClientStateHolder.notifyCommandSuccess(summaryAction)
            } else {
                ClientStateHolder.setMicState(ClientMicState.COMMAND_ERROR, msg)
            }
        } finally {
            isProcessingCommand = false
        }

        if (isFromManualPtt && !wasContinuousActiveBeforeManual) {
            isListeningLoopActive = false
        } else if (isListeningLoopActive || wasContinuousActiveBeforeManual) {
            isListeningLoopActive = true
            scheduleNextRecognition(350L)
        }
    }

    private var toneGenerator: ToneGenerator? = null

    private fun playWakeConfirmationTone() {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Exception) {
            Log.w(TAG, "Error playing tone: ${e.message}")
        }
    }

    private fun activateSpeechRecognizerWakeWindow() {
        isWakeWordActiveWindow = true
        val windowMs = settings.wakeWindowSeconds * 1000L
        wakeWordWindowExpiryTime = System.currentTimeMillis() + windowMs

        mainHandler.removeCallbacks(wakeWindowTimeoutRunnable)
        mainHandler.removeCallbacks(wakeWindowTickerRunnable)
        mainHandler.postDelayed(wakeWindowTimeoutRunnable, windowMs)
        mainHandler.post(wakeWindowTickerRunnable)

        playWakeConfirmationTone()
        requestTransientAudioFocus()

        if (settings.geminiAiVoiceEnhanceEnabled) {
            GeminiVoiceService.prewarm(this@RemoteMicForegroundService)
        }

        notifySignalState(true, "Palabra '${settings.wakeWord}' detectada")
        ClientStateHolder.setPartialText("")
        ClientStateHolder.setWakeWindowState(true, settings.wakeWindowSeconds)
        ClientStateHolder.showWakeWordPopup(isListening = true)
        ClientStateHolder.setMicState(
            ClientMicState.RECORDING_SPEECH,
            "✨ Te escucho... Di tu orden (ej: 'Pon Queen', 'Pausa') [${settings.wakeWindowSeconds}s]"
        )

        scheduleNextRecognition(40L)
    }

    private fun transitionToCommandCaptureAndWhisper(spokenWakeWord: String, initialText: String = "") {
        if (isProcessingCommand) return
        isProcessingCommand = true
        isCaptureCancelledByMesh = false
        meshWinnerNodeName = ""

        val wakeWordDetectedTs = System.currentTimeMillis()
        Log.i(TAG, "WAKE_WORD_DETECTED at $wakeWordDetectedTs: $spokenWakeWord")
        Log.i(TAG, "SPEECH_RECOGNIZER_RELEASE_STARTED at ${System.currentTimeMillis()}")

        // 1. Release SpeechRecognizer completely
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        isCurrentlyRecognizing = false
        val speechRecognizerReleasedTs = System.currentTimeMillis()
        Log.i(TAG, "SPEECH_RECOGNIZER_RELEASED at $speechRecognizerReleasedTs")
        Log.i(TAG, "MIC_OWNER: AudioRecord")

        // Auditory beep feedback & UI popup update (non-blocking tone to keep handoff instant)
        serviceScope.launch(Dispatchers.Default) {
            playWakeConfirmationTone()
        }
        requestTransientAudioFocus()
        ClientStateHolder.showWakeWordPopup(isListening = true)
        ClientStateHolder.setWakeWindowState(true, settings.wakeWindowSeconds)
        ClientStateHolder.setMicState(
            ClientMicState.RECORDING_SPEECH,
            "✨ Te escucho... Di tu orden (ej: 'Pon Queen', 'Pausa')"
        )

        if (settings.geminiAiVoiceEnhanceEnabled) {
            GeminiVoiceService.prewarm(this@RemoteMicForegroundService)
        }

        serviceScope.launch {
            try {
                val pcmAudioData: ShortArray? = withContext(Dispatchers.IO) {
                    recordCommandAudioWithAudioRecord(speechRecognizerReleasedTs)
                }

                if (isCaptureCancelledByMesh) {
                    Log.i(TAG, "MESH_SYNC: Capture cancelled for winner '$meshWinnerNodeName'. Skipping Whisper & dispatch.")
                    isProcessingCommand = false
                    MeshNodeCoordinator.onCaptureCancelled()
                    return@launch
                }

                if (pcmAudioData == null || pcmAudioData.isEmpty()) {
                    Log.i(TAG, "COMMAND_AUDIO_NULL: No speech captured. Reverting to standby.")
                    isProcessingCommand = false
                    MeshNodeCoordinator.onCaptureCancelled()
                    cancelWakeWordWindow(revertToStandby = true)
                    scheduleNextRecognition(200L)
                    return@launch
                }

                Log.i(TAG, "VOICE_STATE: WHISPER_TRANSCRIPTION")
                Log.i(TAG, "WHISPER_TRANSCRIPTION_STARTED")
                Log.i(TAG, "WHISPER_STARTED")

                val rawTranscription: String = withContext(Dispatchers.IO) {
                    WhisperNativeBridge.autoLoadModelIfAvailable(this@RemoteMicForegroundService)
                    if (pcmAudioData != null && pcmAudioData.isNotEmpty() && WhisperNativeBridge.isReady()) {
                        val floatSamples = FloatArray(pcmAudioData.size) { i -> pcmAudioData[i] / 32768.0f }
                        val decoded = WhisperNativeBridge.decode(
                            samples = floatSamples,
                            numThreads = 2,
                            language = if (settings.speechLanguageMode == "en") "en" else "es",
                            translateToEnglish = false
                        )
                        decoded
                    } else {
                        if (!WhisperNativeBridge.isReady()) {
                            Log.w(TAG, "WHISPER_MODEL_ERROR: Whisper model or native runtime not loaded. Using speech recognizer fallback.")
                        }
                        initialText.replace(Regex("(?i)^.*?" + Regex.escape(spokenWakeWord)), "").trim()
                    }
                }

                // 1. RAW_TRANSCRIPTION -> STRIPPED & NORMALIZED_TEXT
                val strippedTranscription = MusicCommandParser.stripWakeWord(rawTranscription)
                val effectiveRaw = if (strippedTranscription.isNotBlank()) strippedTranscription else initialText
                val normalizedText = BilingualSpeechCorrector.normalize(effectiveRaw)

                // 2. NORMALIZED_TEXT -> CORRECTED_TEXT
                val correctionResult = if (settings.bilingualPhoneticFixEnabled) {
                    BilingualSpeechCorrector.correctWithConfidence(effectiveRaw)
                } else {
                    BilingualSpeechCorrector.SpeechCorrectionResult(
                        rawText = effectiveRaw,
                        normalizedText = normalizedText,
                        correctedText = effectiveRaw,
                        applied = false,
                        reason = "Bilingual phonetic correction disabled in settings",
                        confidence = 1.0f
                    )
                }
                val correctedText = correctionResult.correctedText

                // 3. CORRECTED_TEXT -> PARSED_INTENT (local parser first)
                val parsed = VoiceCommandEngine.parseDirect(correctedText)
                val musicParsed = MusicCommandParser.parse(correctedText)
                val parsedIntent = parsed.intent
                val parsedArtist = parsed.artist ?: musicParsed.artist
                val parsedTrack = parsed.track ?: musicParsed.track

                // 4. Determine execution path: Local execution vs Optional Gemini Fallback
                var geminiUsed = false
                var geminiReason = ""
                var finalCommand: VoiceCommand = VoiceCommand.None

                val isClearLocal = parsed.isClearLocalCommand && parsed.command !is VoiceCommand.None
                val isGeminiAvailable = settings.geminiAiVoiceEnhanceEnabled && GeminiVoiceService.isConfigured(this@RemoteMicForegroundService)

                if (isClearLocal) {
                    // Local intent resolution takes precedence (e.g. pausa, siguiente, pon Queen, reproduce Michael Jackson)
                    finalCommand = parsed.command
                    geminiUsed = false
                    geminiReason = if (parsed.command is VoiceCommand.SearchAndPlay) "Clear local search query" else "Local media control command"
                } else if (isGeminiAvailable && (!VoiceCommandEngine.isIncompleteOrNoise(correctedText) && correctedText.length >= 2)) {
                    // Gemini used ONLY for ambiguous, complex, or conversational phrases
                    geminiReason = if (parsed.command is VoiceCommand.None) "Local parser returned None" else "Conversational/complex voice request"
                    val aiResult = try {
                        kotlinx.coroutines.withTimeoutOrNull(2200L) {
                            GeminiVoiceService.enhanceVoiceCommand(
                                spokenText = correctedText,
                                configuredWakeWord = settings.wakeWord,
                                modelName = settings.geminiModelName,
                                context = this@RemoteMicForegroundService
                            )
                        }
                    } catch (e: Exception) {
                        null
                    }

                    if (aiResult != null && aiResult.command !is VoiceCommand.None) {
                        geminiUsed = true
                        finalCommand = aiResult.command
                        if (settings.geminiVoiceTtsEnabled && aiResult.spokenFeedback.isNotBlank()) {
                            TtsSpeaker.speak(aiResult.spokenFeedback)
                        }
                    } else {
                        geminiUsed = false
                        geminiReason += "; Gemini timed out or yielded no command, fallback to local"
                        val cleanQueryText = VoiceCommandEngine.cleanQuery(correctedText, settings.wakeWord)
                        val isValidQuery = cleanQueryText.isNotBlank() && cleanQueryText.length >= 2 && !VoiceCommandEngine.isIncompleteOrNoise(cleanQueryText)
                        finalCommand = if (parsed.command !is VoiceCommand.None) {
                            parsed.command
                        } else if (isValidQuery) {
                            VoiceCommand.SearchAndPlay(cleanQueryText)
                        } else {
                            VoiceCommand.None
                        }
                    }
                } else {
                    geminiUsed = false
                    geminiReason = "Gemini not enabled or not configured; local resolution"
                    val cleanQueryText = VoiceCommandEngine.cleanQuery(correctedText, settings.wakeWord)
                    val isValidQuery = cleanQueryText.isNotBlank() && cleanQueryText.length >= 2 && !VoiceCommandEngine.isIncompleteOrNoise(cleanQueryText)
                    finalCommand = if (parsed.command !is VoiceCommand.None) {
                        parsed.command
                    } else if (isValidQuery) {
                        VoiceCommand.SearchAndPlay(cleanQueryText)
                    } else {
                        VoiceCommand.None
                    }
                }

                // 5. Diagnostic Logging for all stages
                Log.i(TAG, "RAW_WHISPER_TEXT: '$rawTranscription'")
                Log.i(TAG, "NORMALIZED_TEXT: '$normalizedText'")
                Log.i(TAG, "CORRECTED_TEXT: '$correctedText'")
                Log.i(TAG, "CORRECTION_APPLIED: ${correctionResult.applied}")
                Log.i(TAG, "CORRECTION_REASON: '${correctionResult.reason}'")
                Log.i(TAG, "CORRECTION_CONFIDENCE: ${correctionResult.confidence}")
                Log.i(TAG, "PARSED_INTENT: '$parsedIntent'")
                Log.i(TAG, "PARSED_ARTIST: '${parsedArtist ?: "none"}'")
                Log.i(TAG, "PARSED_TRACK: '${parsedTrack ?: "none"}'")
                Log.i(TAG, "GEMINI_USED: $geminiUsed")
                Log.i(TAG, "GEMINI_REASON: '$geminiReason'")
                Log.i(TAG, "FINAL_COMMAND: ${finalCommand.toDisplayAction()}")

                // 6. Dispatch command to host
                if (finalCommand !is VoiceCommand.None) {
                    dispatchCommandToHost(finalCommand, correctedText)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in transitionToCommandCaptureAndWhisper: ${e.message}", e)
            } finally {
                // Return to ESTADO 1
                Log.i(TAG, "VOICE_STATE: RETURNING_TO_WAKE")
                isProcessingCommand = false
                cancelWakeWordWindow(revertToStandby = true)

                if (isListeningLoopActive) {
                    initSpeechRecognizer()
                    startSpeechRecognitionSafely()
                }
            }
        }
    }

    private fun recordCommandAudioWithAudioRecord(speechRecognizerReleasedTs: Long): ShortArray? {
        Log.i(TAG, "AUDIORECORD_START_REQUEST at ${System.currentTimeMillis()}")
        val sampleRate = 16000
        val minBufSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBufSize * 2).coerceAtLeast(4096)

        var recorder: AudioRecord? = null
        var totalCapturedSamples = 0
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            }

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to initialize AudioRecord for command capture")
                return null
            }

            recorder.startRecording()
            val audioRecordStartedTs = System.currentTimeMillis()
            Log.i(TAG, "AUDIORECORD_STARTED_TIMESTAMP: $audioRecordStartedTs")
            val micHandoffGapMs = (audioRecordStartedTs - speechRecognizerReleasedTs).coerceAtLeast(0L)
            Log.i(TAG, "MIC_HANDOFF_GAP_MS: $micHandoffGapMs ms")

            // 1. PRE-ROLL Circular Buffer: 650 ms at 16kHz mono = 10,400 samples
            val preRollCapacity = ((sampleRate * PRE_ROLL_MS) / 1000L).toInt()
            val preRollBuffer = CircularAudioBuffer(preRollCapacity)

            val commandAudioList = ArrayList<Short>(sampleRate * 6)
            val frameBuffer = ShortArray(320) // 20ms frames at 16kHz

            var isVoiceStarted = false
            var voiceStartTime = 0L
            var lastVoiceTime = 0L
            var consecutiveVoiceFrames = 0
            var noiseFloor = 70.0
            val startWaitTime = System.currentTimeMillis()

            Log.i(TAG, "COMMAND_WAITING_FOR_SPEECH (waiting up to ${WAIT_FOR_COMMAND_START_MS}ms)")

            while (true) {
                if (isCaptureCancelledByMesh) {
                    Log.i(TAG, "AUDIORECORD_ABORTED: Mesh node '$meshWinnerNodeName' claimed activation")
                    break
                }
                val readCount = recorder.read(frameBuffer, 0, frameBuffer.size)
                if (readCount <= 0) continue

                var sumSquares = 0.0
                for (i in 0 until readCount) {
                    val s = frameBuffer[i].toDouble()
                    sumSquares += s * s
                }
                val rms = sqrt(sumSquares / readCount)
                ClientStateHolder.setRms(rms.toFloat())

                if (!isVoiceStarted) {
                    // Accumulate samples into circular PRE-ROLL buffer
                    preRollBuffer.write(frameBuffer, readCount)

                    // Track ambient noise floor dynamically during silence
                    noiseFloor = (0.95 * noiseFloor + 0.05 * rms).coerceIn(40.0, 450.0)

                    // VAD with Hysteresis: START_THRESHOLD > CONTINUE_THRESHOLD
                    val startThreshold = (noiseFloor + 120.0).coerceIn(160.0, 550.0)

                    if (rms >= startThreshold) {
                        consecutiveVoiceFrames++
                        // Require 3 consecutive frames (~60ms) of confident voice energy
                        if (consecutiveVoiceFrames >= 3) {
                            val now = System.currentTimeMillis()
                            isVoiceStarted = true
                            voiceStartTime = now
                            lastVoiceTime = now
                            Log.i(TAG, "VOICE_START_DETECTED at $now (rms=$rms, startThreshold=$startThreshold, noiseFloor=$noiseFloor)")

                            // Dump all PRE-ROLL samples into commandAudioList to preserve initial syllables
                            val preRollSamples = preRollBuffer.readAll()
                            for (s in preRollSamples) {
                                commandAudioList.add(s)
                            }
                            val preRollMs = (preRollSamples.size * 1000L) / sampleRate
                            Log.i(TAG, "PRE_ROLL_INCLUDED_MS=$preRollMs")

                            // Also append the triggering frame
                            for (i in 0 until readCount) {
                                commandAudioList.add(frameBuffer[i])
                            }
                            Log.i(TAG, "VOICE_STATE: CAPTURING_COMMAND")
                        }
                    } else {
                        consecutiveVoiceFrames = 0
                    }

                    // Timeout waiting for command start
                    if (!isVoiceStarted && (System.currentTimeMillis() - startWaitTime >= WAIT_FOR_COMMAND_START_MS)) {
                        Log.i(TAG, "WAIT_FOR_COMMAND_START timeout after ${WAIT_FOR_COMMAND_START_MS}ms without speech")
                        break
                    }
                } else {
                    // In CAPTURING_COMMAND state: collect all audio
                    for (i in 0 until readCount) {
                        commandAudioList.add(frameBuffer[i])
                    }

                    // Hysteresis continue threshold (lower and more permissive than start threshold)
                    val continueThreshold = (noiseFloor + 40.0).coerceIn(90.0, 350.0)
                    if (rms >= continueThreshold) {
                        lastVoiceTime = System.currentTimeMillis()
                        Log.d(TAG, "LAST_VOICE_DETECTED (rms=$rms >= continueThreshold=$continueThreshold)")
                    }

                    val voiceDurationMs = System.currentTimeMillis() - voiceStartTime
                    val silenceDurationMs = System.currentTimeMillis() - lastVoiceTime

                    // Silence Hangover check: do not terminate on short pauses; require 1000ms continuous silence
                    if (voiceDurationMs >= MIN_COMMAND_MS && silenceDurationMs >= END_SILENCE_MS) {
                        Log.i(TAG, "END_SILENCE_MS=$silenceDurationMs (voiceDuration=${voiceDurationMs}ms)")
                        break
                    }

                    // Maximum command duration safeguard (12s)
                    if (voiceDurationMs >= MAX_COMMAND_MS) {
                        Log.i(TAG, "MAX_COMMAND_DURATION_REACHED (${voiceDurationMs}ms)")
                        break
                    }
                }
            }

            // POST-ROLL: Ensure at least POST_ROLL_MS (850ms) of audio after the last detected voice frame
            if (isVoiceStarted) {
                Log.i(TAG, "POST_ROLL_STARTED")
                val elapsedSinceLastVoice = System.currentTimeMillis() - lastVoiceTime
                val remainingPostRollMs = (POST_ROLL_MS - elapsedSinceLastVoice).coerceAtLeast(0L)
                if (remainingPostRollMs > 0L) {
                    val postRollStart = System.currentTimeMillis()
                    while (System.currentTimeMillis() - postRollStart < remainingPostRollMs) {
                        val count = recorder.read(frameBuffer, 0, frameBuffer.size)
                        if (count > 0) {
                            for (i in 0 until count) {
                                commandAudioList.add(frameBuffer[i])
                            }
                        }
                    }
                }
                Log.i(TAG, "POST_ROLL_FINISHED")
            }

            totalCapturedSamples = commandAudioList.size
            return if (!isCaptureCancelledByMesh && isVoiceStarted && commandAudioList.isNotEmpty()) {
                commandAudioList.toShortArray()
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error recording command audio with AudioRecord: ${e.message}", e)
            return null
        } finally {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (_: Exception) {}
            Log.i(TAG, "MIC_OWNER: none")
            Log.i(TAG, "COMMAND_CAPTURE_FINISHED")
            val totalDurationMs = (totalCapturedSamples * 1000L) / sampleRate
            Log.i(TAG, "COMMAND_AUDIO_DURATION_MS=$totalDurationMs")
            Log.d(TAG, "AudioRecord stopped and released completely")
        }
    }

    private fun stopListeningService() {
        Log.i(TAG, "Stopping RemoteMicForegroundService...")
        isListeningLoopActive = false
        isManualPushToTalk = false
        isCurrentlyRecognizing = false
        isWakeWordActiveWindow = false
        mainHandler.removeCallbacksAndMessages(null)
        engineHealthScheduled = false

        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null

        applyBeepSuppression(false)

        ClientStateHolder.updateState {
            it.copy(
                isServiceRunning = false,
                isPushToTalk = false,
                rmsLevel = 0f,
                partialText = "",
                micState = ClientMicState.IDLE_DISCONNECTED,
                statusMessage = "⏸️ Escucha en pausa (Toca el micrófono para activar)"
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        MeshNodeCoordinator.unregisterYieldListener()
        stopListeningService()
        TtsSpeaker.shutdown()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        // Tolerant Voice Capture Parameters
        const val PRE_ROLL_MS = 650L
        const val WAIT_FOR_COMMAND_START_MS = 5000L
        const val MIN_COMMAND_MS = 400L
        const val END_SILENCE_MS = 1000L
        const val POST_ROLL_MS = 850L
        const val MAX_COMMAND_MS = 12000L

        const val ACTION_STOP_SERVICE = "com.example.voice.STOP_SERVICE"
        const val ACTION_START_MANUAL_PTT = "com.example.voice.START_MANUAL_PTT"
        const val ACTION_STOP_MANUAL_PTT = "com.example.voice.STOP_MANUAL_PTT"
        const val ACTION_CANCEL_MANUAL_PTT = "com.example.voice.CANCEL_MANUAL_PTT"
        const val ACTION_FORCE_REFRESH_ENGINE = "com.example.voice.FORCE_REFRESH_ENGINE"

        fun start(context: Context) {
            val intent = Intent(context, RemoteMicForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RemoteMicForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }

        fun forceRefresh(context: Context) {
            val intent = Intent(context, RemoteMicForegroundService::class.java).apply {
                action = ACTION_FORCE_REFRESH_ENGINE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startManualPushToTalk(context: Context) {
            val intent = Intent(context, RemoteMicForegroundService::class.java).apply {
                action = ACTION_START_MANUAL_PTT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopManualPushToTalk(context: Context) {
            val intent = Intent(context, RemoteMicForegroundService::class.java).apply {
                action = ACTION_STOP_MANUAL_PTT
            }
            context.startService(intent)
        }

        fun cancelManualPushToTalk(context: Context) {
            val intent = Intent(context, RemoteMicForegroundService::class.java).apply {
                action = ACTION_CANCEL_MANUAL_PTT
            }
            context.startService(intent)
        }
    }
}
