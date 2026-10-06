package com.example.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ClientSettings
import com.example.network.RemoteClientHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 24/7 Continuous Android Foreground Service for 100% Local, Offline Voice Recognition.
 * Operates without external servers or paid cloud APIs.
 * Optimized for stationary devices plugged into power.
 */
class LocalOfflineVoiceService : Service() {

    companion object {
        private const val TAG = "LocalOfflineVoiceSvc"
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "serch_local_offline_voice_channel"

        const val ACTION_START_247 = "com.example.voice.ACTION_START_LOCAL_OFFLINE"
        const val ACTION_STOP_247 = "com.example.voice.ACTION_STOP_LOCAL_OFFLINE"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun startService(context: Context) {
            Log.i(TAG, "Delegating startService to RemoteMicForegroundService for unified single microphone ownership")
            RemoteMicForegroundService.start(context)
        }

        fun stopService(context: Context) {
            Log.i(TAG, "Delegating stopService to RemoteMicForegroundService")
            RemoteMicForegroundService.stop(context)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var localAsrEngine: LocalWhisperAsrEngine? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "LocalOfflineVoiceService onCreate - Initializing 24/7 Local ASR")
        createNotificationChannel()
        acquireWakeLock()
        initAsrEngine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_247
        Log.d(TAG, "onStartCommand action: $action")

        if (action == ACTION_STOP_247) {
            isRunning = false
            stopSelf()
            return START_NOT_STICKY
        }

        isRunning = true
        startForegroundNotification("Escuchando comandos locales 24/7 (Whisper Offline)")
        localAsrEngine?.start()
        ClientStateHolder.setMicState(ClientMicState.LISTENING_STANDBY, "Whisper Local 24/7 Activo")

        return START_STICKY
    }

    private fun initAsrEngine() {
        localAsrEngine = LocalWhisperAsrEngine(
            context = this,
            onWakeWordDetected = { wakeWord ->
                Log.i(TAG, "Local Wake Word Heard: '$wakeWord'")
                serviceScope.launch {
                    ClientStateHolder.showWakeWordPopup(isListening = true)
                    ClientStateHolder.setPartialText("🎤 $wakeWord...")
                    updateNotification("Palabra clave detectada: $wakeWord")
                }
            },
            onCommandTranscribed = { rawText, parsed ->
                Log.i(TAG, "Local Command Transcribed: '$rawText' -> Action=${parsed.action}, Query=${parsed.searchQuery}")
                serviceScope.launch {
                    handleParsedCommand(rawText, parsed)
                }
            },
            onStateChanged = { fsmState ->
                val statusMessage = when (fsmState) {
                    AsrFsmState.ESPERANDO_WAKE_WORD -> "Estado 1: Esperando Palabra Maestra (Escucha activa)"
                    AsrFsmState.GRABANDO_COMANDO -> "Estado 2: Grabando comando de voz (3-5s)..."
                    AsrFsmState.PROCESANDO_WHISPER -> "Estado 3: Inferencia Whisper (nThreads = 2, Mic liberado)"
                }
                Log.d(TAG, "FSM State Changed: $fsmState -> $statusMessage")
                serviceScope.launch {
                    updateNotification(statusMessage)
                }
            },
            onVadStateChanged = { isSpeaking, rmsDb ->
                if (isSpeaking) {
                    ClientStateHolder.setRms(rmsDb / 10f)
                }
            }
        )
    }

    private suspend fun handleParsedCommand(rawText: String, parsed: MusicParsedCommand) {
        val summary = when {
            parsed.isMediaControl -> "Control: ${parsed.action}"
            parsed.searchQuery.isNotBlank() -> "Música: ${parsed.searchQuery}"
            else -> rawText
        }

        ClientStateHolder.notifyCommandSending(summary)
        updateNotification("Ejecutando: $summary")

        // Map parsed command to host command
        val cmdType = when (parsed.controlType) {
            MediaControlType.PAUSE -> "PAUSE"
            MediaControlType.PLAY_RESUME -> "PLAY"
            MediaControlType.NEXT_TRACK -> "NEXT"
            MediaControlType.PREVIOUS_TRACK -> "PREVIOUS"
            MediaControlType.VOLUME_UP -> "VOLUME_UP"
            MediaControlType.VOLUME_DOWN -> "VOLUME_DOWN"
            MediaControlType.VOLUME_SET -> "VOLUME_SET_${parsed.searchQuery}"
            MediaControlType.MUTE -> "MUTE"
            MediaControlType.SEARCH_AND_PLAY -> "SEARCH_PLAY"
            else -> "SEARCH_PLAY"
        }

        val settings = ClientSettings(this)
        val client = RemoteClientHolder.getClient(this)
        val success = client.sendVoiceCommand(
            commandType = cmdType,
            songQuery = parsed.searchQuery,
            rawSpokenText = rawText,
            hostIp = settings.hostIp,
            port = settings.hostPort
        )

        if (success) {
            ClientStateHolder.notifyCommandSuccess(summary)
            updateNotification("Comando enviado: $summary")
        } else {
            ClientStateHolder.setMicState(ClientMicState.COMMAND_ERROR, "Error al enviar al Host")
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SerchMic:LocalOfflineVoiceWakeLock").apply {
                setReferenceCounted(false)
                acquire()
            }
            Log.i(TAG, "Partial WakeLock acquired for 24/7 operation")
        } catch (e: Exception) {
            Log.w(TAG, "Error acquiring WakeLock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing WakeLock: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SerchMic Local ASR 24/7 Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Reconocimiento de voz 100% offline y local con Whisper"
                setShowBadge(false)
                setSound(null, null)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Reconocedor Local Whisper 24/7")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startForegroundNotification(statusText: String) {
        val notification = buildNotification(statusText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(statusText: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "LocalOfflineVoiceService onDestroy")
        isRunning = false
        localAsrEngine?.stop()
        localAsrEngine = null
        releaseWakeLock()
        serviceScope.cancel()
        ClientStateHolder.setMicState(ClientMicState.IDLE_DISCONNECTED, "Reconocedor Local detenido")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
