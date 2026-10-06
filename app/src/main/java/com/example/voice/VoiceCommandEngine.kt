package com.example.voice

import java.text.Normalizer
import java.util.Locale

sealed class VoiceCommand {
    data class SearchAndPlay(val songQuery: String) : VoiceCommand()
    data class SetVolume(val level: Int) : VoiceCommand()
    object VolumeUp : VoiceCommand()
    object VolumeDown : VoiceCommand()
    object VolumeMedium : VoiceCommand()
    object VolumeLow : VoiceCommand()
    object VolumeHigh : VoiceCommand()
    object VolumeMax : VoiceCommand()
    object Mute : VoiceCommand()
    object Pause : VoiceCommand()
    object Stop : VoiceCommand()
    object Resume : VoiceCommand()
    object NextTrack : VoiceCommand()
    object PreviousTrack : VoiceCommand()
    object RepeatTrack : VoiceCommand()
    data class WakeWordOnly(val remainder: String) : VoiceCommand()
    object None : VoiceCommand()
}

fun VoiceCommand.toIntentName(): String = when (this) {
    is VoiceCommand.SearchAndPlay -> "SEARCH_AND_PLAY"
    VoiceCommand.Pause -> "PAUSE"
    VoiceCommand.Stop -> "STOP"
    VoiceCommand.Resume -> "RESUME"
    VoiceCommand.NextTrack -> "NEXT_TRACK"
    VoiceCommand.PreviousTrack -> "PREVIOUS_TRACK"
    VoiceCommand.RepeatTrack -> "REPEAT_TRACK"
    VoiceCommand.VolumeUp -> "VOLUME_UP"
    VoiceCommand.VolumeDown -> "VOLUME_DOWN"
    VoiceCommand.VolumeMedium -> "VOLUME_MEDIUM"
    VoiceCommand.VolumeLow -> "VOLUME_LOW"
    VoiceCommand.VolumeHigh -> "VOLUME_HIGH"
    VoiceCommand.VolumeMax -> "VOLUME_MAX"
    is VoiceCommand.SetVolume -> "VOLUME_SET"
    VoiceCommand.Mute -> "MUTE"
    is VoiceCommand.WakeWordOnly -> "WAKE_WORD_ONLY"
    VoiceCommand.None -> "UNKNOWN"
}

data class ParseResult(
    val command: VoiceCommand,
    val matchedMasterWord: Boolean,
    val rawText: String,
    val description: String,
    val intent: String = command.toIntentName(),
    val artist: String? = null,
    val track: String? = null,
    val searchQuery: String? = null,
    val isClearLocalCommand: Boolean = true
)

object VoiceCommandEngine {

    private val STOP_SYNONYMS = listOf(
        "detente", "deténte", "dentente", "deten", "detén", "detener", "detenlo", "detenla", "detener la musica", "deten la musica", "deten la cancion",
        "deten el tema", "detener musica", "detener cancion", "parar", "para", "para la musica", "para la cancion",
        "para el tema", "parar la musica", "paralo", "parala", "parate", "párate", "alto", "stop", "stop music", "stop the music", "quieto", "estop",
        "apaga la musica", "apagar la musica", "apagar musica", "apaga musica", "quitar la musica", "quitar musica",
        "para de reproducir", "parar cancion", "callate", "cállate", "silencio total", "para todo", "deten todo", "para ya", "detente ya",
        "turn off", "turn off music", "shut down", "halt", "end playback"
    )

    private val PAUSE_SYNONYMS = listOf(
        "pausa", "pausar", "pon pausa", "ponle pausa", "metele pausa", "dale pausa", "pausalo", "pausala", "pausate", "páusate", "paus",
        "pausar la musica", "pausar musica", "pausar cancion", "pausa la musica", "pausa la cancion", "pausa el tema", "pausar el tema",
        "pon en pausa", "poner en pausa", "deja en pausa", "quedate en pausa", "haz una pausa", "pausar reproduccion", "pausa la reproduccion",
        "pause", "pause music", "pause song", "pause playback", "hold on", "freeze"
    )

    private val RESUME_SYNONYMS = listOf(
        "continuar", "continua", "continúa", "continua la musica", "continúa la musica", "continua la cancion", "continúa la cancion", "continua el tema", "continúa el tema",
        "continuar musica", "continuar cancion", "reanudar", "reanuda", "reanuda la musica", "reanuda la cancion", "reanuda el tema",
        "reanudar musica", "reanudar cancion", "play", "plei", "dale play", "dale al play", "pon play", "reproducir",
        "reproduce", "reproducir la musica", "reproduce la musica", "seguir", "sigue", "sigue con la musica",
        "sigue la musica", "sigue tocando", "despausar", "despausa", "despausalo", "despausala", "resume", "iniciar", "inicia",
        "iniciar musica", "dale musica", "ponla", "ponlo", "dale", "reanudalo", "reanudala", "continualo",
        "continuala", "despausar musica", "seguir reproduciendo", "dale marcha", "sigue reproduciendo", "reproducir ahora", "ponla a sonar", "ponlo a sonar", "continua reproduciendo", "reanudar reproduccion",
        "resume music", "resume song", "resume playback", "unpause", "start playing", "keep playing"
    )

