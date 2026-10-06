package com.example.network

import org.json.JSONObject
import kotlin.math.roundToInt

enum class RemoteMessageType {
    PING,
    PONG,
    VOICE_COMMAND,
    COMMAND_ACK,
    DEVICE_REGISTER,
    NODE_SIGNAL_RECEIVING,
    NODE_SIGNAL_IDLE,
    NODE_SYNC_STATE,
    PEER_HEARTBEAT,
    HOST_STATUS_UPDATE,
    NODE_WAKE_CLAIM,
    NODE_WAKE_CANCEL,
    NODE_WAKE_RELEASE
}

data class SatelliteDevice(
    val id: String, // Unique IP or identifier
    val name: String, // User alias e.g. "Micrófono Dormitorio", "Teléfono Cocina"
    val ip: String,
    val firstConnected: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val lastCommand: String = "",
    val commandsCount: Int = 0,
    val isOnline: Boolean = true,
    val batteryPercent: Int = -1
)

data class RemoteNodeMessage(
    val type: RemoteMessageType,
    val senderName: String,
    val commandType: String = "", // e.g. SEARCH_PLAY, PLAY, PAUSE, NEXT, PREV, VOLUME_UP, VOLUME_DOWN, VOLUME_SET
    val songQuery: String = "",
    val rawSpokenText: String = "",
    val message: String = "",
    val success: Boolean = true,
    val volume: Int? = null,
    val isMuted: Boolean? = null,
    val currentSong: String = "",
    val playbackState: String = "",
    val activeNodesCount: Int = 0,
    val activeNodesJson: String = "",
    val senderNodeId: String = "",
    val originNodeAlias: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val audioEnergy: Float = 0f,
    val claimId: String = "",
    val batteryPercent: Int = -1,
    val coverUrl: String = "",
    val upcomingJson: String = "",
    val playlistJson: String = ""
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("type", type.name)
        json.put("senderName", senderName)
        json.put("commandType", commandType)
        json.put("songQuery", songQuery)
        json.put("rawSpokenText", rawSpokenText)
        json.put("message", message)
        json.put("success", success)
        if (volume != null) {
            json.put("volume", volume)
            json.put("volumeLevel", volume)
            json.put("hostVolume", volume)
        }
        if (isMuted != null) {
            json.put("isMuted", isMuted)
            json.put("muted", isMuted)
        }
        if (currentSong.isNotBlank()) {
            json.put("currentSong", currentSong)
        }
        if (playbackState.isNotBlank()) {
            json.put("playbackState", playbackState)
        }
        if (activeNodesCount > 0) {
            json.put("activeNodesCount", activeNodesCount)
        }
        if (activeNodesJson.isNotBlank()) {
            json.put("activeNodesJson", activeNodesJson)
        }
        if (senderNodeId.isNotBlank()) {
            json.put("senderNodeId", senderNodeId)
        }
        if (originNodeAlias.isNotBlank()) {
            json.put("originNodeAlias", originNodeAlias)
        }
        if (audioEnergy > 0f) {
            json.put("audioEnergy", audioEnergy.toDouble())
        }
        if (claimId.isNotBlank()) {
            json.put("claimId", claimId)
        }
        if (batteryPercent >= 0) {
            json.put("batteryPercent", batteryPercent)
        }
        if (coverUrl.isNotBlank()) {
            json.put("coverUrl", coverUrl)
        }
        if (upcomingJson.isNotBlank()) {
            json.put("upcomingJson", upcomingJson)
        }
        if (playlistJson.isNotBlank()) {
            json.put("playlistJson", playlistJson)
        }
        json.put("timestamp", timestamp)
        return json.toString()
    }

    companion object {
        fun parseVolumeValue(raw: Any?): Int? {
            if (raw == null || raw == JSONObject.NULL) return null
            when (raw) {
                is Number -> {
                    val d = raw.toDouble()
                    return when {
                        d < 0.0 -> 0
                        d > 0.0 && d <= 1.0 -> (d * 15.0).roundToInt().coerceIn(1, 15)
                        d > 15.0 -> ((d / 100.0) * 15.0).roundToInt().coerceIn(0, 15)
                        else -> d.roundToInt().coerceIn(0, 15)
                    }
                }
                is String -> {
                    val str = raw.trim()
                    if (str.isBlank() || str.equals("null", ignoreCase = true)) return null
                    // Fraction format e.g. "8/15" or "8 / 15"
                    val fractionMatch = Regex("""(\d{1,3})\s*/\s*(\d{1,3})""").find(str)
                    if (fractionMatch != null) {
                        val num = fractionMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                        val den = fractionMatch.groupValues[2].toDoubleOrNull() ?: 15.0
                        if (den > 0) {
                            return ((num / den) * 15.0).roundToInt().coerceIn(0, 15)
                        }
                    }
                    // Percentage format e.g. "80%"
                    val pctMatch = Regex("""(\d{1,3}(?:\.\d+)?)\s*%""").find(str)
                    if (pctMatch != null) {
                        val pct = pctMatch.groupValues[1].toDoubleOrNull() ?: return null
                        return ((pct / 100.0) * 15.0).roundToInt().coerceIn(0, 15)
                    }
                    // Plain numeric string
                    val numMatch = Regex("""\b(\d+(?:\.\d+)?)\b""").find(str)
                    if (numMatch != null) {
                        val d = numMatch.groupValues[1].toDoubleOrNull() ?: return null
                        return when {
                            d < 0.0 -> 0
                            d > 0.0 && d <= 1.0 -> (d * 15.0).roundToInt().coerceIn(1, 15)
                            d > 15.0 -> ((d / 100.0) * 15.0).roundToInt().coerceIn(0, 15)
                            else -> d.roundToInt().coerceIn(0, 15)
                        }
                    }
                }
            }
            return null
        }

        fun extractVolumeFromJson(json: JSONObject): Int? {
            val possibleKeys = listOf(
                "volume", "volumeLevel", "volume_level", "hostVolume", "host_volume",
                "currentVolume", "current_volume", "vol", "volumen", "level",
                "masterVolume", "master_volume", "soundVolume", "sound_level",
                "volumePercent", "volume_percent", "volumePct", "audioVolume", "audio_volume",
                "val", "value"
            )
            for (key in possibleKeys) {
                if (json.has(key) && !json.isNull(key)) {
                    val rawVal = json.opt(key)
                    val parsed = parseVolumeValue(rawVal)
                    if (parsed != null) return parsed
                }
            }
            val nestedKeys = listOf("data", "state", "status", "payload", "result", "host", "device", "player")
            for (nestedKey in nestedKeys) {
                if (json.has(nestedKey) && !json.isNull(nestedKey)) {
                    val nestedObj = json.optJSONObject(nestedKey)
                    if (nestedObj != null) {
                        val nestedVol = extractVolumeFromJson(nestedObj)
                        if (nestedVol != null) return nestedVol
                    }
                }
            }
            return null
        }

        fun extractMuteFromJson(json: JSONObject): Boolean? {
            val muteKeys = listOf("isMuted", "is_muted", "muted", "mute", "silenciado", "isSilenciado", "silencio")
            for (key in muteKeys) {
                if (json.has(key) && !json.isNull(key)) {
                    val v = json.opt(key)
                    if (v is Boolean) return v
                    if (v is Number) return v.toInt() == 1
                    if (v is String) {
                        val s = v.trim().lowercase()
                        if (s == "true" || s == "1" || s == "yes" || s == "si" || s == "muted" || s == "silenciado") return true
                        if (s == "false" || s == "0" || s == "no" || s == "unmuted" || s == "activo") return false
                    }
                }
            }
            val nestedKeys = listOf("data", "state", "status", "payload", "result", "host", "device", "player")
            for (nestedKey in nestedKeys) {
                if (json.has(nestedKey) && !json.isNull(nestedKey)) {
                    val nestedObj = json.optJSONObject(nestedKey)
                    if (nestedObj != null) {
                        val nestedMute = extractMuteFromJson(nestedObj)
                        if (nestedMute != null) return nestedMute
                    }
                }
            }
            return null
        }

        fun extractVolumeFromText(text: String): Int? {
            if (text.isBlank()) return null
            // Check fraction like "8/15" or "8 de 15"
            val fractionMatch = Regex("""(?i)(?:volumen|volume|vol|nivel|level)?\s*[:=]?\s*(\d{1,2})\s*(?:/|de)\s*15""").find(text)
            if (fractionMatch != null) {
                val num = fractionMatch.groupValues[1].toIntOrNull()
                if (num != null) return num.coerceIn(0, 15)
            }
            // Check percentage like "80%"
            val pctMatch = Regex("""(?i)(?:volumen|volume|vol|al|en)?\s*[:=]?\s*(\d{1,3})\s*%""").find(text)
            if (pctMatch != null) {
                val pct = pctMatch.groupValues[1].toDoubleOrNull()
                if (pct != null) return ((pct / 100.0) * 15.0).roundToInt().coerceIn(0, 15)
            }
            // Check volume level phrases
            val generalMatch = Regex("""(?i)(?:volumen|volume|vol|nivel|level)\s*(?:actual|es|al|en|set to|cambiado a|ajustado a|establecido en|a|de)?\s*(?:nivel|level)?\s*[:=]?\s*(\d{1,2})""").find(text)
            if (generalMatch != null) {
                val num = generalMatch.groupValues[1].toIntOrNull()
                if (num != null) {
                    return if (num > 15) ((num / 100.0) * 15.0).roundToInt().coerceIn(0, 15) else num.coerceIn(0, 15)
                }
            }
            return null
        }

        fun fromJson(raw: String): RemoteNodeMessage? {
            return try {
                val json = JSONObject(raw)
                val parsedVol = extractVolumeFromJson(json)
                val parsedMute = extractMuteFromJson(json)
                val rawType = json.optString("type", RemoteMessageType.PING.name)
                val msgType = try {
                    RemoteMessageType.valueOf(rawType)
                } catch (_: Exception) {
                    RemoteMessageType.PING
                }

                val cmdType = json.optString("commandType", "")
                val songQ = json.optString("songQuery", "")
                val spokenTxt = json.optString("rawSpokenText", "")
                val msgTxt = json.optString("message", "")

                // If volume was not directly in JSON keys, infer from commandType or message
                val finalVol = parsedVol ?: run {
                    if (cmdType.startsWith("VOLUME_SET_")) {
                        cmdType.removePrefix("VOLUME_SET_").toIntOrNull()?.coerceIn(0, 15)
                    } else if (cmdType == "VOLUME_SET" && songQ.isNotBlank()) {
                        songQ.toIntOrNull()?.coerceIn(0, 15)
                    } else {
                        extractVolumeFromText(msgTxt) ?: extractVolumeFromText(spokenTxt)
                    }
                }

                val finalMute = parsedMute ?: run {
                    if (cmdType == "MUTE") true
                    else if (cmdType == "UNMUTE") false
                    else if (msgTxt.contains("silenciado", ignoreCase = true) || msgTxt.contains("muteado", ignoreCase = true)) true
                    else if (msgTxt.contains("desilenciado", ignoreCase = true) || msgTxt.contains("desmuteado", ignoreCase = true)) false
                    else null
                }

                val coverVal = when {
                    json.has("coverUrl") && !json.isNull("coverUrl") -> json.optString("coverUrl")
                    json.has("cover_url") && !json.isNull("cover_url") -> json.optString("cover_url")
                    json.has("cover") && !json.isNull("cover") -> json.optString("cover")
                    json.has("thumbnail") && !json.isNull("thumbnail") -> json.optString("thumbnail")
                    json.has("thumbnailUrl") && !json.isNull("thumbnailUrl") -> json.optString("thumbnailUrl")
                    json.has("artwork") && !json.isNull("artwork") -> json.optString("artwork")
                    json.has("artworkUrl") && !json.isNull("artworkUrl") -> json.optString("artworkUrl")
                    json.has("art") && !json.isNull("art") -> json.optString("art")
                    json.has("image") && !json.isNull("image") -> json.optString("image")
                    json.has("img") && !json.isNull("img") -> json.optString("img")
                    json.has("poster") && !json.isNull("poster") -> json.optString("poster")
                    else -> ""
                }

                val upcomingVal = when {
                    json.has("upcomingJson") && !json.isNull("upcomingJson") -> json.optString("upcomingJson")
                    json.optJSONArray("queue") != null -> json.optJSONArray("queue")?.toString() ?: ""
                    json.optJSONArray("upcoming") != null -> json.optJSONArray("upcoming")?.toString() ?: ""
                    json.optJSONArray("tracks") != null -> json.optJSONArray("tracks")?.toString() ?: ""
                    json.optJSONArray("upcomingTracks") != null -> json.optJSONArray("upcomingTracks")?.toString() ?: ""
                    json.optJSONArray("songs") != null -> json.optJSONArray("songs")?.toString() ?: ""
                    json.optJSONObject("queue") != null -> json.optJSONObject("queue")?.toString() ?: ""
                    json.optJSONObject("upcoming") != null -> json.optJSONObject("upcoming")?.toString() ?: ""
                    else -> json.optString("upcoming", json.optString("queue", json.optString("tracks", "")))
                }

                val playlistVal = when {
                    json.has("playlistJson") && !json.isNull("playlistJson") -> json.optString("playlistJson")
                    json.optJSONArray("playlist") != null -> json.optJSONArray("playlist")?.toString() ?: ""
                    json.optJSONArray("playlists") != null -> json.optJSONArray("playlists")?.toString() ?: ""
                    json.optJSONArray("playlistTracks") != null -> json.optJSONArray("playlistTracks")?.toString() ?: ""
                    json.optJSONObject("playlist") != null -> json.optJSONObject("playlist")?.toString() ?: ""
                    else -> json.optString("playlist", json.optString("playlists", ""))
                }

                val songVal = when {
                    json.has("currentSong") && !json.isNull("currentSong") -> json.optString("currentSong")
                    json.has("song") && !json.isNull("song") -> json.optString("song")
                    json.has("title") && !json.isNull("title") -> json.optString("title")
                    json.has("track") && !json.isNull("track") -> json.optString("track")
                    else -> ""
                }

                RemoteNodeMessage(
                    type = msgType,
                    senderName = json.optString("senderName", "Desconocido"),
                    commandType = cmdType,
                    songQuery = songQ,
                    rawSpokenText = spokenTxt,
                    message = msgTxt,
                    success = json.optBoolean("success", true),
                    volume = finalVol,
                    isMuted = finalMute,
                    currentSong = songVal,
                    playbackState = json.optString("playbackState", ""),
                    activeNodesCount = json.optInt("activeNodesCount", 0),
                    activeNodesJson = json.optString("activeNodesJson", ""),
                    senderNodeId = json.optString("senderNodeId", ""),
                    originNodeAlias = json.optString("originNodeAlias", ""),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                    audioEnergy = json.optDouble("audioEnergy", 0.0).toFloat(),
                    claimId = json.optString("claimId", ""),
                    batteryPercent = json.optInt("batteryPercent", -1),
                    coverUrl = coverVal,
                    upcomingJson = upcomingVal,
                    playlistJson = playlistVal
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
