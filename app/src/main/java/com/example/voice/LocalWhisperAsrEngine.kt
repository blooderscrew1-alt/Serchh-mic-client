package com.example.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt
import com.example.network.MeshNodeCoordinator

/**
 * Estados estrictos de la Máquina de Estados Finita (FSM) para la captura y decodificación ASR.
 */
enum class AsrFsmState {
    /** ESTADO 1: El detector de Wake Word lee continuamente el micrófono en reposo. Whisper está INACTIVO. */
    ESPERANDO_WAKE_WORD,

    /** ESTADO 2: Captura del buffer de audio de 3 a 5 segundos (o VAD de silencio) tras detectar la palabra clave. */
    GRABANDO_COMANDO,

    /** ESTADO 3: Micrófono CERRADO/LIBERADO. Inferencia pesada de Whisper en Dispatchers.IO con nThreads = 2. */
    PROCESANDO_WHISPER
}

/**
 * Motor ASR Local basado en FSM (Máquina de Estados Finita).
 * Garantiza un manejo de micrófono único y compartido con consumo de CPU controlado (nThreads = 2).
 */
class LocalWhisperAsrEngine(
    private val context: Context,
    private val onWakeWordDetected: (wakeWord: String) -> Unit,
    private val onCommandTranscribed: (rawText: String, parsed: MusicParsedCommand) -> Unit,
    private val onStateChanged: (state: AsrFsmState) -> Unit = {},
    private val onVadStateChanged: (isSpeaking: Boolean, rmsDb: Float) -> Unit = { _, _ -> }
) {
    companion object {
        private const val TAG = "LocalWhisperAsrEngine"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        // CPU & Whisper Thread Control (Máximo 2 hilos para no ahogar la CPU)
        private const val WHISPER_CONTROLLED_THREADS = 2

        // Tolerant Audio Capture Parameters
        const val PRE_ROLL_MS = 650L
        const val WAIT_FOR_COMMAND_START_MS = 5000L
        const val MIN_COMMAND_MS = 400L
        const val END_SILENCE_MS = 1000L
        const val POST_ROLL_MS = 850L
        const val MAX_COMMAND_MS = 12000L
    }

    private val isRunning = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var fsmJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    // FSM State Tracking
    @Volatile
    var currentState: AsrFsmState = AsrFsmState.ESPERANDO_WAKE_WORD
        private set(value) {
            field = value
            onStateChanged(value)
            Log.i(TAG, "FSM Transition -> State: $value")
        }

    // Audio Manager for Transient Focus (Ducking/Muting multimedia)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Dynamic Adaptive Noise Floor VAD
    private var adaptiveNoiseFloor = 120.0

    // Tone generator for wake word confirmation beep
    private var toneGenerator: ToneGenerator? = null

    var language: String = "auto" // "auto", "es", or "en"

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        } catch (e: Exception) {
            Log.w(TAG, "Could not initialize ToneGenerator: ${e.message}")
        }
    }

    /**
     * Inicia la Máquina de Estados Finita (FSM) arrancando en ESTADO 1 (ESPERANDO_WAKE_WORD).
     */
    fun start() {
        if (isRunning.getAndSet(true)) {
            Log.d(TAG, "LocalWhisperAsrEngine FSM is already running")
            return
        }

        currentState = AsrFsmState.ESPERANDO_WAKE_WORD
        fsmJob = coroutineScope.launch(Dispatchers.IO) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            runFsmLoop()
        }
    }

    /**
     * Bucle Principal de la Máquina de Estados Finita (FSM).
     */
    private suspend fun runFsmLoop() {
        while (isRunning.get() && coroutineScope.isActive) {
            when (currentState) {
                AsrFsmState.ESPERANDO_WAKE_WORD -> {
                    // ESTADO 1: Escuchando continuamente la palabra clave. Whisper está INACTIVO.
                    runStateEsperandoWakeWord()
                }

                AsrFsmState.GRABANDO_COMANDO -> {
                    // ESTADO 2: Capturando de 3 a 5 segundos de audio del comando tras el beep.
                    val commandPcmData = runStateGrabandoComando()

                    if (commandPcmData != null && commandPcmData.isNotEmpty()) {
                        // Transición a ESTADO 3
                        currentState = AsrFsmState.PROCESANDO_WHISPER
                        runStateProcesandoWhisper(commandPcmData)
                    } else {
                        // Si la grabación fue nula, volver de inmediato a ESTADO 1
                        abandonTransientAudioFocus()
                        currentState = AsrFsmState.ESPERANDO_WAKE_WORD
                    }
                }

                AsrFsmState.PROCESANDO_WHISPER -> {
                    // El estado 3 se gestiona de forma asíncrona dentro de runStateProcesandoWhisper
                    delay(100)
                }
            }
        }
    }

    /**
     * ESTADO 1: Lectura continua del micrófono por el detector de Wake Word.
     * Whisper NO lee ni procesa en este estado.
     */
    private suspend fun runStateEsperandoWakeWord() {
        ensureAudioRecordStarted()
        val shortBuffer = ShortArray(1024)
        val speechAudioBuffer = mutableListOf<Short>()
        var isSpeaking = false
        var consecutiveSilenceFrames = 0

        while (isRunning.get() && currentState == AsrFsmState.ESPERANDO_WAKE_WORD && coroutineScope.isActive) {
            val readCount = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: 0
            if (readCount <= 0) continue

            // 1. Calcular RMS para VAD
            var sumSquares = 0.0
            for (i in 0 until readCount) {
                val sample = shortBuffer[i].toDouble()
                sumSquares += sample * sample
            }
            val rms = sqrt(sumSquares / readCount)
            val rmsDb = (20 * kotlin.math.log10(rms.coerceAtLeast(1.0))).toFloat()

            val dynamicThreshold = (adaptiveNoiseFloor * 1.5).coerceIn(140.0, 420.0)
            val frameHasSpeech = rms > dynamicThreshold
            onVadStateChanged(frameHasSpeech || isSpeaking, rmsDb)

            if (frameHasSpeech) {
                if (!isSpeaking) {
                    isSpeaking = true
                    speechAudioBuffer.clear()
                }
                consecutiveSilenceFrames = 0
                for (i in 0 until readCount) {
                    speechAudioBuffer.add(shortBuffer[i])
                }
            } else {
                adaptiveNoiseFloor = (adaptiveNoiseFloor * 0.95) + (rms * 0.05)
                if (isSpeaking) {
                    for (i in 0 until readCount) {
                        speechAudioBuffer.add(shortBuffer[i])
                    }
                    consecutiveSilenceFrames++

                    if (consecutiveSilenceFrames >= 12) { // ~380ms de silencio
                        isSpeaking = false
                        val audioSegment = speechAudioBuffer.toShortArray()
                        speechAudioBuffer.clear()
                        consecutiveSilenceFrames = 0

                        if (audioSegment.size >= SAMPLE_RATE * 0.3) {
                            val isWakeWord = checkWakeWordInSegment(audioSegment)
                            if (isWakeWord) {
                                val decision = MeshNodeCoordinator.claimWakeWord(rms.toFloat(), "Whisper Local")
                                if (decision is MeshNodeCoordinator.ArbitrationDecision.Yield) {
                                    Log.i(TAG, "ASR_FSM: Yielding wake word to ${decision.winnerName}: ${decision.reason}")
                                    ClientStateHolder.addLog("[Sintonía Mesh] Palabra detectada pero cediendo a '${decision.winnerName}'. Captura cancelada en este nodo.")
                                    ClientStateHolder.setMicState(ClientMicState.LISTENING_STANDBY, "🛰️ Sintonía Mesh: Cediendo a ${decision.winnerName}")
                                    return
                                }

                                val clientSettings = com.example.data.ClientSettings(context)
                                if (clientSettings.wakeWordTriggersHostMicDirectly) {
                                    Log.i(TAG, "EVENT: Wake Word Detected -> Instant Host Mic Activation (skipping State 2 & 3)")
                                    com.example.network.RemoteClientHolder.getClient(context).sendInstantHostMicActivation(
                                        hostIp = clientSettings.hostIp,
                                        port = clientSettings.hostPort,
                                        wakeWord = clientSettings.wakeWord
                                    )
                                    playWakeConfirmationTone()
                                    ClientStateHolder.notifyCommandSuccess("⚡ Host: Activa tu micrófono")
                                    currentState = AsrFsmState.ESPERANDO_WAKE_WORD
                                    return
                                }

                                // EVENTO: Palabra Maestra Detectada!
                                Log.i(TAG, "EVENT: Wake Word Detected in State 1!")
                                playWakeConfirmationTone()
                                requestTransientAudioFocus()
                                onWakeWordDetected("Palabra Maestra Detectada")

                                // Transición inmediata a ESTADO 2
                                currentState = AsrFsmState.GRABANDO_COMANDO
                                return
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * ESTADO 2: Captura tolerante y completa del comando tras la Wake Word.
     * Incorpora Pre-roll (650ms), VAD con histéresis, Silence Hangover (1000ms) y Post-roll (850ms).
     */
    private suspend fun runStateGrabandoComando(): ShortArray? {
        ensureAudioRecordStarted()
        val preRollCapacity = ((SAMPLE_RATE * PRE_ROLL_MS) / 1000L).toInt()
        val preRollBuffer = CircularAudioBuffer(preRollCapacity)
        val shortBuffer = ShortArray(320)
        val commandAudioList = mutableListOf<Short>()

        val startTime = System.currentTimeMillis()
        var lastVoiceTime = 0L
        var voiceStartTime = 0L
        var hasDetectedSpeech = false
        var consecutiveVoiceFrames = 0
        var noiseFloor = adaptiveNoiseFloor.coerceIn(40.0, 450.0)

        Log.i(TAG, "STATE 2: WAITING_FOR_COMMAND_START (Pre-roll 650ms, VAD hysteresis active)")

        while (isRunning.get() && currentState == AsrFsmState.GRABANDO_COMANDO && coroutineScope.isActive) {
            val readCount = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: 0
            if (readCount <= 0) continue

            var sumSquares = 0.0
            for (i in 0 until readCount) {
                val sample = shortBuffer[i].toDouble()
                sumSquares += sample * sample
            }
            val rms = sqrt(sumSquares / readCount)
            val rmsDb = (20 * kotlin.math.log10(rms.coerceAtLeast(1.0))).toFloat()
            onVadStateChanged(true, rmsDb)

            if (!hasDetectedSpeech) {
                preRollBuffer.write(shortBuffer, readCount)
                noiseFloor = (0.95 * noiseFloor + 0.05 * rms).coerceIn(40.0, 450.0)
                val startThreshold = (noiseFloor + 120.0).coerceIn(160.0, 550.0)

                if (rms >= startThreshold) {
                    consecutiveVoiceFrames++
                    if (consecutiveVoiceFrames >= 3) {
                        hasDetectedSpeech = true
                        val now = System.currentTimeMillis()
                        voiceStartTime = now
                        lastVoiceTime = now
                        Log.i(TAG, "VOICE_START_DETECTED at $now (rms=$rms, threshold=$startThreshold, noiseFloor=$noiseFloor)")

                        val preRoll = preRollBuffer.readAll()
                        for (s in preRoll) {
                            commandAudioList.add(s)
                        }
                        val preRollMs = (preRoll.size * 1000L) / SAMPLE_RATE
                        Log.i(TAG, "PRE_ROLL_INCLUDED_MS=$preRollMs")

                        for (i in 0 until readCount) {
                            commandAudioList.add(shortBuffer[i])
                        }
                        Log.i(TAG, "VOICE_STATE: CAPTURING_COMMAND")
                    }
                } else {
                    consecutiveVoiceFrames = 0
                }

                if (!hasDetectedSpeech && (System.currentTimeMillis() - startTime >= WAIT_FOR_COMMAND_START_MS)) {
                    Log.i(TAG, "WAIT_FOR_COMMAND_START timeout after ${WAIT_FOR_COMMAND_START_MS}ms")
                    break
                }
            } else {
                for (i in 0 until readCount) {
                    commandAudioList.add(shortBuffer[i])
                }

                val continueThreshold = (noiseFloor + 40.0).coerceIn(90.0, 350.0)
                if (rms >= continueThreshold) {
                    lastVoiceTime = System.currentTimeMillis()
                    Log.d(TAG, "LAST_VOICE_DETECTED (rms=$rms)")
                }

                val voiceDurationMs = System.currentTimeMillis() - voiceStartTime
                val silenceDurationMs = System.currentTimeMillis() - lastVoiceTime

                if (voiceDurationMs >= MIN_COMMAND_MS && silenceDurationMs >= END_SILENCE_MS) {
                    Log.i(TAG, "END_SILENCE_MS=$silenceDurationMs (voiceDuration=${voiceDurationMs}ms)")
                    break
                }

                if (voiceDurationMs >= MAX_COMMAND_MS) {
                    Log.i(TAG, "MAX_COMMAND_DURATION_REACHED (${voiceDurationMs}ms)")
                    break
                }
            }
        }

        if (hasDetectedSpeech) {
            Log.i(TAG, "POST_ROLL_STARTED")
            val elapsedSinceLastVoice = System.currentTimeMillis() - lastVoiceTime
            val remainingPostRollMs = (POST_ROLL_MS - elapsedSinceLastVoice).coerceAtLeast(0L)
            if (remainingPostRollMs > 0L) {
                val postRollStart = System.currentTimeMillis()
                while (System.currentTimeMillis() - postRollStart < remainingPostRollMs) {
                    val count = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: 0
                    if (count > 0) {
                        for (i in 0 until count) commandAudioList.add(shortBuffer[i])
                    }
                }
            }
            Log.i(TAG, "POST_ROLL_FINISHED")
        }

        val totalDurationMs = (commandAudioList.size * 1000L) / SAMPLE_RATE
        Log.i(TAG, "COMMAND_AUDIO_DURATION_MS=$totalDurationMs")

        return if (hasDetectedSpeech && commandAudioList.isNotEmpty()) {
            commandAudioList.toShortArray()
        } else {
            null
        }
    }

    /**
     * ESTADO 3: Cierra/libera la captura del micrófono y pasa el arreglo a Whisper en Dispatchers.IO (nThreads = 2).
     */
    private suspend fun runStateProcesandoWhisper(pcmAudioData: ShortArray) {
        Log.i(TAG, "STATE 3: Closing microphone capture and passing audio to Whisper.cpp...")

        // CRÍTICO: CERRAR Y LIBERAR MICRÓFONO PARA DEJARLO 100% LIBRE
        stopAndReleaseAudioRecord()

        withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()

                // Convertir PCM 16-bit a Float Array (-1.0f .. 1.0f)
                val floatSamples = FloatArray(pcmAudioData.size) { i ->
                    pcmAudioData[i] / 32768.0f
                }

                // Inferencia controlada de Whisper (nThreads = 2 para no sobrecargar CPU)
                val transcription = WhisperNativeBridge.decode(
                    samples = floatSamples,
                    numThreads = WHISPER_CONTROLLED_THREADS,
                    language = if (language == "auto") "es" else language,
                    translateToEnglish = false
                )

                val durationMs = System.currentTimeMillis() - startTime
                Log.i(TAG, "Whisper Decoded in ${durationMs}ms (Threads=$WHISPER_CONTROLLED_THREADS): '$transcription'")

                if (transcription.isNotBlank()) {
                    val stripped = MusicCommandParser.stripWakeWord(transcription)
                    val textToParse = if (stripped.isNotBlank()) stripped else transcription
                    val parsed = MusicCommandParser.parse(textToParse)
                    onCommandTranscribed(textToParse, parsed)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing Whisper in State 3: ${e.message}", e)
            } finally {
                // Restablecer foco de audio multimedia
                abandonTransientAudioFocus()

                // Regresar de inmediato al ESTADO 1
                currentState = AsrFsmState.ESPERANDO_WAKE_WORD
            }
        }
    }

    /**
     * Evalúa si un segmento corto de audio contiene la palabra clave o patrones de voz.
     */
    private fun checkWakeWordInSegment(pcmData: ShortArray): Boolean {
        // Conversión a floats
        val floatSamples = FloatArray(pcmData.size) { i -> pcmData[i] / 32768.0f }

        // Decodificación ultra-rápida de prueba
        val transcript = WhisperNativeBridge.decode(
            samples = floatSamples,
            numThreads = 2,
            language = if (language == "auto") "es" else language,
            translateToEnglish = false
        )

        return containsWakeWord(transcript)
    }

    /**
     * Verifica si el texto transcrito contiene la palabra clave configurada de forma estricta.
     */
    private fun containsWakeWord(text: String): Boolean {
        if (text.isBlank()) return false
        val configuredWakeWord = try {
            com.example.data.ClientSettings(context).wakeWord
        } catch (_: Exception) {
            "Música"
        }
        return VoiceCommandEngine.isWakeWordDetected(text, configuredWakeWord)
    }

    /**
     * Asegura que AudioRecord esté iniciado para captura.
     */
    private fun ensureAudioRecordStarted() {
        if (audioRecord != null && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return
        }

        stopAndReleaseAudioRecord()

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT
        )

        for (source in sources) {
            try {
                val record = AudioRecord(
                    source,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord = record
                    audioRecord?.startRecording()
                    Log.i(TAG, "AudioRecord started with source: $source")
                    break
                } else {
                    record.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error starting AudioRecord with source $source: ${e.message}")
            }
        }
    }

    /**
     * Detiene y libera la interfaz del micrófono.
     */
    private fun stopAndReleaseAudioRecord() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audioRecord: ${e.message}")
        } finally {
            audioRecord = null
        }
    }

    /**
     * Solicita atenuación/corte temporal de audio multimedia (Audio Focus Ducking).
     */
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

    /**
     * Libera el foco de audio restaurando el volumen multimedia previo.
     */
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

    /**
     * Emite un tono acústico breve al detectar la palabra clave.
     */
    fun playWakeConfirmationTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Exception) {
            Log.w(TAG, "Error playing tone: ${e.message}")
        }
    }

    /**
     * Detiene por completo el motor ASR y libera recursos.
     */
    fun stop() {
        isRunning.set(false)
        fsmJob?.cancel()
        fsmJob = null

        stopAndReleaseAudioRecord()
        abandonTransientAudioFocus()

        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}

        Log.i(TAG, "LocalWhisperAsrEngine FSM stopped")
    }
}