    private val NEXT_SYNONYMS = listOf(
        "siguiente", "siguiente cancion", "siguiente tema", "siguiente pista", "siguiente rola", "siguiente video", "pasa", "pasala", "pasalo",
        "pasa la cancion", "pasa el tema", "pasa de cancion", "pasa de tema", "pasa cancion", "pasa tema", "pasa a la siguiente", "pasa a la siguiente cancion",
        "cambia", "cambiala", "cambialo", "cambia cancion", "cambia de cancion", "cambia la cancion", "cambiar cancion", "cambiar de cancion",
        "cambia el tema", "cambiar el tema", "cambia de pista", "cambiar pista", "otra", "otra cancion", "otro tema", "pon otra", "pon otro",
        "pon otra cancion", "pon otro tema", "avanzar", "avanza", "avanza la cancion", "avanzar cancion", "adelantar", "adelanta",
        "adelanta la cancion", "adelantar cancion", "salta", "saltar", "salta la cancion", "saltar cancion", "salta esta cancion",
        "saltar esta cancion", "salta de cancion", "salta a la siguiente", "next", "nex", "necst", "next song", "next track", "proxima", "proxima cancion",
        "proximo tema", "proxima pista", "pasar video", "cambiar video", "dale siguiente", "pon siguiente", "pasa pista", "pasar a la siguiente",
        "cambia de tema", "saltar de tema", "pasar cancion", "siguiente pieza",
        "skip", "skip song", "skip track", "skip this", "forward", "forward track"
    )

    private val PREVIOUS_SYNONYMS = listOf(
        "anterior", "anterior cancion", "anterior tema", "anterior pista", "cancion anterior", "tema anterior", "pista anterior",
        "la anterior", "el anterior", "pon la anterior", "pon el anterior", "atras", "atrás", "atras la cancion", "atrás la cancion",
        "retrocede", "retroceder", "retroceder cancion", "retrocede la cancion", "volver", "vuelve", "cancion previa", "tema previo",
        "pista previa", "previous", "privios", "previos", "previous song", "previous track", "cancion de antes", "la de antes", "el de antes", "tema de antes",
        "regresa", "regresar", "regresa a la anterior", "vuelve a la anterior", "video anterior", "cancion pasada", "la pasada", "el pasado",
        "dale anterior", "pon anterior", "retroceder tema", "volver a la anterior", "retroceder pista",
        "prev", "prev song", "go back", "back", "backtrack", "replay previous"
    )

    private val REPEAT_SYNONYMS = listOf(
        "repetir", "repite", "repite la cancion", "repite el tema", "repite la pista", "repetir cancion",
        "repetir tema", "otra vez", "de nuevo", "reproducir de nuevo", "desde el principio", "desde el inicio",
        "reiniciar cancion", "reinicia la cancion", "reiniciar tema", "bucle", "loop", "repeat", "repiti", "repit",
        "ponla de nuevo", "ponlo de nuevo", "ponla otra vez", "ponlo otra vez", "repite de nuevo", "repetir de nuevo",
        "repeat song", "repeat this song", "replay song", "replay this", "loop song", "from the beginning"
    )

    private val VOLUME_UP_SYNONYMS = listOf(
        "sube el volumen", "sube volumen", "subir el volumen", "subir volumen", "subele al volumen", "subele el volumen",
        "subirle al volumen", "subirle el volumen", "subele", "subelo", "subela", "subirle", "subir", "sube",
        "mas volumen", "mas alto", "mas fuerte", "mas sonido", "mas audio", "mas musica", "arriba volumen", "volumen arriba",
        "arriba el volumen", "aumenta el volumen", "aumenta volumen", "aumentar el volumen", "aumentar volumen",
        "aumenta el sonido", "aumentar el sonido", "aumenta sonido", "aumentar sonido", "aumenta el audio", "aumentar audio",
        "sube la musica", "subir la musica", "sube musica", "subir musica", "subele a la musica", "subirle a la musica",
        "sube la cancion", "subir la cancion", "subele a la cancion", "subirle a la cancion",
        "subir sonido", "sube sonido", "sube el sonido", "subele al sonido", "volume up", "volumen ap", "louder", "turn up", "turn it up", "crank it up",
        "increase volume", "raise volume", "more volume",
        "sube un poco", "subele un poco", "subir un poco", "sube un poco mas", "subele un poco mas", "sube mas", "sube mas volumen",
        "sube mas el volumen", "un poco mas de volumen", "un poco mas de sonido", "dale mas volumen", "ponle mas volumen",
        "pon mas volumen", "ponle mas sonido", "dale volumen", "sube audio", "subir audio",
        "muy bajito", "no se escucha", "no se oye", "esta muy bajo", "esta muy bajito", "casi no se escucha", "casi no se oye"
    )

