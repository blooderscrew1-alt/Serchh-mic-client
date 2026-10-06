package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.voice.VoiceCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiVoiceResult(
    val command: VoiceCommand,
    val intent: String,
    val songQuery: String,
    val spokenFeedback: String,
    val confidence: Float,
    val rawModelResponse: String? = null
)

data class GeminiChatMessage(
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class GeminiConversationResult(
    val replyText: String,
    val suggestedCommand: VoiceCommand? = null,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

object GeminiVoiceService {
    private const val TAG = "GeminiVoiceService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    // Optimized HTTP Client with Connection Pooling & Fast Timeouts for Voice AI
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(4, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private var isPrewarmed = false

    /**
     * Pre-warms HTTP/2 connection & DNS resolution to Gemini API endpoint
     * to eliminate initial TLS handshake latency when user speaks.
     */
    fun prewarm(context: android.content.Context? = null) {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") return

        Thread {
            try {
                // Quick DNS/TLS connection handshake
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/")
                    .head()
                    .build()
                httpClient.newCall(request).execute().close()
                isPrewarmed = true
                Log.d(TAG, "Gemini connection pre-warmed successfully")
            } catch (e: Exception) {
                // Non-blocking prewarm
            }
        }.start()
    }

    fun getApiKey(context: android.content.Context? = null): String {
        // 1. Check custom user-entered key in settings if available
        try {
            if (context != null) {
                val custom = com.example.data.ClientSettings(context).customGeminiApiKey
                if (custom.isNotBlank() && custom != "MY_GEMINI_API_KEY") {
                    return custom
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        // 2. Check BuildConfig key
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun isConfigured(context: android.content.Context? = null): Boolean {
        val key = getApiKey(context)
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    private fun resolveModel(modelName: String): String {
        return when (modelName.trim()) {
            "gemini-3.1-flash-lite-preview", "Lite 3.1" -> "gemini-3.1-flash-lite-preview"
            "gemini-3.5-flash", "Flash 3.5" -> "gemini-3.5-flash"
            "gemini-3.1-pro-preview", "Pro 3.1" -> "gemini-3.1-pro-preview"
            "gemini-flash-latest", "Flash Latest" -> "gemini-flash-latest"
            else -> "gemini-3.1-flash-lite-preview" // Default to fastest preview model for instant voice responsiveness
        }
    }

    /**
     * Diagnostic tool to test Gemini API Key and connectivity in real-time.
     */
    suspend fun testConnection(
        explicitKey: String? = null,
        modelName: String = "gemini-3.1-flash-lite-preview",
        context: android.content.Context? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val key = explicitKey?.trim()?.ifBlank { null } ?: getApiKey(context)
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            return@withContext Pair(
                false,
                "API Key no configurada. Ingresa tu API Key de Google AI Studio (empieza con AIza...)."
            )
        }

        val primaryModel = resolveModel(modelName)
        val modelsToTry = listOf(primaryModel, "gemini-3.1-flash-lite-preview", "gemini-3.5-flash", "gemini-flash-latest").distinct()

        var lastError = ""
        for (model in modelsToTry) {
            try {
                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", "Responde solo 'OK'.") })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("maxOutputTokens", 5)
                        put("temperature", 0.0)
                    })
                }

                val endpoint = "$BASE_URL$model:generateContent?key=$key"
                val body = requestJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(endpoint).post(body).build()

                val startTime = System.currentTimeMillis()
                val response = httpClient.newCall(request).execute()
                val duration = System.currentTimeMillis() - startTime

                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val root = JSONObject(responseBody)
                    val reply = root.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text", "Conectado")
                        ?.trim() ?: "Conectado"
                    return@withContext Pair(true, "¡Conexión ultra rápida con Gemini! ($model • ${duration}ms)\nRespuesta: \"$reply\"")
                } else {
                    val errCode = response.code
                    val errBody = response.body?.string() ?: ""
                    val errorMsg = try {
                        JSONObject(errBody).optJSONObject("error")?.optString("message") ?: "Error HTTP $errCode"
                    } catch (e: Exception) {
                        "Error HTTP $errCode"
                    }
                    lastError = "Error ($model): $errorMsg (Código $errCode)"
                    if (errCode != 404) {
                        return@withContext Pair(false, "Fallo de autenticación: $errorMsg (Verifica tu clave)")
                    }
                }
            } catch (e: Exception) {
                lastError = "Error de red: ${e.localizedMessage ?: "No se pudo conectar"}"
            }
        }

        return@withContext Pair(false, lastError)
    }

    /**
     * Ultra-fast voice command & natural language interpretation using Gemini AI.
     * Fixes speech recognition errors, phonetics, natural phrasing, and extracts clean music intent.
     */
    suspend fun enhanceVoiceCommand(
        spokenText: String,
        configuredWakeWord: String = "Música",
        modelName: String = "gemini-3.1-flash-lite-preview",
        context: android.content.Context? = null
    ): GeminiVoiceResult? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || spokenText.isBlank()) {
            return@withContext null
        }

        val primaryModel = resolveModel(modelName)
        val modelsToTry = listOf(primaryModel, "gemini-3.1-flash-lite-preview", "gemini-3.5-flash", "gemini-flash-latest").distinct()

        val systemPrompt = """
            You are a lightning-fast bilingual (Spanish & English) voice command parser and music search enhancer for a remote music player.
            Input is spoken text captured by ASR, which may be in Spanish, English, or Spanglish.
            
            CRITICAL RULES:
            1. PRESERVE PROPER NAMES: Strictly preserve artists, bands, song titles, and albums heard in the audio. NEVER invent, hallucinate, or replace a specific artist/song with an unrelated one.
            2. DISTINGUISH HEARD TEXT FROM INTERPRETATION: If the user says a concrete artist or song (e.g. 'Queen', 'Metallica', 'Michael Jackson', 'Nothing Else Matters'), return EXACTLY that name in 'songQuery'. Never convert an assumption into a supposedly literal transcription. If you are uncertain about an entity, preserve the original heard words.
            3. PHONETIC CORRECTION: Only correct clear phonetic transliterations when confidence is very high (e.g. 'kuin' -> 'Queen', 'col plei' -> 'Coldplay', 'maicol yacson' -> 'Michael Jackson'). Do NOT change words that already spell real artists or common words.
            4. CONVERSATIONAL & MOOD REQUESTS: Only interpret conceptually when the user explicitly asks for recommendations, moods, eras, or categories (e.g. 'algo parecido a rock de los 80', 'música tranquila para estudiar').
            5. CLEAN QUERIES: Strip wake words ('$configuredWakeWord', 'oye', 'hey'), prefixes ('pon', 'reproduce', 'busca', 'play', 'put on', 'listen to').
            6. MEDIA CONTROLS: Recognize PAUSE, RESUME, NEXT_TRACK, PREV_TRACK, STOP, VOLUME_UP, VOLUME_DOWN, VOLUME_SET (0-15), MUTE, REPEAT_TRACK.
            
            Strict JSON format:
            {"intent":"SEARCH_AND_PLAY","songQuery":"Artist - Title","volumeLevel":null,"spokenFeedback":"Reproduciendo Artist - Title","confidence":0.99}
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", spokenText) })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.0)
                put("maxOutputTokens", 90)
                put("responseMimeType", "application/json")
            })
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())

        for (effectiveModel in modelsToTry) {
            try {
                val endpoint = "$BASE_URL$effectiveModel:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(endpoint)
                    .post(body)
                    .build()

                val startTime = System.currentTimeMillis()
                val response = httpClient.newCall(request).execute()
                val elapsed = System.currentTimeMillis() - startTime

                if (!response.isSuccessful) {
                    Log.w(TAG, "Gemini API error HTTP ${response.code} on model $effectiveModel: ${response.message}")
                    if (response.code == 404) continue
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: continue
                val root = JSONObject(responseBody)
                val candidates = root.optJSONArray("candidates") ?: continue
                if (candidates.length() == 0) continue

                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content") ?: continue
                val parts = content.optJSONArray("parts") ?: continue
                if (parts.length() == 0) continue

                val rawText = parts.getJSONObject(0).optString("text", "")
                if (rawText.isBlank()) continue

                val parsedJson = JSONObject(rawText.trim().removeSurrounding("```json", "```").trim())
                val intent = parsedJson.optString("intent", "UNKNOWN").uppercase()
                val songQuery = parsedJson.optString("songQuery", "").trim()
                val volumeLevel = if (parsedJson.has("volumeLevel") && !parsedJson.isNull("volumeLevel")) {
                    parsedJson.optInt("volumeLevel")
                } else null
                val spokenFeedback = parsedJson.optString("spokenFeedback", "")
                val confidence = parsedJson.optDouble("confidence", 0.95).toFloat()

                val command: VoiceCommand = when (intent) {
                    "SEARCH_AND_PLAY" -> {
                        if (songQuery.isNotBlank()) VoiceCommand.SearchAndPlay(songQuery) else VoiceCommand.None
                    }
                    "VOLUME_SET" -> {
                        if (volumeLevel != null) {
                            val lvl = if (volumeLevel > 15) ((volumeLevel / 100f) * 15f).toInt().coerceIn(0, 15) else volumeLevel.coerceIn(0, 15)
                            VoiceCommand.SetVolume(lvl)
                        } else VoiceCommand.None
                    }
                    "VOLUME_UP" -> VoiceCommand.VolumeUp
                    "VOLUME_DOWN" -> VoiceCommand.VolumeDown
                    "VOLUME_MAX" -> VoiceCommand.VolumeMax
                    "VOLUME_MEDIUM" -> VoiceCommand.VolumeMedium
                    "VOLUME_LOW" -> VoiceCommand.VolumeLow
                    "MUTE" -> VoiceCommand.Mute
                    "PAUSE" -> VoiceCommand.Pause
                    "RESUME" -> VoiceCommand.Resume
                    "NEXT_TRACK" -> VoiceCommand.NextTrack
                    "PREV_TRACK" -> VoiceCommand.PreviousTrack
                    "REPEAT_TRACK" -> VoiceCommand.RepeatTrack
                    else -> VoiceCommand.None
                }

                Log.d(TAG, "Gemini responded in ${elapsed}ms with intent=$intent query='$songQuery'")

                return@withContext GeminiVoiceResult(
                    command = command,
                    intent = intent,
                    songQuery = songQuery,
                    spokenFeedback = spokenFeedback,
                    confidence = confidence,
                    rawModelResponse = rawText
                )
            } catch (e: Exception) {
                Log.e(TAG, "Gemini enhancement error on $effectiveModel", e)
            }
        }

        return@withContext null
    }

    /**
     * Conducts a real-time conversational exchange with Gemini AI.
     */
    suspend fun chatWithGemini(
        userMessage: String,
        history: List<GeminiChatMessage> = emptyList(),
        modelName: String = "gemini-3.5-flash",
        context: android.content.Context? = null
    ): GeminiConversationResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiConversationResult(
                replyText = "⚠️ Clave de Gemini no configurada.\n\nIngresa tu API Key de Google AI Studio en Ajustes (o en el panel Secrets de AI Studio) para activar las respuestas de IA.",
                isSuccess = false,
                errorMessage = "GEMINI_API_KEY no configurada"
            )
        }

        val primaryModel = resolveModel(modelName)
        val modelsToTry = listOf(primaryModel, "gemini-3.5-flash", "gemini-flash-latest").distinct()

        val systemPrompt = """
            Eres un asistente de voz y música inteligente integrado en una app de micrófono remoto.
            Tu propósito es ayudar al usuario con recomendaciones musicales, información de artistas, letras, canciones, listas de reproducción y comandos de control de música.
            Responde de forma concisa, cálida, natural y directa (máximo 2 a 3 oraciones cortas), ideal para ser leída por un sintetizador de voz (TTS).
            Si el usuario te pide que pongas o busques una canción, recomiéndala con entusiasmo e indica el artista y título claramente.
        """.trimIndent()

        val contentsArray = JSONArray()

        // Include last turns of history (up to 6 messages)
        val recentHistory = history.takeLast(6)
        for (msg in recentHistory) {
            contentsArray.put(JSONObject().apply {
                put("role", if (msg.role == "user") "user" else "model")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", msg.text)
                    })
                })
            })
        }

        // Current user turn
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", userMessage)
                })
            })
        })

        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemPrompt)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 250)
            })
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        var lastHttpError: Pair<Int, String>? = null

        for (effectiveModel in modelsToTry) {
            try {
                val endpoint = "$BASE_URL$effectiveModel:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(endpoint)
                    .post(body)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val code = response.code
                    val errBody = response.body?.string() ?: ""
                    val parsedMsg = try {
                        JSONObject(errBody).optJSONObject("error")?.optString("message") ?: response.message
                    } catch (e: Exception) {
                        response.message
                    }
                    lastHttpError = Pair(code, parsedMsg)
                    if (code == 404) {
                        // Try next fallback model
                        continue
                    } else if (code == 400 || code == 403) {
                        return@withContext GeminiConversationResult(
                            replyText = "⚠️ Clave de Gemini rechazada ($code): $parsedMsg.\n\nRevisa que tu API Key sea correcta en Ajustes.",
                            isSuccess = false,
                            errorMessage = "HTTP $code: $parsedMsg"
                        )
                    }
                    return@withContext GeminiConversationResult(
                        replyText = "No pude conectar con Gemini en este momento ($code): $parsedMsg.",
                        isSuccess = false,
                        errorMessage = "HTTP $code: $parsedMsg"
                    )
                }

                val responseBody = response.body?.string() ?: ""
                val root = JSONObject(responseBody)
                val candidates = root.optJSONArray("candidates")
                val candidate = candidates?.optJSONObject(0)
                val content = candidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val replyText = parts?.optJSONObject(0)?.optString("text", "")?.trim() ?: ""

                if (replyText.isBlank()) {
                    return@withContext GeminiConversationResult(
                        replyText = "No obtuve respuesta de texto de Gemini.",
                        isSuccess = false
                    )
                }

                // Check if response contains a music play suggestion
                var suggestedCommand: VoiceCommand? = null
                if (userMessage.contains("pon", ignoreCase = true) ||
                    userMessage.contains("reproduce", ignoreCase = true) ||
                    userMessage.contains("busca", ignoreCase = true)
                ) {
                    val cleaned = userMessage
                        .replace(Regex("^(pon|reproduce|busca|quiero escuchar|toca)\\s+", RegexOption.IGNORE_CASE), "")
                        .trim()
                    if (cleaned.isNotBlank()) {
                        suggestedCommand = VoiceCommand.SearchAndPlay(cleaned)
                    }
                }

                return@withContext GeminiConversationResult(
                    replyText = replyText,
                    suggestedCommand = suggestedCommand,
                    isSuccess = true
                )
            } catch (e: Exception) {
                Log.e(TAG, "Gemini chat attempt error on $effectiveModel", e)
            }
        }

        val errMsg = lastHttpError?.let { "HTTP ${it.first}: ${it.second}" } ?: "Error de red al conectar con Gemini."
        return@withContext GeminiConversationResult(
            replyText = "No pude conectar con los servidores de Gemini AI ($errMsg).",
            isSuccess = false,
            errorMessage = errMsg
        )
    }
}
