package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.ClientSettings

class RebootAlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SCHEDULED_REBOOT = "com.example.ACTION_SCHEDULED_REBOOT"
        private const val TAG = "RebootAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "RebootAlarmReceiver triggered with action: ${intent.action}")
        val settings = ClientSettings(context)
        if (settings.dailyRebootEnabled) {
            ScheduledRebootManager.performRebootOrMaintenance(context)
            // Re-schedule for next day
            ScheduledRebootManager.scheduleDailyReboot(context)
        }
    }
}
