package com.example.network

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONObject

data class ScannedQrConfig(
    val wsUrl: String,
    val room: String = "serchtube-master",
    val role: String = "satellite",
    val nodeName: String? = null
)

object QrCodePairingHelper {
    private const val TAG = "QrCodePairingHelper"

    /**
     * Parses scanned QR payload which can be a URL or a JSON structure.
     * Examples:
     * - URL: https://[URL_SERVIDOR]/?room=[SALA_ID]&role=satellite&nodeName=Android
     * - JSON: {"wsUrl": "wss://[URL_SERVIDOR]/ws", "room": "serchtube-master", "nodeName": "Android Satélite"}
     */
    fun parseQrCodePayload(raw: String): ScannedQrConfig? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null

        // 1. Try parsing JSON structure
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return try {
                val json = JSONObject(trimmed)
                val rawWsUrl = json.optString("wsUrl", json.optString("url", json.optString("host", "")))
                val room = json.optString("room", json.optString("roomCode", json.optString("roomId", "serchtube-master")))
                val role = json.optString("role", "satellite")
                val nodeName = json.optString("nodeName", null)

                if (rawWsUrl.isNotBlank()) {
                    val formattedUrl = RemoteNodeClient.buildWebSocketUrl(rawWsUrl)
                    ScannedQrConfig(
                        wsUrl = formattedUrl,
                        room = if (room.isNotBlank()) room else "serchtube-master",
                        role = if (role.isNotBlank()) role else "satellite",
                        nodeName = if (!nodeName.isNullOrBlank()) nodeName else null
                    )
                } else null
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse JSON QR payload: ${e.message}")
                null
            }
        }

        // 2. Try parsing URL with parameters
        try {
            val uri = Uri.parse(trimmed)
            val roomParam = uri.getQueryParameter("room")
                ?: uri.getQueryParameter("roomCode")
                ?: uri.getQueryParameter("roomId")
                ?: "serchtube-master"
            val roleParam = uri.getQueryParameter("role") ?: "satellite"
            val nodeNameParam = uri.getQueryParameter("nodeName")

            val explicitWsUrl = uri.getQueryParameter("wsUrl")
            if (!explicitWsUrl.isNullOrBlank()) {
                val formatted = RemoteNodeClient.buildWebSocketUrl(explicitWsUrl)
                return ScannedQrConfig(
                    wsUrl = formatted,
                    room = roomParam,
                    role = roleParam,
                    nodeName = nodeNameParam
                )
            }

            var scheme = uri.scheme?.lowercase() ?: ""
            var host = uri.host ?: ""
            var port = uri.port

            if (host.isNotBlank()) {
                val isDomain = host.contains(".run.app") || host.contains(".com") ||
                               host.contains(".net") || host.contains(".io") ||
                               host.contains(".ngrok") || host.contains(".trycloudflare")

                val wsScheme = when (scheme) {
                    "https", "wss" -> "wss"
                    "http", "ws" -> "ws"
                    else -> if (isDomain) "wss" else "ws"
                }

                val portPart = if (port != -1) ":$port" else ""
                var path = uri.path ?: ""
                if (path.isBlank() || path == "/") {
                    path = "/ws"
                } else if (!path.endsWith("/ws") && !path.contains("/ws/")) {
                    path = if (path.endsWith("/")) "${path}ws" else "$path/ws"
                }

                val constructedWsUrl = "$wsScheme://$host$portPart$path"
                return ScannedQrConfig(
                    wsUrl = constructedWsUrl,
                    room = roomParam,
                    role = roleParam,
                    nodeName = nodeNameParam
                )
            } else {
                // Raw string fallback
                val formattedFallback = RemoteNodeClient.buildWebSocketUrl(trimmed)
                if (formattedFallback.isNotBlank()) {
                    return ScannedQrConfig(
                        wsUrl = formattedFallback,
                        room = roomParam,
                        role = roleParam,
                        nodeName = nodeNameParam
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse URL QR payload: ${e.message}")
            val fallbackUrl = RemoteNodeClient.buildWebSocketUrl(trimmed)
            if (fallbackUrl.isNotBlank()) {
                return ScannedQrConfig(
                    wsUrl = fallbackUrl,
                    room = "serchtube-master",
                    role = "satellite"
                )
            }
        }

        return null
    }

    /**
     * Short haptic vibration feedback on successful QR code scan.
     */
    fun triggerScanHapticFeedback(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(120)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    /**
     * Returns default ScanOptions for ZXing Embedded QR Scanner.
     */
    fun createScanOptions(): ScanOptions {
        return ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Apunta con la cámara al código QR de la PC para vincular")
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(false)
        }
    }
}
