package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.ClientSettings
import com.example.voice.RemoteMicForegroundService
import java.io.DataOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ScheduledRebootManager {
    private const val TAG = "ScheduledRebootManager"
    private const val REBOOT_ALARM_REQ_CODE = 9021

    enum class RebootResult {
        ROOT_SYSTEM_REBOOT,
        SOFT_MAINTENANCE_REFRESH,
        FAILED
    }

    fun scheduleDailyReboot(context: Context) {
        val settings = ClientSettings(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, RebootAlarmReceiver::class.java).apply {
            action = RebootAlarmReceiver.ACTION_SCHEDULED_REBOOT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REBOOT_ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!settings.dailyRebootEnabled) {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Daily reboot disabled, alarm cancelled")
            return
        }

        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.dailyRebootHour)
            set(Calendar.MINUTE, settings.dailyRebootMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time today has already passed, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val triggerAtMillis = targetCalendar.timeInMillis
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled daily reboot for: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(triggerAtMillis))}")
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot set exact alarm (exact alarm permission missing?), falling back to set(): ${e.message}")
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to schedule reboot alarm: ${ex.message}")
            }
        }
    }

    fun getNextScheduledTimeFormatted(context: Context): String {
        val settings = ClientSettings(context)
        if (!settings.dailyRebootEnabled) return "Desactivado"
        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.dailyRebootHour)
            set(Calendar.MINUTE, settings.dailyRebootMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val sdf = SimpleDateFormat("HH:mm '(mañana)'", Locale("es", "ES"))
        val now = Calendar.getInstance()
        return if (targetCalendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
            String.format(Locale.getDefault(), "%02d:%02d (hoy)", settings.dailyRebootHour, settings.dailyRebootMinute)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d (mañana)", settings.dailyRebootHour, settings.dailyRebootMinute)
        }
    }

    /**
     * Executes reboot or graceful maintenance.
     * 1. Attempts root command `reboot`.
     * 2. If no root access, performs a clean app/satellite memory recycling and restarts microphone listening.
     */
    fun performRebootOrMaintenance(context: Context): RebootResult {
        Log.i(TAG, "Initiating scheduled maintenance/reboot cycle...")
        val rootSuccess = tryRootReboot()
        if (rootSuccess) {
            return RebootResult.ROOT_SYSTEM_REBOOT
        }

        // Soft Maintenance: Refresh app and satellite microphone
        Log.i(TAG, "Root not available, executing soft maintenance and satellite memory refresh")
        try {
            val settings = ClientSettings(context)
            if (settings.autoStartListeningOnLaunch || settings.continuousListening) {
                RemoteMicForegroundService.stop(context)
                Thread.sleep(300)
                RemoteMicForegroundService.start(context)
            }

            // Launch/Bring MainActivity to front to keep satellite active
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(launchIntent)

            // Suggest garbage collection to clean up any long-running memory allocations
            System.gc()
            return RebootResult.SOFT_MAINTENANCE_REFRESH
        } catch (e: Exception) {
            Log.e(TAG, "Soft maintenance failed: ${e.message}")
            return RebootResult.FAILED
        }
    }

    private fun tryRootReboot(): Boolean {
        val rebootCommands = arrayOf(
            "su -c reboot",
            "su -c /system/bin/reboot",
            "su -c svc power reboot",
            "reboot"
        )
        for (cmd in rebootCommands) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot"))
                val os = DataOutputStream(process.outputStream)
                os.writeBytes("reboot\n")
                os.flush()
                os.writeBytes("exit\n")
                os.flush()
                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    Log.i(TAG, "Root reboot command '$cmd' dispatched successfully")
                    return true
                }
            } catch (e: Exception) {
                Log.d(TAG, "Root command attempt failed ($cmd): ${e.message}")
            }
        }
        return false
    }
}
