package com.example.voice

import java.util.regex.Pattern

/**
 * Data class representing a parsed music command extracted via Regular Expressions.
 */
data class MusicParsedCommand(
    val action: String,             // E.g., "reproducir", "poner", "play", "pausar", "volumen"
    val searchQuery: String,        // Extracted song, artist, album, or query string
    val artist: String? = null,     // Extracted artist if "de <artista>" or "by <artist>" pattern is present
    val track: String? = null,      // Extracted track title
    val album: String? = null,      // Extracted album title
    val genre: String? = null,      // Extracted genre
    val isMediaControl: Boolean = false,
    val controlType: MediaControlType = MediaControlType.SEARCH_AND_PLAY,
    val intent: String = "SEARCH_AND_PLAY",
    val confidence: Float = 1.0f,
    val rawText: String
)

enum class MediaControlType {
    SEARCH_AND_PLAY,
    PLAY_RESUME,
    PAUSE,
    STOP,
    NEXT_TRACK,
    PREVIOUS_TRACK,
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_SET,
    MUTE,
    REPEAT,
    UNKNOWN
}

/**
 * High-performance regex parser for offline multilingual (Spanish / English / Spanglish) music commands.
 */
object MusicCommandParser {

    // Regex pattern to strip wake words from start of utterance
    private val WAKE_WORD_STRIP_REGEX = Pattern.compile(
        "^\\s*(?:oye\\s+m[uú]sica|hey\\s+player|hey\\s+music|oye\\s+reproductor|m[uú]sica|player|oye|hey|ok\\s+player|ok\\s+m[uú]sica)\\s*[,.:;]?\\s*",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    // Regex for playback search & play intent
    private val PLAY_ACTION_REGEX = Pattern.compile(
        "^\\s*(?:por\\s+favor\\s+)?(?:quiero\\s+escuchar|me\\s+gustar[ií]a\\s+escuchar|ponme|ponte|pon|reproduce|reproducir|toca|tocar|play|coloca|colocar|busca|buscar|escuchar|puedes\\s+poner|pon la canci[oó]n|pon la de|reproduce la de|play the song)\\s+(.+)$",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    // Regex for "Song by Artist" or "Song de Artist" extraction
    private val SONG_ARTIST_SPLIT_REGEX = Pattern.compile(
        "^\\s*(?:la\\s+canci[oó]n\\s+|el\\s+tema\\s+)?(.+?)\\s+(?:del?\\s+|by\\s+|de\\s+la\\s+banda\\s+|de\\s+los\\s+)(.+)$",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    // Regex patterns for media controls
    private val PAUSE_REGEX = Pattern.compile(
        "^\\s*(?:pausa|pausar|pause|det[eé]n\\s+la\\s+m[uú]sica|para\\s+la\\s+m[uú]sica|stop|para|detente)\\s*$",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    private val RESUME_REGEX = Pattern.compile(
        "^\\s*(?:contin[uú]a|reanuda|reanudar|play|resume|sigue\\s+tocando|sigue\\s+reproduciendo|despausar)\\s*$",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    private val NEXT_REGEX = Pattern.compile(
        "^\\s*(?:siguiente|siguiente\\s+canci[oó]n|siguiente\\s+pista|next|next\\s+track|next\\s+song|pasa\\s+de\\s+canci[oó]n|otra\\s+canci[oó]n|salta)\\s*$",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    private val PREVIOUS_REGEX = Pattern.compile(
        "^\\s*(?:anterior|anterior\\s+canci[oó]n|canci[oó]n\\s+anterior|previous|previous\\s+track|atr[aá]s|vuelve\\s+a\\s+la\\s+anterior)\\s*$",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    private val VOLUME_UP_REGEX = Pattern.compile(
        "^\\s*(?:sube\\s+el?\\s+volumen|m[aá]s\\s+volumen|aumenta\\s+el?\\s+volumen|subir\\s+volumen|volume\\s+up|louder)\\s*$",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    private val VOLUME_DOWN_REGEX = Pattern.compile(
        "^\\s*(?:baja\\s+el?\\s+volumen|menos\\s+volumen|disminuye\\s+el?\\s+volumen|bajar\\s+volumen|volume\\s+down|quieter)\\s*$",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    private val VOLUME_SET_REGEX = Pattern.compile(
        "^\\s*(?:volumen\\s+al?\\s+|pon\\s+el?\\s+volumen\\s+al?\\s+|set\\s+volume\\s+to\\s+)(\\d{1,3})(?:\\s*%)?\\s*$",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    private val MUTE_REGEX = Pattern.compile(
        "^\\s*(?:silencio|silenciar|mute|mutear|quitar\\s+sonido|apaga\\s+el\\s+volumen)\\s*$",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    /**
     * Parses spoken text into a structured MusicParsedCommand.
     *
     * @param text Raw spoken text from ASR (e.g. "Oye Música, pon Blinding Lights de The Weeknd")
     * @return Extracted MusicParsedCommand with action, search target, and metadata.
     */
    fun parse(text: String): MusicParsedCommand {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return MusicParsedCommand(
                action = "none",
                searchQuery = "",
                isMediaControl = false,
                controlType = MediaControlType.UNKNOWN,
                rawText = text
            )
        }

        // 1. Strip wake word if present at the beginning
        val stripped = stripWakeWord(trimmed)

        // 2. Check for direct media control commands
        when {
            PAUSE_REGEX.matcher(stripped).matches() -> {
                return MusicParsedCommand(
                    action = "pausar",
                    searchQuery = "",
                    isMediaControl = true,
                    controlType = MediaControlType.PAUSE,
                    intent = "PAUSE",
                    confidence = 1.0f,
                    rawText = text
                )
            }
            RESUME_REGEX.matcher(stripped).matches() -> {
                return MusicParsedCommand(
                    action = "reanudar",
                    searchQuery = "",
                    isMediaControl = true,
                    controlType = MediaControlType.PLAY_RESUME,
                    intent = "RESUME",
                    confidence = 1.0f,
                    rawText = text
                )
            }
            NEXT_REGEX.matcher(stripped).matches() -> {
                return MusicParsedCommand(
                    action = "siguiente",
                    searchQuery = "",
                    isMediaControl = true,
                    controlType = MediaControlType.NEXT_TRACK,
                    intent = "NEXT_TRACK",
                    confidence = 1.0f,
                    rawText = text
                )
            }
            PREVIOUS_REGEX.matcher(stripped).matches() -> {
                return MusicParsedCommand(
                    action = "anterior",
                    searchQuery = "",
                    isMediaControl = true,
                    controlType = MediaControlType.PREVIOUS_TRACK,
                    intent = "PREVIOUS_TRACK",
                    confidence = 1.0f,
                    rawText = text
                )
            }
            VOLUME_UP_REGEX.matcher(stripped).matches() -> {
                return MusicParsedCommand(
                    action = "subir_volumen",
                    searchQuery = "",
                    isMediaControl = true,
                    controlType = MediaControlType.VOLUME_UP,
                    intent = "VOLUME_UP",
                    confidence = 1.0f,
                    rawText = text
                )
            }
            VOLUME_DOWN_REGEX.matcher(stripped).matches() -> {
                return MusicParsedCommand(
                    action = "bajar_volumen",
                    searchQuery = "",
                    isMediaControl = true,
                    controlType = MediaControlType.VOLUME_DOWN,
                    intent = "VOLUME_DOWN",
                    confidence = 1.0f,
                    rawText = text
                )
            }
            MUTE_REGEX.matcher(stripped).matches() -> {
                return MusicParsedCommand(
                    action = "silenciar",
                    searchQuery = "",
                    isMediaControl = true,
                    controlType = MediaControlType.MUTE,
                    intent = "MUTE",
                    confidence = 1.0f,
                    rawText = text
                )
            }
        }

        // Check for specific volume percentage
        val volMatcher = VOLUME_SET_REGEX.matcher(stripped)
        if (volMatcher.matches()) {
            val level = volMatcher.group(1) ?: "50"
            return MusicParsedCommand(
                action = "ajustar_volumen",
                searchQuery = level,
                isMediaControl = true,
                controlType = MediaControlType.VOLUME_SET,
                intent = "VOLUME_SET",
                confidence = 1.0f,
                rawText = text
            )
        }

        // 3. Check for Search & Play commands
        val playMatcher = PLAY_ACTION_REGEX.matcher(stripped)
        if (playMatcher.matches()) {
            val fullQuery = playMatcher.group(1)?.trim() ?: ""
            val (track, artist, album, genre) = extractEntities(fullQuery)

            val normalizedAction = when {
                stripped.startsWith("play", ignoreCase = true) -> "play"
                stripped.startsWith("reproduc", ignoreCase = true) -> "reproducir"
                stripped.startsWith("busca", ignoreCase = true) -> "buscar"
                stripped.startsWith("toca", ignoreCase = true) -> "tocar"
                else -> "poner"
            }

            return MusicParsedCommand(
                action = normalizedAction,
                searchQuery = cleanSearchQuery(fullQuery),
                artist = artist,
                track = track,
                album = album,
                genre = genre,
                isMediaControl = false,
                controlType = MediaControlType.SEARCH_AND_PLAY,
                intent = "SEARCH_AND_PLAY",
                confidence = 0.95f,
                rawText = text
            )
        }

        // 4. Fallback: If no explicit verb was used but user named a song/artist directly after wake word
        val (track, artist, album, genre) = extractEntities(stripped)
        return MusicParsedCommand(
            action = "reproducir",
            searchQuery = cleanSearchQuery(stripped),
            artist = artist,
            track = track,
            album = album,
            genre = genre,
            isMediaControl = false,
            controlType = MediaControlType.SEARCH_AND_PLAY,
            intent = "SEARCH_AND_PLAY",
            confidence = 0.90f,
            rawText = text
        )
    }

    /**
     * Determines whether a voice request is complex, ambiguous, or conversational
     * and therefore benefits from Gemini AI natural language interpretation.
     */
    fun isComplexOrConversational(text: String): Boolean {
        val lower = text.lowercase(java.util.Locale.ROOT).trim()
        val conversationalMarkers = listOf(
            "parecido a", "parecida a", "algo parecido", "algo similar", "algo como",
            "tranquil", "relajan", "para dormir", "para estudiar", "para entrenar", "para correr", "para cocinar",
            "de los ochenta", "de los noventa", "de los setenta", "de los 80", "de los 90", "de los 70", "de los 2000",
            "recomiendame", "recomiéndame", "que me recomiendas", "qué me recomiendas",
            "sorprendeme", "sorpréndeme", "lo que estabamos escuchando", "lo que estábamos escuchando",
            "lo que sonaba", "canciones de esa epoca", "canciones de esa época", "música alegre", "musica alegre",
            "música triste", "musica triste"
        )
        return conversationalMarkers.any { lower.contains(it) }
    }

    /**
     * Strips leading wake words from transcription.
     */
    fun stripWakeWord(text: String): String {
        val matcher = WAKE_WORD_STRIP_REGEX.matcher(text)
        return if (matcher.find()) {
            matcher.replaceFirst("").trim()
        } else {
            text.trim()
        }
    }

    /**
     * Extracts music entities: track, artist, album, genre.
     */
    private fun extractEntities(query: String): Quadruple<String?, String?, String?, String?> {
        val clean = query.trim()

        // 1. Check album: "álbum <album> de <artist>" or "disco <disco> de <artist>"
        val albumMatcher = Pattern.compile(
            "^(?:el\\s+)?(?:[aá]lbum|disco)\\s+(.+?)\\s+(?:del?\\s+|by\\s+|de\\s+la\\s+banda\\s+)(.+)$",
            Pattern.CASE_INSENSITIVE
        ).matcher(clean)
        if (albumMatcher.matches()) {
            val album = albumMatcher.group(1)?.trim()
            val artist = albumMatcher.group(2)?.trim()
            return Quadruple(null, artist, album, null)
        }

        // 2. Check genre: "rock de los 80", "música clásica", "jazz", etc.
        val genreMatcher = Pattern.compile(
            "^(?:m[uú]sica\\s+)?(rock(?:\\s+de\\s+los\\s+\\d{2,4})?|pop|jazz|metal|salsa|reggaeton|clasica|electr[oó]nica|baladas(?:\\s+en\\s+espa[nñ]ol)?|blues)\\s*$",
            Pattern.CASE_INSENSITIVE
        ).matcher(clean)
        if (genreMatcher.matches()) {
            val genre = genreMatcher.group(1)?.trim()
            return Quadruple(null, null, null, genre)
        }

        // 3. Check "Song Title de Artist" or "Song Title by Artist"
        val splitMatcher = SONG_ARTIST_SPLIT_REGEX.matcher(clean)
        if (splitMatcher.matches()) {
            val rawTrack = splitMatcher.group(1)?.trim()
            val rawArtist = splitMatcher.group(2)?.trim()
            return Quadruple(rawTrack, rawArtist, null, null)
        }

        // 4. Check "música de Artist" or "canciones de Artist"
        val artistOnlyMatcher = Pattern.compile(
            "^(?:m[uú]sica\\s+de|canciones\\s+de|temas\\s+de|algo\\s+de|la\\s+banda|el\\s+grupo)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE
        ).matcher(clean)
        if (artistOnlyMatcher.matches()) {
            val artist = artistOnlyMatcher.group(1)?.trim()
            return Quadruple(null, artist, null, null)
        }

        return Quadruple(null, null, null, null)
    }

    /**
     * Attempts to split "Song Title de Artist" or "Song Title by Artist".
     */
    private fun extractSongAndArtist(query: String): Pair<String?, String?> {
        val (track, artist, _, _) = extractEntities(query)
        return Pair(track, artist)
    }

    /**
     * Cleans noise words and leading articles.
     */
    private fun cleanSearchQuery(query: String): String {
        return query
            .replace(Regex("^(?:m[uú]sica\\s+de|canciones\\s+de|la\\s+canci[oó]n\\s+de|algo\\s+de)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(?:la\\s+canci[oó]n|el\\s+tema|la\\s+rola)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
