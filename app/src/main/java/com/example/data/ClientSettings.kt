package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build

class ClientSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("serch_mic_client_prefs", Context.MODE_PRIVATE)

    var hostIp: String
        get() = prefs.getString(KEY_HOST_IP, "") ?: ""
        set(value) = prefs.edit().putString(KEY_HOST_IP, value.trim()).apply()

    var hostPort: Int
        get() = prefs.getInt(KEY_HOST_PORT, 8998)
        set(value) = prefs.edit().putInt(KEY_HOST_PORT, value.coerceIn(1024, 65535)).apply()

    var satelliteRoom: String
        get() = prefs.getString(KEY_SATELLITE_ROOM, "serchtube-master") ?: "serchtube-master"
        set(value) = prefs.edit().putString(KEY_SATELLITE_ROOM, value.trim()).apply()

    var deviceAlias: String
        get() = prefs.getString(KEY_DEVICE_ALIAS, Build.MODEL?.take(20) ?: "Mic Remoto") ?: "Mic Remoto"
        set(value) = prefs.edit().putString(KEY_DEVICE_ALIAS, value.trim()).apply()

    var wakeWord: String
        get() = prefs.getString(KEY_WAKE_WORD, "Música") ?: "Música"
        set(value) = prefs.edit().putString(KEY_WAKE_WORD, value.trim()).apply()

    var continuousListening: Boolean
        get() = prefs.getBoolean(KEY_CONTINUOUS_LISTENING, true)
        set(value) = prefs.edit().putBoolean(KEY_CONTINUOUS_LISTENING, value).apply()

    var autoStartListeningOnLaunch: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START_LISTENING, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START_LISTENING, value).apply()

    var directCommandsEnabled: Boolean
        get() = prefs.getBoolean(KEY_DIRECT_COMMANDS, false)
        set(value) = prefs.edit().putBoolean(KEY_DIRECT_COMMANDS, value).apply()

    var wakeWordTriggersHostMicDirectly: Boolean
        get() = prefs.getBoolean(KEY_WAKE_TRIGGERS_HOST_MIC, false)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_TRIGGERS_HOST_MIC, value).apply()

    var wakeWindowSeconds: Int
        get() = prefs.getInt(KEY_WAKE_WINDOW_SECONDS, 8)
        set(value) = prefs.edit().putInt(KEY_WAKE_WINDOW_SECONDS, value.coerceIn(3, 20)).apply()

    var autoConnectOnLaunch: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT, value).apply()

    var muteRecognizerBeeps: Boolean
        get() = prefs.getBoolean(KEY_MUTE_BEEPS, true)
        set(value) = prefs.edit().putBoolean(KEY_MUTE_BEEPS, value).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()

    var keepQueueOpenDuringPlayback: Boolean
        get() = prefs.getBoolean(KEY_KEEP_QUEUE_OPEN_DURING_PLAYBACK, false)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_QUEUE_OPEN_DURING_PLAYBACK, value).apply()

    var hapticFeedback: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()

    var autoRefreshEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_REFRESH_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_REFRESH_ENABLED, value).apply()

    var autoRefreshMinutes: Int
        get() = prefs.getInt(KEY_AUTO_REFRESH_MINUTES, 15)
        set(value) = prefs.edit().putInt(KEY_AUTO_REFRESH_MINUTES, value.coerceIn(1, 180)).apply()

    var nodeMeshSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_NODE_MESH_SYNC, true)
        set(value) = prefs.edit().putBoolean(KEY_NODE_MESH_SYNC, value).apply()

    var nightModeScheduleEnabled: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_MODE_SCHEDULE, false)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT_MODE_SCHEDULE, value).apply()

    var nightStartHour: Int
        get() = prefs.getInt(KEY_NIGHT_START_HOUR, 22)
        set(value) = prefs.edit().putInt(KEY_NIGHT_START_HOUR, value.coerceIn(0, 23)).apply()

    var nightStartMinute: Int
        get() = prefs.getInt(KEY_NIGHT_START_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_NIGHT_START_MINUTE, value.coerceIn(0, 59)).apply()

    var nightEndHour: Int
        get() = prefs.getInt(KEY_NIGHT_END_HOUR, 7)
        set(value) = prefs.edit().putInt(KEY_NIGHT_END_HOUR, value.coerceIn(0, 23)).apply()

    var nightEndMinute: Int
        get() = prefs.getInt(KEY_NIGHT_END_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_NIGHT_END_MINUTE, value.coerceIn(0, 59)).apply()

    var nightShowFullScreenClock: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_FULL_SCREEN_CLOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT_FULL_SCREEN_CLOCK, value).apply()

    var nightScreenOffComplete: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_SCREEN_OFF_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT_SCREEN_OFF_COMPLETE, value).apply()

    var nightTapToWake: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_TAP_TO_WAKE, true)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT_TAP_TO_WAKE, value).apply()

    var nightVoiceWake: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_VOICE_WAKE, true)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT_VOICE_WAKE, value).apply()

    var nightDimLevel: Float
        get() = prefs.getFloat(KEY_NIGHT_DIM_LEVEL, 0.05f)
        set(value) = prefs.edit().putFloat(KEY_NIGHT_DIM_LEVEL, value.coerceIn(0.00f, 0.6f)).apply()

    var nightClockBurnInProtection: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_BURN_IN_PROTECT, true)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT_BURN_IN_PROTECT, value).apply()

    var geminiAiVoiceEnhanceEnabled: Boolean
        get() = prefs.getBoolean(KEY_GEMINI_AI_VOICE_ENHANCE, true)
        set(value) = prefs.edit().putBoolean(KEY_GEMINI_AI_VOICE_ENHANCE, value).apply()

    var geminiLiveVoiceConversationEnabled: Boolean
        get() = prefs.getBoolean(KEY_GEMINI_LIVE_CONVERSATION, true)
        set(value) = prefs.edit().putBoolean(KEY_GEMINI_LIVE_CONVERSATION, value).apply()

    var geminiModelName: String
        get() = prefs.getString(KEY_GEMINI_MODEL, "gemini-3.1-flash-lite-preview") ?: "gemini-3.1-flash-lite-preview"
        set(value) = prefs.edit().putString(KEY_GEMINI_MODEL, value.trim()).apply()

    var geminiVoiceTtsEnabled: Boolean
        get() = prefs.getBoolean(KEY_GEMINI_VOICE_TTS, true)
        set(value) = prefs.edit().putBoolean(KEY_GEMINI_VOICE_TTS, value).apply()

    var customGeminiApiKey: String
        get() = prefs.getString(KEY_CUSTOM_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_GEMINI_API_KEY, value.trim()).apply()

    // Voice Recognition & Language Settings
    var speechLanguageMode: String
        get() = prefs.getString(KEY_SPEECH_LANGUAGE_MODE, "bilingual") ?: "bilingual"
        set(value) = prefs.edit().putString(KEY_SPEECH_LANGUAGE_MODE, value.trim().lowercase()).apply()

    var bilingualPhoneticFixEnabled: Boolean
        get() = prefs.getBoolean(KEY_BILINGUAL_PHONETIC_FIX, true)
        set(value) = prefs.edit().putBoolean(KEY_BILINGUAL_PHONETIC_FIX, value).apply()

    // Boot & Scheduled Maintenance Settings
    var startOnBootEnabled: Boolean
        get() = prefs.getBoolean(KEY_START_ON_BOOT, true)
        set(value) = prefs.edit().putBoolean(KEY_START_ON_BOOT, value).apply()

    var dailyRebootEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_REBOOT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_REBOOT_ENABLED, value).apply()

    var dailyRebootHour: Int
        get() = prefs.getInt(KEY_DAILY_REBOOT_HOUR, 4)
        set(value) = prefs.edit().putInt(KEY_DAILY_REBOOT_HOUR, value.coerceIn(0, 23)).apply()

    var dailyRebootMinute: Int
        get() = prefs.getInt(KEY_DAILY_REBOOT_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_DAILY_REBOOT_MINUTE, value.coerceIn(0, 59)).apply()

    fun isCurrentlyInNightWindow(): Boolean {
        if (!nightModeScheduleEnabled) return false
        val now = java.util.Calendar.getInstance()
        val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
        val startMinutes = nightStartHour * 60 + nightStartMinute
        val endMinutes = nightEndHour * 60 + nightEndMinute

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            // Overnight window (e.g. 22:00 to 07:00)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    companion object {
        const val DEFAULT_HOST_PORT_PRIMARY = 8998
        const val DEFAULT_HOST_PORT_SECONDARY = 3000
        val DEFAULT_HOST_PORTS = listOf(8998, 3000)

        private const val KEY_HOST_IP = "host_ip"
        private const val KEY_HOST_PORT = "host_port"
        private const val KEY_SATELLITE_ROOM = "satellite_room"
        private const val KEY_DEVICE_ALIAS = "device_alias"
        private const val KEY_WAKE_WORD = "wake_word"
        private const val KEY_CONTINUOUS_LISTENING = "continuous_listening"
        private const val KEY_AUTO_START_LISTENING = "auto_start_listening"
        private const val KEY_DIRECT_COMMANDS = "direct_commands"
        private const val KEY_WAKE_TRIGGERS_HOST_MIC = "wake_triggers_host_mic_directly"
        private const val KEY_WAKE_WINDOW_SECONDS = "wake_window_seconds"
        private const val KEY_AUTO_CONNECT = "auto_connect"
        private const val KEY_MUTE_BEEPS = "mute_beeps"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_KEEP_QUEUE_OPEN_DURING_PLAYBACK = "keep_queue_open_during_playback"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val KEY_AUTO_REFRESH_ENABLED = "auto_refresh_enabled"
        private const val KEY_AUTO_REFRESH_MINUTES = "auto_refresh_minutes"
        private const val KEY_NODE_MESH_SYNC = "node_mesh_sync"
        private const val KEY_NIGHT_MODE_SCHEDULE = "night_mode_schedule"
        private const val KEY_NIGHT_START_HOUR = "night_start_hour"
        private const val KEY_NIGHT_START_MINUTE = "night_start_minute"
        private const val KEY_NIGHT_END_HOUR = "night_end_hour"
        private const val KEY_NIGHT_END_MINUTE = "night_end_minute"
        private const val KEY_NIGHT_FULL_SCREEN_CLOCK = "night_full_screen_clock"
        private const val KEY_NIGHT_SCREEN_OFF_COMPLETE = "night_screen_off_complete"
        private const val KEY_NIGHT_TAP_TO_WAKE = "night_tap_to_wake"
        private const val KEY_NIGHT_VOICE_WAKE = "night_voice_wake"
        private const val KEY_NIGHT_DIM_LEVEL = "night_dim_level"
        private const val KEY_NIGHT_BURN_IN_PROTECT = "night_burn_in_protect"
        private const val KEY_GEMINI_AI_VOICE_ENHANCE = "gemini_ai_voice_enhance"
        private const val KEY_GEMINI_LIVE_CONVERSATION = "gemini_live_conversation"
        private const val KEY_GEMINI_MODEL = "gemini_model"
        private const val KEY_GEMINI_VOICE_TTS = "gemini_voice_tts"
        private const val KEY_CUSTOM_GEMINI_API_KEY = "custom_gemini_api_key"
        private const val KEY_SPEECH_LANGUAGE_MODE = "speech_language_mode"
        private const val KEY_BILINGUAL_PHONETIC_FIX = "bilingual_phonetic_fix"
        private const val KEY_START_ON_BOOT = "start_on_boot"
        private const val KEY_DAILY_REBOOT_ENABLED = "daily_reboot_enabled"
        private const val KEY_DAILY_REBOOT_HOUR = "daily_reboot_hour"
        private const val KEY_DAILY_REBOOT_MINUTE = "daily_reboot_minute"
    }
}
