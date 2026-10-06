package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.data.ClientSettings
import com.example.network.RemoteClientHolder
import com.example.voice.RemoteMicForegroundService

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "BootReceiver received broadcast action: $action")

        val settings = ClientSettings(context)

        // Always ensure scheduled maintenance alarm is configured after device restart
        if (settings.dailyRebootEnabled) {
            ScheduledRebootManager.scheduleDailyReboot(context)
        }

        // Check if automatic launch on boot is enabled
        if (settings.startOnBootEnabled) {
            Log.i(TAG, "Start on boot is enabled. Auto-launching MainActivity and Satellite services...")

            // 1. Acquire temporary WakeLock (10 seconds) so CPU remains awake while bootstrapping
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "serchmic:boot_receiver_wakelock"
                )
                wakeLock?.acquire(10_000L)
            } catch (e: Exception) {
                Log.w(TAG, "Could not acquire WakeLock on boot: ${e.message}")
            }

            try {
                // 2. Start Foreground Listening Service / WebSocket client immediately
                if (settings.autoStartListeningOnLaunch || settings.continuousListening) {
                    RemoteMicForegroundService.start(context)
                } else if (settings.autoConnectOnLaunch && settings.hostIp.isNotBlank()) {
                    RemoteClientHolder.connect(context)
                }

                // 3. Launch MainActivity with both direct Intent and FullScreenIntent
                AutostartHelper.launchMainActivityFromBoot(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting app on boot: ${e.message}", e)
            }
        } else {
            Log.d(TAG, "Start on boot is disabled in settings.")
        }
    }
}
