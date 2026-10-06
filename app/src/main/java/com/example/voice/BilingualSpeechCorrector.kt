package com.example.voice

import java.text.Normalizer
import java.util.Locale

/**
 * Intelligent Bilingual (English & Spanish) Speech Corrector and Phonetic Disambiguator.
 *
 * Resolves the issue where Android's SpeechRecognizer transcribes English songs, artists,
 * and commands phonetically as Spanish words (e.g. "kuin" -> "Queen", "col plei" -> "Coldplay",
 * "vili ailis" -> "Billie Eilish", "maicol yacson" -> "Michael Jackson", "plei" -> "play", etc.)
 * or transcribes bilingual mix-ups.
 */
object BilingualSpeechCorrector {

    // Direct mapping of known Spanish phonetic transcriptions to proper English/International artist and band names
    private val PHONETIC_ARTIST_MAP = mapOf(
        "kuin" to "Queen",
        "cuin" to "Queen",
        "quin" to "Queen",
        "kueen" to "Queen",
        "la reina" to "Queen",
        
        "colplei" to "Coldplay",
        "col plei" to "Coldplay",
        "colplay" to "Coldplay",
        "cold play" to "Coldplay",
        "col plai" to "Coldplay",

        "maicol yacson" to "Michael Jackson",
        "maicol yakson" to "Michael Jackson",
        "maikel jackson" to "Michael Jackson",
        "maicol jackson" to "Michael Jackson",
        "miguel jackson" to "Michael Jackson",
        "maikol yacson" to "Michael Jackson",
        "maikol jackson" to "Michael Jackson",

        "vili ailis" to "Billie Eilish",
        "bili eilis" to "Billie Eilish",
        "bili ailish" to "Billie Eilish",
        "billy eilish" to "Billie Eilish",
        "bili ailis" to "Billie Eilish",
        "vili eilish" to "Billie Eilish",
        "bili aylish" to "Billie Eilish",

        "du a lipa" to "Dua Lipa",
        "dua lipa" to "Dua Lipa",
        "tua lipa" to "Dua Lipa",
        "dualipa" to "Dua Lipa",

        "de bitles" to "The Beatles",
        "los bitles" to "The Beatles",
        "the bitles" to "The Beatles",
        "los beatles" to "The Beatles",
        "de beatles" to "The Beatles",

        "rolling eston" to "The Rolling Stones",
        "los rollin" to "The Rolling Stones",
        "roling estons" to "The Rolling Stones",
        "los rolin" to "The Rolling Stones",
        "rolin estons" to "The Rolling Stones",
        "rolling estons" to "The Rolling Stones",
        "the rolin eston" to "The Rolling Stones",

        "gun sand roses" to "Guns N' Roses",
        "gans an roses" to "Guns N' Roses",
        "gans and roses" to "Guns N' Roses",
        "gans roses" to "Guns N' Roses",
        "ganz n roses" to "Guns N' Roses",
        "gan san roses" to "Guns N' Roses",

        "lece pelin" to "Led Zeppelin",
        "le zepelin" to "Led Zeppelin",
        "let zepelin" to "Led Zeppelin",
        "led zepelin" to "Led Zeppelin",
        "ler zepelin" to "Led Zeppelin",
        "led cepelin" to "Led Zeppelin",

        "ac dc" to "AC/DC",
        "asedese" to "AC/DC",
        "asdc" to "AC/DC",
        "ase dese" to "AC/DC",
        "acdc" to "AC/DC",
        "asi disi" to "AC/DC",

        "bon yovi" to "Bon Jovi",
        "von jovi" to "Bon Jovi",
        "bon jobi" to "Bon Jovi",
        "von yovi" to "Bon Jovi",

        "airon meiden" to "Iron Maiden",
        "iron meiden" to "Iron Maiden",
        "airon maiden" to "Iron Maiden",

        "escorpions" to "Scorpions",
        "escorpion" to "Scorpions",

        "aerocmis" to "Aerosmith",
        "erosmis" to "Aerosmith",
        "aerosmit" to "Aerosmith",
        "erosmith" to "Aerosmith",

        "nirbana" to "Nirvana",

        "greenday" to "Green Day",
        "grin dei" to "Green Day",
        "grin day" to "Green Day",
        "green dei" to "Green Day",

        "linquin parc" to "Linkin Park",
        "lin kin park" to "Linkin Park",
        "linkin parc" to "Linkin Park",
        "linquin park" to "Linkin Park",

        "guan direccion" to "One Direction",
        "uan direccion" to "One Direction",
        "wan direccion" to "One Direction",
        "uan direcsion" to "One Direction",

        "avichi" to "Avicii",
        "abichi" to "Avicii",
        "avici" to "Avicii",

        "marun faif" to "Maroon 5",
        "marun faiv" to "Maroon 5",
        "maroon faiv" to "Maroon 5",
        "marun fay" to "Maroon 5",
        "marun fayv" to "Maroon 5",

        "justin biber" to "Justin Bieber",
        "justin viver" to "Justin Bieber",
        "justin baiber" to "Justin Bieber",

        "teilor esuif" to "Taylor Swift",
        "teilor suif" to "Taylor Swift",
        "tailor swift" to "Taylor Swift",
        "taylor suif" to "Taylor Swift",
        "teilor swift" to "Taylor Swift",

        "ed sheran" to "Ed Sheeran",
        "ed shiran" to "Ed Sheeran",
        "ed cheran" to "Ed Sheeran",
        "et shiran" to "Ed Sheeran",

        "bruno mars" to "Bruno Mars",
        "bruno marz" to "Bruno Mars",

        "post malon" to "Post Malone",
        "pos malon" to "Post Malone",
        "post melon" to "Post Malone",

        "de wikend" to "The Weeknd",
        "de guikend" to "The Weeknd",
        "the wikend" to "The Weeknd",
        "el wikend" to "The Weeknd",

        "dreik" to "Drake",

        "bad boni" to "Bad Bunny",
        "bat boni" to "Bad Bunny",
        "bad buni" to "Bad Bunny",

        "calvin garis" to "Calvin Harris",
        "calvin jarris" to "Calvin Harris",
        "calvin jaris" to "Calvin Harris",

        "david geta" to "David Guetta",
        "david gueta" to "David Guetta",
        "david getta" to "David Guetta",

        "marshmelo" to "Marshmello",
        "marhmelo" to "Marshmello",
        "marxmelo" to "Marshmello",

        "alan woker" to "Alan Walker",
        "alan guaker" to "Alan Walker",

        "imayin dragons" to "Imagine Dragons",
        "imayin dragon" to "Imagine Dragons",

        "tuenti uan pilots" to "Twenty One Pilots",
        "tuenti wan pilots" to "Twenty One Pilots",

        "re hot chili pepers" to "Red Hot Chili Peppers",
        "re jot chili pepers" to "Red Hot Chili Peppers",
        "red jot chili peppers" to "Red Hot Chili Peppers",

        "pin floi" to "Pink Floyd",
        "pink floi" to "Pink Floyd",
        "pin floyd" to "Pink Floyd",

        "daft panc" to "Daft Punk",
        "daf pank" to "Daft Punk",

        "shia" to "Sia",

        "gorilas" to "Gorillaz",

        "artic monquis" to "Arctic Monkeys",
        "arctic monquis" to "Arctic Monkeys",
        "artic monkeys" to "Arctic Monkeys",

        "oueisis" to "Oasis",

        "de polis" to "The Police",

        "u dos" to "U2",
        "u two" to "U2",
        "iu tu" to "U2",

        "kanye wes" to "Kanye West",
        "kendrik lamar" to "Kendrick Lamar",
        "kendric lamar" to "Kendrick Lamar",
        "travis escoot" to "Travis Scott",
        "travis escot" to "Travis Scott",
        "keity perri" to "Katy Perry",
        "katy pery" to "Katy Perry",
        "leidy gaga" to "Lady Gaga",
        "riana" to "Rihanna",
        "rihana" to "Rihanna",
        "dadi yanki" to "Daddy Yankee",
        "dady yanqui" to "Daddy Yankee",
        "biza" to "Bizarrap",
        "bzp" to "Bizarrap"
    )

