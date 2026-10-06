package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object AutostartHelper {
    private const val TAG = "AutostartHelper"
    const val BOOT_LAUNCH_CHANNEL_ID = "serch_boot_autostart_channel"
    private const val NOTIF_BOOT_LAUNCH_ID = 7070

    /**
     * Bulletproof launch of MainActivity from Boot / Background.
     * Uses:
     * 1. Direct Activity Intent with NEW_TASK + CLEAR_TOP + SINGLE_TOP + REORDER_TO_FRONT
     * 2. High-priority FullScreenIntent Notification (the official Android 10-15 standard to start UI from background)
     */
    fun launchMainActivityFromBoot(context: Context) {
        Log.i(TAG, "Executing launchMainActivityFromBoot...")

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
        }

        // 1. Direct startActivity attempt
        try {
            context.startActivity(launchIntent)
            Log.i(TAG, "Direct startActivity invoked successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Direct startActivity had exception (may be restricted by Android 10+ BAL): ${e.message}")
        }

        // 2. Dispatch high-priority FullScreenIntent notification
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        BOOT_LAUNCH_CHANNEL_ID,
                        "Auto-inicio del Sistema",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Permite a Serch Mic Client abrirse automáticamente al encender el teléfono"
                        setSound(null, null)
                        enableVibration(false)
                        setShowBadge(false)
                        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    9911,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )

                val notif = NotificationCompat.Builder(context, BOOT_LAUNCH_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                    .setContentTitle("Serch Mic Client")
                    .setContentText("Iniciando micrófono satélite y enlace...")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setContentIntent(fullScreenPendingIntent)
                    .setAutoCancel(true)
                    .setTimeoutAfter(8000L) // auto-dismiss after 8 seconds
                    .build()

                notificationManager.notify(NOTIF_BOOT_LAUNCH_ID, notif)
                Log.i(TAG, "FullScreenIntent notification dispatched for boot launch.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting FullScreenIntent notification: ${e.message}")
        }
    }

    /**
     * Checks if the app has permission to draw over other apps (Overlay permission).
     * This permission allows launching activities from background seamlessly on Android 10+.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Opens system overlay permission settings for this app.
     */
    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to open overlay settings: ${e.message}")
                openAppDetailsSettings(context)
            }
        }
    }

    /**
     * Checks if battery optimization is disabled for this app.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        }
        return true
    }

    /**
     * Requests disabling battery optimization so background services and boot receivers are never killed.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (ex: Exception) {
                    Log.w(TAG, "Failed to open battery optimization settings: ${ex.message}")
                    openAppDetailsSettings(context)
                }
            }
        }
    }

    /**
     * Attempts to open manufacturer-specific Auto-start / Autolaunch settings (Xiaomi MIUI/HyperOS, Samsung, Huawei, Oppo, Vivo, OnePlus, Realme).
     */
    fun openOemAutostartSettings(context: Context): Boolean {
        val oemIntents = listOf(
            // Xiaomi MIUI / HyperOS
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")),
            // Huawei / Honor
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
            // Samsung
            Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            Intent().setComponent(ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.battery.BatteryActivity")),
            // Oppo / Realme / ColorOS
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            // Vivo / FuntouchOS / OriginOS
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
            // OnePlus
            Intent().setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListAct")),
            // Asus
            Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity")),
            // Transsion (Infinix / Tecno / Itel)
            Intent().setComponent(ComponentName("com.transsion.phonemanager", "com.transsion.phonemanager.view.AutoRunActivity"))
        )

        val packageManager = context.packageManager
        for (intent in oemIntents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()) {
                try {
                    context.startActivity(intent)
                    Log.i(TAG, "Successfully opened OEM autostart settings with intent: $intent")
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "Error opening OEM autostart intent: ${e.message}")
                }
            }
        }

        // Fallback to app details
        openAppDetailsSettings(context)
        return false
    }

    /**
     * Opens Application Details Settings screen.
     */
    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open application details settings: ${e.message}")
        }
    }
}