    private val VOLUME_DOWN_SYNONYMS = listOf(
        "baja el volumen", "baja volumen", "bajar el volumen", "bajar volumen", "bajale al volumen", "bajale el volumen",
        "bajarle al volumen", "bajarle el volumen", "bajale", "bajalo", "bajala", "bajarle", "bajar", "baja",
        "menos volumen", "mas bajo", "mas despacio", "menos fuerte", "menos sonido", "menos audio", "menos musica",
        "abajo volumen", "volumen abajo", "abajo el volumen", "disminuye el volumen", "disminuye volumen",
        "disminuir el volumen", "disminuir volumen", "disminuye el sonido", "disminuir el sonido", "disminuye sonido",
        "disminuir sonido", "disminuye el audio", "disminuir audio",
        "baja la musica", "bajar la musica", "baja musica", "bajar musica", "bajale a la musica", "bajarle a la musica",
        "baja la cancion", "bajar la cancion", "bajale a la cancion", "bajarle a la cancion",
        "bajar sonido", "baja sonido", "baja el sonido", "bajale al sonido", "volume down", "volumen daun", "quieter", "turn down", "turn it down",
        "decrease volume", "lower volume", "less volume",
        "baja un poco", "bajale un poco", "bajar un poco", "baja un poco mas", "bajale un poco mas", "baja mas", "baja mas volumen",
        "baja mas el volumen", "un poco menos de volumen", "un poco menos de sonido", "dale menos volumen", "ponle menos volumen",
        "pon menos volumen", "ponle menos sonido", "quita volumen", "quitale volumen", "baja audio", "bajar audio",
        "muy alto", "muy fuerte", "esta muy alto", "esta muy fuerte", "esta muy duro", "suena muy fuerte", "suena muy alto"
    )

    private val VOLUME_MEDIUM_SYNONYMS = listOf(
        "volumen medio", "volumen a la mitad", "volumen al 50", "volumen al cincuenta", "volumen 50", "volumen regular",
        "volumen normal", "volumen intermedio", "a la mitad", "mitad de volumen", "medio volumen", "pon volumen medio",
        "pon el volumen medio", "pon volumen a la mitad", "ponle volumen medio", "dale volumen medio",
        "volumen al 50 por ciento", "volumen 50 por ciento", "cincuenta de volumen", "medium volume", "half volume", "mid volume"
    )

    private val VOLUME_LOW_SYNONYMS = listOf(
        "volumen bajo", "volumen bajito", "volumen suave", "volumen tenue", "volumen al minimo", "volumen minimo",
        "pon volumen bajo", "pon el volumen bajo", "ponle volumen bajo", "dale volumen bajo", "pon volumen bajito",
        "pon el volumen bajito", "volumen al 20", "volumen al 25", "volumen 20", "volumen 25", "volumen suavemente",
        "volumen leve", "low volume", "soft volume", "quiet volume"
    )

    private val VOLUME_HIGH_SYNONYMS = listOf(
        "volumen alto", "volumen fuerte", "volumen bien alto", "volumen arriba", "volumen al 80", "volumen 80",
        "pon volumen alto", "pon el volumen alto", "ponle volumen alto", "dale volumen alto", "pon volumen fuerte",
        "volumen elevado", "high volume", "loud volume"
    )

    private val VOLUME_MAX_SYNONYMS = listOf(
        "volumen al maximo", "volumen maximo", "al maximo", "al tope", "sube al maximo", "subir al maximo",
        "subelo al maximo", "subele al maximo", "pon al maximo", "ponlo al maximo", "ponle al maximo",
        "todo el volumen", "a todo volumen", "pon todo el volumen", "dale todo el volumen", "pon a todo volumen",
        "volumen tope", "volumen al 100", "volumen cien", "volumen al cien", "100 de volumen", "cien por ciento",
        "max volume", "full volume", "al max", "tope de volumen", "maximo volumen", "maximum volume"
    )

    private val MUTE_SYNONYMS = listOf(
        "silencio", "mutear", "mute", "miut", "enmudecer", "enmudece", "quitar sonido", "quitar el sonido", "quita el sonido",
        "quitale el sonido", "sin sonido", "silenciar", "silencia", "silencielo", "silencialo", "callate", "shh", "shhh",
        "mute audio", "silenciar audio", "silenciar la musica", "silencia la musica", "silence", "be quiet", "shut up"
    )