    // Popular bilingual tracks and iconic songs phonetically corrected
    private val PHONETIC_TRACK_MAP = mapOf(
        "bohemian rapsodi" to "Queen - Bohemian Rhapsody",
        "bohemian rapsody" to "Queen - Bohemian Rhapsody",
        "bohemian rapzody" to "Queen - Bohemian Rhapsody",
        "bohemia rapsodi" to "Queen - Bohemian Rhapsody",
        "estar boy" to "The Weeknd - Starboy",
        "star boy" to "The Weeknd - Starboy",
        "hotel california" to "Eagles - Hotel California",
        "shape of iu" to "Ed Sheeran - Shape of You",
        "cheip of iu" to "Ed Sheeran - Shape of You",
        "in di end" to "Linkin Park - In the End",
        "smels laik tin spirit" to "Nirvana - Smells Like Teen Spirit",
        "blinding laits" to "The Weeknd - Blinding Lights",
        "blaindin laits" to "The Weeknd - Blinding Lights",
        "sweet chail of main" to "Guns N' Roses - Sweet Child O' Mine",
        "take on mi" to "A-ha - Take On Me",
        "teik on mi" to "A-ha - Take On Me",
        "don stop bilivin" to "Journey - Don't Stop Believin'",
        "don stop believin" to "Journey - Don't Stop Believin'",
        "sweet caroline" to "Neil Diamond - Sweet Caroline",
        "suil carolain" to "Neil Diamond - Sweet Caroline",
        "viva la vida" to "Coldplay - Viva La Vida",
        "yellow" to "Coldplay - Yellow",
        "ielou" to "Coldplay - Yellow",
        "despacito" to "Luis Fonsi - Despacito",
        "danza kuduro" to "Don Omar - Danza Kuduro"
    )

