package com.example.data

import org.json.JSONArray
import org.json.JSONObject

enum class MediaItemCategory {
    CURRENT,
    UPCOMING,
    PLAYLIST
}

data class MediaCardItem(
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val duration: String = "",
    val coverUrl: String = "",
    val category: MediaItemCategory = MediaItemCategory.CURRENT,
    val playlistName: String = "",
    val commandQuery: String = "",
    val isSample: Boolean = false
) {
    val displaySubtitle: String
        get() = when {
            isSample -> "$artist • Muestra de portada"
            artist.isNotBlank() && album.isNotBlank() -> "$artist • $album"
            artist.isNotBlank() -> artist
            playlistName.isNotBlank() -> playlistName
            else -> when (category) {
                MediaItemCategory.CURRENT -> "Reproduciendo ahora"
                MediaItemCategory.UPCOMING -> "En cola de espera"
                MediaItemCategory.PLAYLIST -> "Lista de reproducción"
            }
        }
}

object MediaDataParser {

    /**
     * Normaliza URLs relativas de carátulas (ej: /cover.jpg o covers/song.png)
     * convirtiéndolas en URLs completas del servidor Host si se conocen la IP y puerto.
     */
    fun normalizeCoverUrl(rawUrl: String, hostIp: String = "", hostPort: Int = 8998): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("data:", ignoreCase = true) ||
            trimmed.startsWith("content://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true)
        ) {
            return trimmed
        }
        if (hostIp.isNotBlank()) {
            val cleanPath = trimmed.trimStart('/')
            return "http://$hostIp:$hostPort/$cleanPath"
        }
        return trimmed
    }

    private fun extractArrayFromPotentialContainer(rawJson: String): JSONArray? {
        val trimmed = rawJson.trim()
        if (trimmed.startsWith("[")) {
            return try {
                JSONArray(trimmed)
            } catch (_: Exception) {
                null
            }
        }
        if (trimmed.startsWith("{")) {
            return try {
                val obj = JSONObject(trimmed)
                val candidates = listOf(
                    "queue", "upcoming", "upcomingTracks", "tracks", "songs",
                    "items", "list", "playlist", "playlists", "data", "results"
                )
                for (key in candidates) {
                    val arr = obj.optJSONArray(key)
                    if (arr != null) return arr
                }
                null
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    fun optFirstString(obj: JSONObject, vararg keys: String): String {
        for (key in keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                val s = obj.optString(key, "").trim()
                if (s.isNotBlank() && !s.equals("null", ignoreCase = true)) return s
            }
        }
        return ""
    }

    /**
     * Compara de forma robusta si dos canciones corresponden a la misma pista,
     * ignorando mayúsculas, signos de puntuación y sufijos de formato (Official Video, Remastered, etc.).
     */
    fun isSameTrack(title1: String, artist1: String = "", title2: String, artist2: String = ""): Boolean {
        val t1 = title1.trim()
        val t2 = title2.trim()
        if (t1.isBlank() || t2.isBlank()) return false
        if (t1.equals(t2, ignoreCase = true)) return true

        fun clean(s: String): String {
            return s.lowercase()
                .replace(Regex("""\s*[\(\[](?:official|audio|video|lyrics|remastered|deluxe|explicit|hd|4k|feat|ft\.)[^\)\]]*[\)\]]"""), "")
                .replace(Regex("""\s*-\s*(?:remastered|single version|radio edit|live|official).*$"""), "")
                .replace(Regex("""[^a-z0-9áéíóúñ\s]"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
        }

        val c1 = clean(t1)
        val c2 = clean(t2)
        if (c1.isNotBlank() && c2.isNotBlank() && c1 == c2) return true

        if (c1.length >= 4 && c2.length >= 4) {
            if (c1.contains(c2) || c2.contains(c1)) {
                val a1 = clean(artist1)
                val a2 = clean(artist2)
                if (a1.isBlank() || a2.isBlank() || a1 == a2 || a1.contains(a2) || a2.contains(a1)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Parsea la lista de próximas canciones (queue/upcoming) enviada por el Host.
     * Soporta tanto JSON Arrays como JSON Objects contenedores de colas y múltiples nombres de campos.
     */
    fun parseUpcomingList(jsonString: String, hostIp: String = "", hostPort: Int = 8998): List<MediaCardItem> {
        if (jsonString.isBlank()) return emptyList()
        val items = mutableListOf<MediaCardItem>()
        try {
            val array = extractArrayFromPotentialContainer(jsonString) ?: return emptyList()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i)
                if (obj != null) {
                    val title = optFirstString(obj, "title", "song", "name", "track", "trackName", "track_name", "label")
                    if (title.isBlank()) continue

                    val artist = optFirstString(obj, "artist", "channel", "author", "singer", "uploader", "creator", "subtitle")
                    val album = optFirstString(obj, "album", "collection")
                    val duration = optFirstString(obj, "duration", "time", "length")
                    val rawCover = optFirstString(
                        obj,
                        "coverUrl", "cover_url", "cover", "thumbnail", "thumbnailUrl", "thumbnail_url",
                        "artwork", "artworkUrl", "art", "image", "img", "poster", "picture", "caratula"
                    )
                    val cover = normalizeCoverUrl(rawCover, hostIp, hostPort)
                    val id = obj.optString("id", "upcoming-$i-${title.hashCode()}")
                    items.add(
                        MediaCardItem(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            coverUrl = cover,
                            category = MediaItemCategory.UPCOMING,
                            commandQuery = if (artist.isNotBlank() && !title.contains(artist, ignoreCase = true)) "$title $artist" else title,
                            isSample = false
                        )
                    )
                } else {
                    // String plano en la lista: ej. "Queen - Bohemian Rhapsody"
                    val plainString = array.optString(i, "").trim()
                    if (plainString.isNotBlank()) {
                        val parts = plainString.split(" - ", limit = 2)
                        val title = if (parts.size == 2) parts[1].trim() else plainString
                        val artist = if (parts.size == 2) parts[0].trim() else ""
                        items.add(
                            MediaCardItem(
                                id = "upcoming-plain-$i-${title.hashCode()}",
                                title = title,
                                artist = artist,
                                category = MediaItemCategory.UPCOMING,
                                commandQuery = plainString,
                                isSample = false
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return items
    }

    /**
     * Parsea la lista de canciones de playlist recibidas del Host.
     */
    fun parsePlaylistList(jsonString: String, hostIp: String = "", hostPort: Int = 8998): List<MediaCardItem> {
        if (jsonString.isBlank()) return emptyList()
        val items = mutableListOf<MediaCardItem>()
        try {
            val array = extractArrayFromPotentialContainer(jsonString) ?: return emptyList()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i)
                if (obj != null) {
                    val title = optFirstString(obj, "title", "song", "name", "track", "trackName", "label")
                    if (title.isBlank()) continue

                    val artist = optFirstString(obj, "artist", "channel", "author", "singer")
                    val playlist = optFirstString(obj, "playlistName", "playlist", "category", "collection").ifBlank { "Favoritos" }
                    val rawCover = optFirstString(
                        obj,
                        "coverUrl", "cover_url", "cover", "thumbnail", "thumbnailUrl", "thumbnail_url",
                        "artwork", "artworkUrl", "art", "image", "img", "poster", "picture", "caratula"
                    )
                    val cover = normalizeCoverUrl(rawCover, hostIp, hostPort)
                    val id = obj.optString("id", "playlist-$i-${title.hashCode()}")
                    items.add(
                        MediaCardItem(
                            id = id,
                            title = title,
                            artist = artist,
                            playlistName = playlist,
                            coverUrl = cover,
                            category = MediaItemCategory.PLAYLIST,
                            commandQuery = if (artist.isNotBlank() && !title.contains(artist, ignoreCase = true)) "$title $artist" else title,
                            isSample = false
                        )
                    )
                } else {
                    val plainString = array.optString(i, "").trim()
                    if (plainString.isNotBlank()) {
                        val parts = plainString.split(" - ", limit = 2)
                        val title = if (parts.size == 2) parts[1].trim() else plainString
                        val artist = if (parts.size == 2) parts[0].trim() else ""
                        items.add(
                            MediaCardItem(
                                id = "playlist-plain-$i-${title.hashCode()}",
                                title = title,
                                artist = artist,
                                playlistName = "Favoritos",
                                category = MediaItemCategory.PLAYLIST,
                                commandQuery = plainString,
                                isSample = false
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return items
    }

    /**
     * Proporciona posters cinemáticos de muestra ÚNICAMENTE cuando NO hay nada en la cola del Host.
     * Cuando el Host envía datos reales (canción actual, cola o listas), estas muestras son 100%
     * sustituidas por las portadas y títulos reales del Host.
     */
    fun getCinematicPresetCards(currentSongTitle: String = "", currentCoverUrl: String = ""): List<MediaCardItem> {
        val list = mutableListOf<MediaCardItem>()

        if (currentSongTitle.isNotBlank()) {
            list.add(
                MediaCardItem(
                    id = "curr-live",
                    title = currentSongTitle,
                    artist = "Transmitido por Host PC",
                    album = "Master Audio Stream",
                    coverUrl = currentCoverUrl,
                    category = MediaItemCategory.CURRENT,
                    commandQuery = currentSongTitle,
                    isSample = false
                )
            )
        }

        // Preset cards de muestra con indicador explícito isSample = true
        list.addAll(
            listOf(
                MediaCardItem(
                    id = "preset-1",
                    title = "DUNE: PART TWO",
                    artist = "Hans Zimmer",
                    album = "Original Motion Picture Soundtrack",
                    duration = "3:48",
                    coverUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
                    category = if (currentSongTitle.isBlank()) MediaItemCategory.CURRENT else MediaItemCategory.UPCOMING,
                    commandQuery = "Dune Hans Zimmer",
                    isSample = true
                ),
                MediaCardItem(
                    id = "preset-2",
                    title = "THE BATMAN",
                    artist = "Michael Giacchino",
                    album = "The Batman Theme",
                    duration = "4:12",
                    coverUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&q=80",
                    category = MediaItemCategory.UPCOMING,
                    commandQuery = "The Batman Theme",
                    isSample = true
                ),
                MediaCardItem(
                    id = "preset-3",
                    title = "JOHN WICK: CHAPTER 4",
                    artist = "Tyler Bates & Joel J. Richard",
                    album = "Neo-Noir Action Beats",
                    duration = "3:25",
                    coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
                    category = MediaItemCategory.UPCOMING,
                    commandQuery = "John Wick Soundtrack",
                    isSample = true
                ),
                MediaCardItem(
                    id = "preset-4",
                    title = "THE LAST OF US",
                    artist = "Gustavo Santaolalla",
                    album = "All Gone (Seasons)",
                    duration = "2:54",
                    coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&q=80",
                    category = MediaItemCategory.UPCOMING,
                    commandQuery = "Gustavo Santaolalla The Last of Us",
                    isSample = true
                ),
                MediaCardItem(
                    id = "preset-5",
                    title = "STRANGER THINGS",
                    artist = "Kyle Dixon & Michael Stein",
                    album = "Synthwave Nostalgia",
                    duration = "3:10",
                    coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&q=80",
                    category = MediaItemCategory.PLAYLIST,
                    playlistName = "Synthwave 80s",
                    commandQuery = "Stranger Things Theme",
                    isSample = true
                ),
                MediaCardItem(
                    id = "preset-6",
                    title = "STAR WARS: THE FORCE",
                    artist = "John Williams",
                    album = "Imperial March Orchestral",
                    duration = "5:02",
                    coverUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&q=80",
                    category = MediaItemCategory.PLAYLIST,
                    playlistName = "Epic Soundtracks",
                    commandQuery = "Star Wars Imperial March John Williams",
                    isSample = true
                ),
                MediaCardItem(
                    id = "preset-7",
                    title = "JURASSIC WORLD",
                    artist = "Michael Giacchino",
                    album = "Theme Park Orchestral",
                    duration = "4:30",
                    coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800&q=80",
                    category = MediaItemCategory.PLAYLIST,
                    playlistName = "Cinematic World",
                    commandQuery = "Jurassic World Soundtrack",
                    isSample = true
                )
            )
        )
        return list
    }
}