    private val SEARCH_PREFIXES = listOf(
        "activar busqueda de la cancion", "activar busqueda del tema", "activar busqueda de",
        "activar busqueda", "activar la busqueda de", "activar la busqueda",
        "activa busqueda de la cancion", "activa busqueda del tema", "activa busqueda de",
        "activa busqueda", "activa la busqueda de", "activa la busqueda",
        "modo busqueda de", "modo busqueda", "iniciar busqueda de", "iniciar busqueda",
        "hacer busqueda de", "hacer busqueda", "abrir busqueda de", "abrir busqueda",
        "pon la cancion de", "pon la cancion", "pon el tema de", "pon el tema", "pon el disco de",
        "pon algo de", "pon alguna de", "pon un tema de", "pon una cancion de", "pon la musica de",
        "pon musica de", "pon musica", "pon me", "ponme", "ponte", "pon a", "pon el", "pon la", "pon",
        "reproduce la cancion de", "reproduce la cancion", "reproduce el tema de", "reproduce el tema",
        "reproduce algo de", "reproduce un tema de", "reproduce musica de", "reproduce musica", "reproduce me", "reproduceme", "reproduce a", "reproduce el", "reproduce la", "reproduce",
        "reproducir la cancion de", "reproducir la cancion", "reproducir el tema", "reproducir musica de", "reproducir musica",
        "reproducir a", "reproducir me", "reproducirme", "reproducir",
        "busca la cancion de", "busca la cancion", "busca el tema de", "busca el tema", "busca el disco de",
        "busca algo de", "busca musica de", "busca musica", "busca a", "busca video de", "busca video", "buscame a", "buscame", "buscate", "busca",
        "buscar la cancion de", "buscar la cancion", "buscar el tema de", "buscar el tema",
        "buscar musica de", "buscar musica", "buscar a", "buscar video de", "buscar video", "buscar",
        "buscame la cancion de", "buscame la cancion", "buscame el tema de", "buscame el tema",
        "escuchar la cancion de", "escuchar la cancion", "escuchar el tema", "escuchar musica de", "escuchar musica", "escuchar a", "escuchar",
        "escucha la cancion de", "escucha la cancion", "escucha el tema", "escucha musica de", "escucha musica", "escucha a", "escucha",
        "toca la cancion de", "toca la cancion", "toca el tema", "toca musica de", "toca musica", "toca algo de", "toca a", "tocame", "toca",
        "tocar la cancion", "tocar musica", "tocar", "sintoniza", "sintonizar", "colocar", "coloca",
        // English & Bilingual search prefixes
        "play song by", "play track by", "play song", "play track", "play the song", "play the track", "play music by", "play music", "play some music by", "play some", "play me", "play a song by", "play a", "play the", "play",
        "plei la cancion de", "plei la cancion", "plei cancion", "plei musica", "plei",
        "search for song", "search for the song", "search for track", "search for artist", "search for", "search song", "search track", "search artist", "search music", "search",
        "listen to the song", "listen to song", "listen to track", "listen to music by", "listen to music", "listen to", "listen",
        "put on the song", "put on some", "put on music by", "put on music", "put on", "stream the song", "stream", "find the song", "find song", "find track", "find artist", "find"
    )