    // Phonetic command words (e.g. user saying English playback commands in Spanish phonetics)
    private val PHONETIC_COMMAND_WORDS = mapOf(
        "plei" to "play",
        "ple" to "play",
        "estop" to "stop",
        "paus" to "pause",
        "nex" to "next",
        "necst" to "next",
        "privios" to "previous",
        "previos" to "previous",
        "miut" to "mute",
        "desmiut" to "unmute",
        "shafl" to "shuffle",
        "shofl" to "shuffle",
        "repiti" to "repeat",
        "repit" to "repeat"
    )

    // Set of common Spanish words that must NOT be converted to musical entities without strong evidence
    private val COMMON_WORDS = setOf(
        "rosa", "rojo", "verde", "azul", "negro", "blanco", "claro", "oscuro",
        "luna", "sol", "estrella", "mar", "cielo", "tierra", "fuego", "aire", "viento", "lluvia",
        "casa", "vida", "amor", "corazon", "noche", "dia", "tarde", "manana", "tiempo",
        "pan", "vino", "agua", "cafe", "comida", "mesa", "puerta", "camino", "calle",
        "amigo", "hermano", "madre", "padre", "hijo", "hombre", "mujer", "chico", "chica",
        "reina", "rey", "princesa", "principe", "santo", "santa", "dios",
        "musica", "cancion", "canciones", "tema", "pista", "disco", "album", "sonido", "audio",
        "radio", "volumen", "tono", "ritmo", "letra", "voz", "bateria", "guitarra", "bajo",
        "playa", "mundo", "fiesta", "baile", "sueno", "ojos", "mano", "boca", "cuerpo", "alma"
    )

    // Canonical international artists: If already present, no correction needed
    private val CANONICAL_ARTISTS = setOf(
        "queen", "coldplay", "michael jackson", "billie eilish", "dua lipa", "the beatles",
        "the rolling stones", "guns n roses", "guns n' roses", "led zeppelin", "ac/dc", "ac dc",
        "bon jovi", "iron maiden", "scorpions", "aerosmith", "nirvana", "green day",
        "linkin park", "one direction", "avicii", "maroon 5", "justin bieber", "taylor swift",
        "ed sheeran", "bruno mars", "post malone", "the weeknd", "drake", "bad bunny",
        "calvin harris", "david guetta", "marshmello", "alan walker", "imagine dragons",
        "twenty one pilots", "red hot chili peppers", "pink floyd", "daft punk", "sia",
        "gorillaz", "arctic monkeys", "oasis", "the police", "u2", "kanye west", "kendrick lamar",
        "travis scott", "katy perry", "lady gaga", "rihanna", "daddy yankee", "bizarrap",
        "metallica", "eminem", "journey", "eagles"
    )

    // Musical entity trigger prefixes that confirm intent
    private val MUSICAL_PREFIXES = listOf(
        "musica de la banda", "musica del grupo", "musica de", "canciones de", "la cancion de",
        "el tema de", "cancion de", "tema de", "disco de", "el disco de", "album de", "el album de",
        "algo de", "un tema de", "una cancion de", "la banda", "el grupo"
    )