/**
 * Interface nativa bridge para ejecución local de Whisper.cpp en Android.
 */
object WhisperNativeBridge {
    private const val TAG = "WhisperNativeBridge"

    @Volatile
    private var isLibraryLoaded = false

    @Volatile
    private var isModelLoaded = false

    @Volatile
    private var modelPath: String? = null

    @Volatile
    private var nativeContextPtr: Long = 0L

    init {
        try {
            System.loadLibrary("whisper")
            isLibraryLoaded = true
            Log.i(TAG, "WHISPER_NATIVE_LIB_LOADED: libwhisper.so loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            isLibraryLoaded = false
            Log.w(TAG, "WHISPER_MODEL_ERROR: Native libwhisper.so library not present in APK runtime (${e.message})")
        } catch (e: Exception) {
            isLibraryLoaded = false
            Log.e(TAG, "WHISPER_MODEL_ERROR: Exception loading native whisper library: ${e.message}")
        }
    }

    fun isReady(): Boolean {
        return isLibraryLoaded && isModelLoaded && nativeContextPtr != 0L
    }

    fun autoLoadModelIfAvailable(context: Context): Boolean {
        if (isReady()) return true
        if (!isLibraryLoaded) return false
        val candidates = listOf(
            File(context.filesDir, "models"),
            context.filesDir,
            context.getExternalFilesDir(null),
            context.cacheDir,
            File("/sdcard/Download")
        )
        for (folder in candidates) {
            try {
                if (folder != null && folder.exists() && folder.isDirectory) {
                    val modelFiles = folder.listFiles { f ->
                        f.isFile && f.length() > 500000L && (f.name.endsWith(".bin", ignoreCase = true) || f.name.contains("whisper", ignoreCase = true) || f.name.contains("ggml", ignoreCase = true))
                    }
                    if (!modelFiles.isNullOrEmpty()) {
                        val chosen = modelFiles[0]
                        Log.i(TAG, "Auto-discovered candidate Whisper model: ${chosen.absolutePath}")
                        if (loadModel(chosen)) return true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking candidate folder ${folder?.path}: ${e.message}")
            }
        }
        return isReady()
    }

    fun loadModel(modelFile: File): Boolean {
        if (!modelFile.exists() || modelFile.length() <= 0) {
            isModelLoaded = false
            nativeContextPtr = 0L
            Log.w(TAG, "WHISPER_MODEL_ERROR: Model file does not exist or is empty at '${modelFile.absolutePath}'")
            return false
        }

        if (!isLibraryLoaded) {
            Log.w(TAG, "WHISPER_MODEL_ERROR: Cannot load model because native libwhisper.so is not available")
            return false
        }

        return try {
            val ctx = nativeInitModelContext(modelFile.absolutePath)
            if (ctx != 0L) {
                nativeContextPtr = ctx
                modelPath = modelFile.absolutePath
                isModelLoaded = true
                Log.i(TAG, "WHISPER_MODEL_LOADED: Model successfully initialized at '${modelFile.absolutePath}' with context ptr=$ctx")
                true
            } else {
                nativeContextPtr = 0L
                isModelLoaded = false
                Log.e(TAG, "WHISPER_MODEL_ERROR: nativeInitModelContext returned null context (0L) for '${modelFile.absolutePath}'")
                false
            }
        } catch (e: Throwable) {
            nativeContextPtr = 0L
            isModelLoaded = false
            Log.e(TAG, "WHISPER_MODEL_ERROR: Exception initializing model context: ${e.message}", e)
            false
        }
    }

    fun decode(
        samples: FloatArray,
        numThreads: Int,
        language: String,
        translateToEnglish: Boolean = false
    ): String {
        if (samples.isEmpty()) {
            return ""
        }

        if (!isReady()) {
            Log.w(TAG, "WHISPER_MODEL_ERROR: Whisper engine not ready (libraryLoaded=$isLibraryLoaded, modelLoaded=$isModelLoaded). Skipping native decode.")
            return ""
        }

        return try {
            val result = nativeFullTranscribe(
                contextPtr = nativeContextPtr,
                samples = samples,
                numThreads = numThreads.coerceIn(1, 2),
                language = language,
                translate = translateToEnglish
            )
            result ?: ""
        } catch (e: Throwable) {
            Log.e(TAG, "WHISPER_MODEL_ERROR: Exception during native decoding: ${e.message}", e)
            ""
        }
    }

    fun freeModel() {
        if (nativeContextPtr != 0L && isLibraryLoaded) {
            try {
                nativeFreeContext(nativeContextPtr)
                Log.i(TAG, "WHISPER_MODEL_FREED: Released native model context $nativeContextPtr")
            } catch (e: Throwable) {
                Log.w(TAG, "Error releasing native model context: ${e.message}")
            }
        }
        nativeContextPtr = 0L
        isModelLoaded = false
    }

    // Declaraciones de métodos nativos JNI
    private external fun nativeInitModelContext(modelPath: String): Long
    private external fun nativeFullTranscribe(
        contextPtr: Long,
        samples: FloatArray,
        numThreads: Int,
        language: String,
        translate: Boolean
    ): String?
    private external fun nativeFreeContext(contextPtr: Long)
}