    /**
     * Determines whether the given spoken text contains the configured wake word or any of its greetings/variants at the start.
     */
    fun isWakeWordDetected(rawSpokenText: String, configuredMasterWord: String): Boolean {
        val normalizedText = normalize(rawSpokenText)
        val normalizedMaster = normalize(configuredMasterWord)
        if (normalizedText.isBlank()) return false

        val sortedVariants = getWakeWordVariants(normalizedMaster)
        for (variant in sortedVariants) {
            if (variant.isNotBlank()) {
                if (normalizedText == variant || normalizedText.startsWith("$variant ")) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Extracts all variants for a wake word, including Spanish and English colloquial greetings.
     */
    fun getWakeWordVariants(normalizedMaster: String): List<String> {
        val masterVariants = mutableListOf(normalizedMaster)
        if (normalizedMaster == "musica" || normalizedMaster == "music") {
            masterVariants.addAll(listOf("musica", "music", "musiquita"))
        }

        val prefixGreetings = listOf(
            "oye", "hey", "ok", "hola", "a ver", "por favor", "porfa", "hazme el favor", "activa", "activar", "bueno", "atencion", "disculpa"
        )

        val extendedVariants = mutableListOf<String>()
        for (v in masterVariants) {
            extendedVariants.add(v)
            for (g in prefixGreetings) {
                extendedVariants.add("$g $v")
            }
        }
        return extendedVariants.distinct().sortedByDescending { it.length }
    }

    /**
     * Extracts the command part after the wake word if present, otherwise returns the input.
     */
    fun extractCommandAfterWakeWord(rawSpokenText: String, configuredMasterWord: String): String {
        val normalizedText = normalize(rawSpokenText)
        val normalizedMaster = normalize(configuredMasterWord)
        val sortedVariants = getWakeWordVariants(normalizedMaster)

        for (variant in sortedVariants) {
            if (variant.isNotBlank()) {
                if (normalizedText == variant) {
                    return ""
                }
                if (normalizedText.startsWith("$variant ")) {
                    return normalizedText.substring(variant.length).trim()
                }
            }
        }
        return rawSpokenText.trim()
    }

    fun parse(
        rawSpokenText: String,
        configuredMasterWord: String,
        allowDirectCommands: Boolean = false
    ): ParseResult {
        val normalizedText = normalize(rawSpokenText)
        val normalizedMaster = normalize(configuredMasterWord)

        if (normalizedText.isBlank()) {
            return ParseResult(VoiceCommand.None, false, rawSpokenText, "Texto vacío")
        }

        // 1. If Direct Commands are allowed, check for media actions or explicit search phrases immediately
        if (allowDirectCommands) {
            val directMediaCmd = matchMediaCommand(cleanFillers(normalizedText)) ?: matchMediaCommand(normalizedText)
            if (directMediaCmd != null) {
                return ParseResult(
                    command = directMediaCmd,
                    matchedMasterWord = true,
                    rawText = rawSpokenText,
                    description = directMediaCmd.toDisplayAction()
                )
            }

            // Check if user said an explicit search action without wake word: e.g. "pon Eminem", "reproduce Queen", "busca Coldplay"
            for (prefix in SEARCH_PREFIXES) {
                if (normalizedText.startsWith("$prefix ")) {
                    val query = cleanQuery(normalizedText, configuredMasterWord)
                    if (query.isNotBlank() && !isIncompleteOrNoise(query)) {
                        return ParseResult(
                            command = VoiceCommand.SearchAndPlay(query),
                            matchedMasterWord = true,
                            rawText = rawSpokenText,
                            description = "Buscar y reproducir: '$query'"
                        )
                    }
                }
            }
        }

        // 2. Identify Master / Wake Word (e.g. "música", "oye música", "hey música", "ok música", "por favor música")
        val sortedVariants = getWakeWordVariants(normalizedMaster)

        var containsMasterAtStart = false
        var commandText = ""

        for (variant in sortedVariants) {
            if (variant.isNotBlank()) {
                if (normalizedText == variant) {
                    containsMasterAtStart = true
                    commandText = ""
                    break
                } else if (normalizedText.startsWith("$variant ")) {
                    containsMasterAtStart = true
                    commandText = normalizedText.substring(variant.length).trim()
                    break
                }
            }
        }

        // If the wake word was NOT spoken at the beginning, ignore this speech when direct commands is disabled
        if (!containsMasterAtStart) {
            return ParseResult(
                VoiceCommand.None,
                matchedMasterWord = false,
                rawText = rawSpokenText,
                description = "Palabra de activación '$configuredMasterWord' no detectada al inicio"
            )
        }

        // If user said ONLY the master word (e.g. "Música", "Oye Música") -> Opens wake window
        if (commandText.isBlank()) {
            return ParseResult(
                VoiceCommand.WakeWordOnly(""),
                matchedMasterWord = true,
                rawText = rawSpokenText,
                description = "Palabra de activación detectada: '$configuredMasterWord'. Esperando orden..."
            )
        }

        val cleanedTarget = cleanFillers(commandText)

        // 3. Check Media Commands on extracted command text (e.g. "música pausa", "oye música siguiente", "música sube volumen")
        val mediaCmd = matchMediaCommand(cleanedTarget) ?: matchMediaCommand(commandText)
        if (mediaCmd != null) {
            return ParseResult(mediaCmd, true, rawSpokenText, mediaCmd.toDisplayAction())
        }

        // 4. Extract clean song query for search and play (e.g. "música pon Queen", "oye música busca rock", "música eminem")
        val cleanSongQuery = cleanQuery(commandText, configuredMasterWord)
        if (cleanSongQuery.isNotBlank() && cleanSongQuery.length >= 2 && !isIncompleteOrNoise(cleanSongQuery) && !isIncompleteOrNoise(commandText)) {
            return ParseResult(
                VoiceCommand.SearchAndPlay(cleanSongQuery),
                true,
                rawSpokenText,
                "Buscar y reproducir: '$cleanSongQuery'"
            )
        } else {
            // User spoke master word followed by ambiguous filler, incomplete verb or empty noise -> Open wake window to allow user to complete request
            return ParseResult(
                VoiceCommand.WakeWordOnly(""),
                matchedMasterWord = true,
                rawText = rawSpokenText,
                description = "Palabra de activación detectada: '$configuredMasterWord'. Esperando orden..."
            )
        }
    }

    /**
     * Checks if a word or phrase is just grammatical noise, search action verb, or placeholder without a real title/artist.
     */
    private val TRIVIAL_WORDS = setOf(
        "que", "de", "la", "el", "los", "las", "un", "una", "unos", "unas", "y", "o", "u", "e",
        "en", "a", "al", "del", "por", "para", "con", "sin", "sobre", "tras",
        "es", "son", "fue", "era", "ser", "estar", "esta", "estan", "este", "esta", "esto", "estos", "estas",
        "no", "si", "ya", "hay", "ha", "han", "he", "has", "se", "me", "te", "le", "nos", "les", "lo",
        "mi", "tu", "su", "mis", "tus", "sus", "mio", "tuyo", "suyo",
        "pero", "mas", "aunque", "porque", "como", "cuando", "donde", "quien", "cual",
        "bueno", "bien", "nada", "todo", "algo", "asi", "aqui", "ahi", "alla", "alli",
        "eh", "ah", "oh", "uh", "um", "ajá", "aja", "oye", "hey", "hola", "ok", "a ver"
    )

    val GENERIC_MUSIC_PLACEHOLDERS = setOf(
        "cancion", "canciones", "tema", "temas", "musica", "rolita", "rolitas", "rola", "rolas",
        "pista", "pistas", "disco", "discos", "album", "albumes", "artista", "banda", "grupo",
        "cantante", "video", "videos", "audio", "sonido", "volumen", "nivel", "radio", "reproductor",
        "playlist", "lista", "cancioncita", "temita", "musiquita"
    )

    val SEARCH_ACTION_WORDS = setOf(
        "pon", "pone", "poner", "ponle", "ponlo", "ponte", "ponme", "ponla",
        "busca", "buscar", "buscame", "buscate", "buscale",
        "reproduce", "reproducir", "reproduceme", "reproducila", "reproducilo",
        "toca", "tocar", "tocame",
        "escucha", "escuchar",
        "sintoniza", "sintonizar", "coloca", "colocar", "dale",
        "play", "search", "listen"
    )

    fun isIncompleteOrNoise(text: String): Boolean {
        val clean = normalize(text)
        if (clean.isBlank()) return true
        val words = clean.split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return true
        if (words.size == 1) {
            val single = words[0]
            if (TRIVIAL_WORDS.contains(single) || GENERIC_MUSIC_PLACEHOLDERS.contains(single) || SEARCH_ACTION_WORDS.contains(single)) {
                return true
            }
        }
        val isAllFillersOrPlaceholders = words.all { w ->
            TRIVIAL_WORDS.contains(w) ||
            GENERIC_MUSIC_PLACEHOLDERS.contains(w) ||
            SEARCH_ACTION_WORDS.contains(w)
        }
        return isAllFillersOrPlaceholders
    }

    /**
     * Direct parser for Push-to-Talk input or when the wake-word window is active.
     */
    fun parseDirect(rawSpokenText: String): ParseResult {
        val normalizedText = normalize(rawSpokenText)
        if (normalizedText.isBlank()) {
            return ParseResult(
                command = VoiceCommand.None,
                matchedMasterWord = true,
                rawText = rawSpokenText,
                description = "Orden incompleta. Te sigo escuchando...",
                intent = "UNKNOWN",
                isClearLocalCommand = false
            )
        }

        val cleanedTarget = cleanFillers(normalizedText)

        // 1. Check Media Commands First (play, pause, next, volume, stop, mute, etc.)
        val mediaCmd = matchMediaCommand(cleanedTarget) ?: matchMediaCommand(normalizedText)
        if (mediaCmd != null) {
            return ParseResult(
                command = mediaCmd,
                matchedMasterWord = true,
                rawText = rawSpokenText,
                description = mediaCmd.toDisplayAction(),
                intent = mediaCmd.toIntentName(),
                isClearLocalCommand = true
            )
        }

        if (isIncompleteOrNoise(normalizedText)) {
            return ParseResult(
                command = VoiceCommand.None,
                matchedMasterWord = true,
                rawText = rawSpokenText,
                description = "Orden incompleta. Te sigo escuchando...",
                intent = "UNKNOWN",
                isClearLocalCommand = false
            )
        }

        // 2. Extract structured music entities and clean query
        val parsed = MusicCommandParser.parse(rawSpokenText)
        val isConversational = MusicCommandParser.isComplexOrConversational(rawSpokenText)

        // 3. Extract pure clean query (strips wake words, "música", prefixes like "pon", "de", etc.)
        val cleanSongQuery = cleanQuery(normalizedText)
        if (cleanSongQuery.isNotBlank() && cleanSongQuery.length >= 2 && !isIncompleteOrNoise(cleanSongQuery)) {
            return ParseResult(
                command = VoiceCommand.SearchAndPlay(cleanSongQuery),
                matchedMasterWord = true,
                rawText = rawSpokenText,
                description = "Buscar y reproducir: '$cleanSongQuery'",
                intent = "SEARCH_AND_PLAY",
                artist = parsed.artist,
                track = parsed.track,
                searchQuery = cleanSongQuery,
                isClearLocalCommand = !isConversational
            )
        }

        return ParseResult(
            command = VoiceCommand.None,
            matchedMasterWord = true,
            rawText = rawSpokenText,
            description = "Comando no reconocido: '$rawSpokenText'",
            intent = "UNKNOWN",
            isClearLocalCommand = false
        )
    }

    /**
     * Cleans wake words, filler phrases, action verbs (pon, reproduce, busca),
     * and connector prepositions (de, a, del) so that only the actual artist/song name remains.
     * Example: "Música - eminem" -> "eminem"
     * Example: "Música pon la canción de eminem" -> "eminem"
     * Example: "Oye música de Queen" -> "queen"
     */
    fun cleanQuery(rawText: String, masterWord: String = ""): String {
        var text = normalize(rawText)

        // 1. Strip master word and variations
        val masterVariants = mutableListOf(
            "musica", "music", "musiquita", "cancion", "canción", "reproductor"
        )
        if (masterWord.isNotBlank()) {
            val normMaster = normalize(masterWord)
            masterVariants.add(normMaster)
        }
        val extendedVariants = masterVariants.flatMap { v ->
            listOf(
                "oye $v", "hey $v", "ok $v", "hola $v", "a ver $v", "activa $v", "activar $v", v
            )
        }.distinct().sortedByDescending { it.length }

        for (variant in extendedVariants) {
            if (text == variant) {
                return ""
            }
            if (text.startsWith("$variant ")) {
                text = text.substring(variant.length).trim()
            }
        }

        // 2. Strip polite fillers
        val fillerPrefixes = listOf(
            "por favor", "porfa", "hazme el favor de", "haz el favor de",
            "quiero que", "puedes", "quiero escuchar", "quiero oir",
            "quiero la cancion de", "quiero la cancion", "quiero el tema de", "quiero el tema", "quiero",
            "dale a", "dale al", "dale"
        ).sortedByDescending { it.length }

        for (f in fillerPrefixes) {
            if (text.startsWith("$f ")) {
                text = text.substring(f.length).trim()
            }
        }

        // 3. Strip search action prefixes
        val searchPrefixes = SEARCH_PREFIXES.sortedByDescending { it.length }

        for (prefix in searchPrefixes) {
            if (text.startsWith("$prefix ")) {
                text = text.substring(prefix.length).trim()
            } else if (text == prefix) {
                return ""
            }
        }

        // 4. Strip leading connectors if left after prefix stripping: e.g. "de eminem" -> "eminem", "a eminem" -> "eminem"
        val leadingConnectors = listOf("de ", "a ", "del ", "para ", "por ")
        for (conn in leadingConnectors) {
            if (text.startsWith(conn)) {
                text = text.substring(conn.length).trim()
            }
        }

        // 5. Secondary check: if master word was still attached at front
        for (variant in extendedVariants) {
            if (text.startsWith("$variant ")) {
                text = text.substring(variant.length).trim()
            }
        }

        val baseQuery = text.trim()
        if (baseQuery.isBlank()) return ""

        // 6. Bilingual & Phonetic correction (e.g. "kuin" -> "Queen", "col plei" -> "Coldplay", "vili ailis" -> "Billie Eilish")
        return BilingualSpeechCorrector.correctQuery(baseQuery)
    }

    private val BILINGUAL_NUMBER_WORDS = mapOf(
        "cero" to 0, "zero" to 0,
        "uno" to 1, "un" to 1, "one" to 1,
        "dos" to 2, "two" to 2,
        "tres" to 3, "three" to 3,
        "cuatro" to 4, "four" to 4,
        "cinco" to 5, "five" to 5,
        "seis" to 6, "six" to 6,
        "siete" to 7, "seven" to 7,
        "ocho" to 8, "eight" to 8,
        "nueve" to 9, "nine" to 9,
        "diez" to 10, "ten" to 10,
        "once" to 11, "eleven" to 11,
        "doce" to 12, "twelve" to 12,
        "trece" to 13, "thirteen" to 13,
        "catorce" to 14, "fourteen" to 14,
        "quince" to 15, "fifteen" to 15
    )

    private fun parseNumberOrWord(token: String): Int? {
        val directInt = token.toIntOrNull()
        if (directInt != null) return directInt
        return BILINGUAL_NUMBER_WORDS[token.lowercase(Locale.ROOT)]
    }

    private fun matchSpecificVolumeLevel(text: String): VoiceCommand.SetVolume? {
        val normalized = normalize(text)
        if (normalized.isBlank()) return null

        // 1. Regex match for commands in Spanish and English like:
        // "volumen 1" ... "volumen 15", "volumen al 7", "set volume to 8", "volume 10", "level 8"
        val volumeRegex = Regex("""^(?:(?:pon(?:le|lo)?|ajusta(?:r)?|coloca(?:r)?|sube(?:le)?|baja(?:le)?|dale|establece(?:r)?|set|change|turn)\s+)?(?:(?:to|the|el)\s+)?(?:(?:nivel\s+(?:de\s+)?|level\s+(?:of\s+)?)?volumen|volume|nivel|level)\s*(?:al?|en|nivel|a\s+nivel|to|at)?\s*(\d{1,2}|cero|zero|uno|un|one|dos|two|tres|three|cuatro|four|cinco|five|seis|six|siete|seven|ocho|eight|nueve|nine|diez|ten|once|eleven|doce|twelve|trece|thirteen|catorce|fourteen|quince|fifteen)(?:\s*(?:de\s+volumen|puntos?|niveles?|percent|%))?$""")
        val match = volumeRegex.find(normalized)
        if (match != null) {
            val numStr = match.groupValues[1]
            val level = parseNumberOrWord(numStr)
            if (level != null && level in 0..15) {
                return VoiceCommand.SetVolume(level)
            }
        }

        // 2. Tokenized scan: find "volumen [X]", "volume [X]", "level [X]" or "volumen al/en/a [X]"
        val words = normalized.split(" ").filter { it.isNotBlank() }
        for (i in words.indices) {
            val w = words[i]
            if (w == "volumen" || w == "volume" || w == "nivel" || w == "level") {
                if (i + 1 < words.size) {
                    val next = words[i + 1]
                    val parsedNext = parseNumberOrWord(next)
                    if (parsedNext != null && parsedNext in 0..15) {
                        return VoiceCommand.SetVolume(parsedNext)
                    }
                    if ((next == "al" || next == "a" || next == "en" || next == "del" || next == "nivel" || next == "to" || next == "at") && i + 2 < words.size) {
                        val nextNext = words[i + 2]
                        val parsedNextNext = parseNumberOrWord(nextNext)
                        if (parsedNextNext != null && parsedNextNext in 0..15) {
                            return VoiceCommand.SetVolume(parsedNextNext)
                        }
                    }
                }
            }
        }

        return null
    }

    private fun matchMediaCommand(text: String): VoiceCommand? {
        if (text.isBlank()) return null

        // 0. Specific Volume Level (1 to 15)
        val specificVol = matchSpecificVolumeLevel(text)
        if (specificVol != null) return specificVol

        // 1. Volume Max
        if (matchesAny(text, VOLUME_MAX_SYNONYMS)) return VoiceCommand.VolumeMax

        // 2. Volume Medium
        if (matchesAny(text, VOLUME_MEDIUM_SYNONYMS)) return VoiceCommand.VolumeMedium

        // 3. Volume Low
        if (matchesAny(text, VOLUME_LOW_SYNONYMS)) return VoiceCommand.VolumeLow

        // 4. Volume High
        if (matchesAny(text, VOLUME_HIGH_SYNONYMS)) return VoiceCommand.VolumeHigh

        // 5. Mute / Silence
        if (matchesAny(text, MUTE_SYNONYMS)) return VoiceCommand.Mute

        // 6. Volume Up
        if (matchesAny(text, VOLUME_UP_SYNONYMS)) return VoiceCommand.VolumeUp

        // 7. Volume Down
        if (matchesAny(text, VOLUME_DOWN_SYNONYMS)) return VoiceCommand.VolumeDown

        // 8. Stop
        if (matchesAny(text, STOP_SYNONYMS)) return VoiceCommand.Stop

        // 9. Pause
        if (matchesAny(text, PAUSE_SYNONYMS)) return VoiceCommand.Pause

        // 10. Resume / Continue / Play
        if (matchesAny(text, RESUME_SYNONYMS)) return VoiceCommand.Resume

        // 11. Next Track / Change
        if (matchesAny(text, NEXT_SYNONYMS)) return VoiceCommand.NextTrack

        // 12. Previous Track / Back
        if (matchesAny(text, PREVIOUS_SYNONYMS)) return VoiceCommand.PreviousTrack

        // 13. Repeat Track
        if (matchesAny(text, REPEAT_SYNONYMS)) return VoiceCommand.RepeatTrack

        return null
    }

    private val SEARCH_VERB_PREFIXES = setOf("reproduce", "reproducir", "pon", "poner", "busca", "buscar", "toca", "tocar", "play")

    private fun matchesAny(text: String, patterns: List<String>): Boolean {
        val cleanText = cleanFillers(text)
        val paddedText = " $cleanText "
        for (pattern in patterns) {
            val words = pattern.split(" ").filter { it.isNotBlank() }

            // 1. Exact match on clean or raw text
            if (cleanText == pattern || text == pattern) {
                return true
            }

            // 2. Multi-word phrase matching
            if (words.size >= 2) {
                if (cleanText.endsWith(" $pattern") || text.endsWith(" $pattern")) {
                    return true
                }
                if (cleanText.startsWith("$pattern ") || text.startsWith("$pattern ")) {
                    return true
                }
                if (paddedText.contains(" $pattern ")) {
                    return true
                }
            } else {
                // 3. Single-word command matching: ONLY allowed as full phrase or suffix to polite filler
                if (cleanText == pattern || cleanText.endsWith(" $pattern") || text.endsWith(" $pattern")) {
                    // Make sure the single word is not a generic search verb prefix if there are extra trailing words
                    return true
                }
            }
        }
        return false
    }

    private fun cleanFillers(input: String): String {
        var result = input.trim()
        val fillerPrefixes = listOf(
            "por favor", "porfa", "hazme el favor de", "haz el favor de",
            "quiero que", "puedes", "quiero escuchar", "quiero oir",
            "dale a", "dale al", "dale", "pon a", "pon", "a ver", "oye"
        )
        for (f in fillerPrefixes) {
            if (result.startsWith("$f ")) {
                result = result.substring(f.length).trim()
            }
        }
        val fillerSuffixes = listOf(
            "por favor", "porfa", "por fa", "ya", "ahora", "rapido", "amigo", "de una vez", "porfis"
        )
        for (s in fillerSuffixes) {
            if (result.endsWith(" $s")) {
                result = result.substring(0, result.length - s.length).trim()
            }
        }
        return result
    }

    fun normalize(input: String): String {
        val trimmed = input.trim().lowercase(Locale.ROOT)
        val nfd = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
        val stripped = nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return stripped.replace(Regex("[¿?¡!.,;:\"'()\\-_]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

fun VoiceCommand.toDisplayAction(): String {
    return when (this) {
        is VoiceCommand.SearchAndPlay -> "Buscar y reproducir: '$songQuery'"
        is VoiceCommand.SetVolume -> "Ajustar volumen al nivel $level (de 15)"
        is VoiceCommand.VolumeUp -> "Subir volumen"
        is VoiceCommand.VolumeDown -> "Bajar volumen"
        is VoiceCommand.VolumeMedium -> "Volumen medio (50%)"
        is VoiceCommand.VolumeLow -> "Volumen bajo (25%)"
        is VoiceCommand.VolumeHigh -> "Volumen alto (85%)"
        is VoiceCommand.VolumeMax -> "Volumen al máximo"
        is VoiceCommand.Mute -> "Silenciar"
        is VoiceCommand.Pause -> "Pausar música"
        is VoiceCommand.Stop -> "Detener música"
        is VoiceCommand.Resume -> "Reanudar música"
        is VoiceCommand.NextTrack -> "Siguiente canción"
        is VoiceCommand.PreviousTrack -> "Anterior canción"
        is VoiceCommand.RepeatTrack -> "Repetir canción"
        is VoiceCommand.WakeWordOnly -> "Palabra maestra detectada"
        is VoiceCommand.None -> "Ninguna acción"
    }
}