    private val ACTION_VERBS = listOf(
        "ponme", "ponte", "pon la", "pon el", "pon", "pone", "poner",
        "reproduce la", "reproduce el", "reproduce", "reproducir",
        "busca la", "busca el", "busca", "buscar",
        "toca la", "toca el", "toca", "tocar",
        "escucha la", "escucha el", "escucha", "escuchar",
        "play the", "play a", "play", "listen to"
    )

    /**
     * Normalizes text by removing accents, diacritics, punctuation, and extra whitespace.
     */
    fun normalize(input: String): String {
        val trimmed = input.trim().lowercase(Locale.ROOT)
        val nfd = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
        val stripped = nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return stripped.replace(Regex("[¿?¡!.,;:\"'()\\-_]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Contextual, confidence-based speech correction result.
     */
    data class SpeechCorrectionResult(
        val rawText: String,
        val normalizedText: String,
        val correctedText: String,
        val applied: Boolean,
        val reason: String,
        val confidence: Float,
        val matchedEntity: String? = null,
        val entityType: String? = null // "ARTIST", "TRACK", "COMMAND"
    )

    /**
     * Performs conservative, context-aware speech correction.
     * Evaluates phonetic similarity, entity position, musical context, and word length
     * to avoid converting common Spanish words or altering accurate transcriptions.
     */
    fun correctWithConfidence(rawText: String): SpeechCorrectionResult {
        val norm = normalize(rawText)
        if (norm.isBlank()) {
            return SpeechCorrectionResult(
                rawText = rawText,
                normalizedText = "",
                correctedText = rawText,
                applied = false,
                reason = "Blank input",
                confidence = 1.0f
            )
        }

        // 1. Check if input or part of input is ALREADY a canonical artist or track
        for (canonical in CANONICAL_ARTISTS) {
            val padded = " $norm "
            if (padded.contains(" $canonical ")) {
                return SpeechCorrectionResult(
                    rawText = rawText,
                    normalizedText = norm,
                    correctedText = rawText,
                    applied = false,
                    reason = "Already canonical artist/entity '$canonical'",
                    confidence = 1.0f,
                    matchedEntity = canonical,
                    entityType = "ARTIST"
                )
            }
        }

        // 2. Check exact full-phrase track match (e.g. "bohemian rapsodi", "estar boy")
        PHONETIC_TRACK_MAP[norm]?.let { matchedTrack ->
            return SpeechCorrectionResult(
                rawText = rawText,
                normalizedText = norm,
                correctedText = matchedTrack,
                applied = true,
                reason = "Exact phonetic track match '$norm' -> '$matchedTrack'",
                confidence = 0.95f,
                matchedEntity = matchedTrack,
                entityType = "TRACK"
            )
        }

        // 3. Detect musical context (e.g. preceded by "pon musica de", "canciones de", "de", etc.)
        val hasMusicalPrefix = MUSICAL_PREFIXES.any { norm.contains(it) }
        val hasActionVerb = ACTION_VERBS.any { norm.startsWith("$it ") || norm.startsWith(it) }
        val hasConnectingPreposition = norm.contains(" de ") || norm.contains(" by ")

        val isMusicalContext = hasMusicalPrefix || hasActionVerb || hasConnectingPreposition

        // 4. Check for multi-word phonetic track matches inside the utterance
        val sortedTracks = PHONETIC_TRACK_MAP.keys.sortedByDescending { it.length }
        for (pattern in sortedTracks) {
            val padded = " $norm "
            if (padded.contains(" $pattern ")) {
                val replacement = PHONETIC_TRACK_MAP[pattern] ?: continue
                val confidence = if (isMusicalContext) 0.95f else 0.88f
                val corrected = padded.replace(" $pattern ", " $replacement ").trim()
                return SpeechCorrectionResult(
                    rawText = rawText,
                    normalizedText = norm,
                    correctedText = corrected,
                    applied = true,
                    reason = "Phonetic track match '$pattern' -> '$replacement'",
                    confidence = confidence,
                    matchedEntity = replacement,
                    entityType = "TRACK"
                )
            }
        }

        // 5. Check phonetic artist matches (sorted by length to prefer multi-word like 'maicol yacson', 'col plei')
        val sortedArtists = PHONETIC_ARTIST_MAP.keys.sortedByDescending { it.length }
        for (pattern in sortedArtists) {
            val padded = " $norm "
            if (!padded.contains(" $pattern ")) continue

            val replacement = PHONETIC_ARTIST_MAP[pattern] ?: continue

            // Protection against converting common words
            val isCommonWord = COMMON_WORDS.contains(pattern)
            val isMultiWord = pattern.contains(" ")

            // Calculate confidence
            var confidence = if (isMultiWord) 0.90f else 0.82f
            if (isMusicalContext) confidence += 0.08f

            // If the pattern is a common Spanish word (e.g. "reina", "rosa"), heavily penalize unless there is strong musical prefix
            if (isCommonWord) {
                if (!hasMusicalPrefix && !hasConnectingPreposition) {
                    // Do NOT replace common words if not preceded by "musica de", "de", etc.
                    continue
                }
                confidence -= 0.25f
            }

            // Only apply if confidence is high (>= 0.75)
            if (confidence >= 0.75f) {
                val corrected = padded.replace(" $pattern ", " $replacement ").trim()
                return SpeechCorrectionResult(
                    rawText = rawText,
                    normalizedText = norm,
                    correctedText = corrected,
                    applied = true,
                    reason = "Phonetic artist match '$pattern' -> '$replacement' (context=$isMusicalContext)",
                    confidence = confidence,
                    matchedEntity = replacement,
                    entityType = "ARTIST"
                )
            }
        }

        // 6. Check isolated command words (e.g. user just said "plei", "estop", "paus")
        if (PHONETIC_COMMAND_WORDS.containsKey(norm)) {
            val cmd = PHONETIC_COMMAND_WORDS[norm] ?: norm
            return SpeechCorrectionResult(
                rawText = rawText,
                normalizedText = norm,
                correctedText = cmd,
                applied = true,
                reason = "Phonetic command word '$norm' -> '$cmd'",
                confidence = 0.90f,
                matchedEntity = cmd,
                entityType = "COMMAND"
            )
        }

        // No correction needed or confidence too low: preserve input faithfully
        return SpeechCorrectionResult(
            rawText = rawText,
            normalizedText = norm,
            correctedText = rawText,
            applied = false,
            reason = "No high-confidence phonetic pattern matched; preserving transcription",
            confidence = 1.0f
        )
    }

    /**
     * Backward-compatible convenience method.
     */
    fun correctQuery(rawQuery: String): String {
        return correctWithConfidence(rawQuery).correctedText
    }

    /**
     * Scores a candidate speech hypothesis to determine if it contains high-confidence
     * English or Spanish music entities or clear commands.
     */
    fun scoreHypothesis(hypothesis: String): Int {
        var score = 10
        val norm = normalize(hypothesis)

        if (PHONETIC_TRACK_MAP.containsKey(norm)) score += 50
        if (PHONETIC_ARTIST_MAP.containsKey(norm)) score += 40

        for (artist in PHONETIC_ARTIST_MAP.keys) {
            if (norm.contains(artist)) score += 25
        }

        // Presence of clear action verbs adds confidence
        val verbs = listOf("pon", "reproduce", "busca", "play", "listen", "escucha", "pause", "stop", "siguiente", "next")
        for (v in verbs) {
            if (norm.startsWith("$v ")) score += 20
        }

        // Penalty for single-character or nonsensical fragments
        if (norm.length <= 2) score -= 15

        return score
    }

    /**
     * Selects the best candidate hypothesis from a list of SpeechRecognizer matches,
     * applying bilingual corrections and confidence scoring.
     */
    fun pickBestMatch(matches: List<String>): Pair<String, String> {
        if (matches.isEmpty()) return Pair("", "")
        if (matches.size == 1) {
            val original = matches.first()
            val result = correctWithConfidence(original)
            return Pair(original, result.correctedText)
        }

        var bestOriginal = matches.first()
        var bestResult = correctWithConfidence(bestOriginal)
        var highestScore = scoreHypothesis(bestOriginal)

        for (candidate in matches) {
            val result = correctWithConfidence(candidate)
            val score = scoreHypothesis(candidate) + if (result.applied) 15 else 0
            if (score > highestScore) {
                highestScore = score
                bestOriginal = candidate
                bestResult = result
            }
        }

        return Pair(bestOriginal, bestResult.correctedText)
    }
}
