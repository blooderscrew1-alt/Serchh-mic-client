package com.example

import com.example.network.RemoteNodeMessage
import com.example.network.RemoteMessageType
import com.example.voice.VoiceCommand
import com.example.voice.VoiceCommandEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
    private val master = "Música"

    @Test
    fun testPauseCommandsWithWakeWord() {
        val pausePhrasesWithWake = listOf(
            "música pausa",
            "oye música pon pausa",
            "música pausar",
            "música ponle pausa"
        )
        for (phrase in pausePhrasesWithWake) {
            val result = VoiceCommandEngine.parse(phrase, master)
            assertEquals("Expected Pause for '$phrase'", VoiceCommand.Pause, result.command)
        }
    }

    @Test
    fun testStopCommandsWithWakeWord() {
        val stopPhrasesWithWake = listOf(
            "música detener",
            "música para de reproducir",
            "música stop",
            "música detente"
        )
        for (phrase in stopPhrasesWithWake) {
            val result = VoiceCommandEngine.parse(phrase, master)
            assertEquals("Expected Stop for '$phrase'", VoiceCommand.Stop, result.command)
        }
    }

    @Test
    fun testDirectPauseCommands() {
        val pausePhrases = listOf(
            "pausa",
            "pausar",
            "pon pausa",
            "dale pausa"
        )
        for (phrase in pausePhrases) {
            val result = VoiceCommandEngine.parseDirect(phrase)
            assertEquals("Expected Pause for '$phrase'", VoiceCommand.Pause, result.command)
        }
    }

    @Test
    fun testDirectStopCommands() {
        val stopPhrases = listOf(
            "detener",
            "para",
            "stop",
            "detente"
        )
        for (phrase in stopPhrases) {
            val result = VoiceCommandEngine.parseDirect(phrase)
            assertEquals("Expected Stop for '$phrase'", VoiceCommand.Stop, result.command)
        }
    }

    @Test
    fun testResumeCommandsWithWakeWord() {
        val resumePhrases = listOf(
            "música continuar",
            "oye música dale play",
            "música reanudar",
            "música play"
        )
        for (phrase in resumePhrases) {
            val result = VoiceCommandEngine.parse(phrase, master)
            assertEquals("Expected Resume for '$phrase'", VoiceCommand.Resume, result.command)
        }
    }

    @Test
    fun testDirectResumeCommands() {
        val resumePhrases = listOf(
            "continuar",
            "continua",
            "reanudar",
            "play",
            "dale play",
            "pon play",
            "despausar"
        )
        for (phrase in resumePhrases) {
            val result = VoiceCommandEngine.parseDirect(phrase)
            assertEquals("Expected Resume for '$phrase'", VoiceCommand.Resume, result.command)
        }
    }

    @Test
    fun testNextTrackCommands() {
        val nextPhrases = listOf(
            "música siguiente",
            "música cambia la cancion",
            "oye música pasa de cancion",
            "música next"
        )
        for (phrase in nextPhrases) {
            val result = VoiceCommandEngine.parse(phrase, master)
            assertEquals("Expected NextTrack for '$phrase'", VoiceCommand.NextTrack, result.command)
        }

        val directNext = listOf("siguiente", "pasa la cancion", "cambia de cancion", "otra cancion", "next")
        for (phrase in directNext) {
            val result = VoiceCommandEngine.parseDirect(phrase)
            assertEquals("Expected NextTrack for '$phrase'", VoiceCommand.NextTrack, result.command)
        }
    }

    @Test
    fun testPreviousTrackCommands() {
        val prevPhrases = listOf(
            "música anterior",
            "oye música pon la anterior",
            "música retroceder"
        )
        for (phrase in prevPhrases) {
            val result = VoiceCommandEngine.parse(phrase, master)
            assertEquals("Expected PreviousTrack for '$phrase'", VoiceCommand.PreviousTrack, result.command)
        }

        val directPrev = listOf("anterior", "anterior cancion", "cancion anterior", "atras", "retroceder", "previous")
        for (phrase in directPrev) {
            val result = VoiceCommandEngine.parseDirect(phrase)
            assertEquals("Expected PreviousTrack for '$phrase'", VoiceCommand.PreviousTrack, result.command)
        }
    }

    @Test
    fun testVolumeCommands() {
        val volUpPhrases = listOf(
            "música sube volumen",
            "música mas volumen",
            "música aumenta el volumen"
        )
        for (phrase in volUpPhrases) {
            val result = VoiceCommandEngine.parse(phrase, master)
            assertEquals("Expected VolumeUp for '$phrase'", VoiceCommand.VolumeUp, result.command)
        }

        val directVolUp = listOf("sube el volumen", "subir volumen", "mas volumen", "subele", "mas alto")
        for (phrase in directVolUp) {
            val result = VoiceCommandEngine.parseDirect(phrase)
            assertEquals("Expected VolumeUp for '$phrase'", VoiceCommand.VolumeUp, result.command)
        }

        val directVolDown = listOf("baja el volumen", "bajar volumen", "menos volumen", "bajale", "mas bajo")
        for (phrase in directVolDown) {
            val result = VoiceCommandEngine.parseDirect(phrase)
            assertEquals("Expected VolumeDown for '$phrase'", VoiceCommand.VolumeDown, result.command)
        }

        val directVolMax = listOf("volumen al maximo", "al tope", "sube al maximo")
        for (phrase in directVolMax) {
            val result = VoiceCommandEngine.parseDirect(phrase)
            assertEquals("Expected VolumeMax for '$phrase'", VoiceCommand.VolumeMax, result.command)
        }

        val directVolMed = listOf("volumen medio", "volumen a la mitad", "a la mitad")
        for (phrase in directVolMed) {
            val result = VoiceCommandEngine.parseDirect(phrase)
            assertEquals("Expected VolumeMedium for '$phrase'", VoiceCommand.VolumeMedium, result.command)
        }

        val directVolLow = listOf("volumen bajo", "volumen bajito")
        for (phrase in directVolLow) {
            val result = VoiceCommandEngine.parseDirect(phrase)
            assertEquals("Expected VolumeLow for '$phrase'", VoiceCommand.VolumeLow, result.command)
        }

        val directMute = listOf("silencio", "mutear", "mute", "silenciar", "sin sonido")
        for (phrase in directMute) {
            val result = VoiceCommandEngine.parseDirect(phrase)
            assertEquals("Expected Mute for '$phrase'", VoiceCommand.Mute, result.command)
        }
    }

    @Test
    fun testWakeWordStrippedFromSearchQuery() {
        val testCases = listOf(
            "Música - eminem" to "eminem",
            "Música eminem" to "eminem",
            "Música: eminem" to "eminem",
            "Música, eminem" to "eminem",
            "Música de eminem" to "eminem",
            "Música pon eminem" to "eminem",
            "Música pon la canción de eminem" to "eminem",
            "Música reproduce eminem" to "eminem",
            "Música busca eminem" to "eminem",
            "Oye música eminem" to "eminem",
            "Oye música pon a eminem" to "eminem",
            "Música Queen" to "queen",
            "Música - Queen Bohemian Rhapsody" to "queen bohemian rhapsody"
        )

        for ((input, expectedQuery) in testCases) {
            val result = VoiceCommandEngine.parse(input, master)
            assertTrue("Expected SearchAndPlay for '$input' but was ${result.command}", result.command is VoiceCommand.SearchAndPlay)
            val query = (result.command as VoiceCommand.SearchAndPlay).songQuery
            assertEquals("Query mismatch for '$input'", expectedQuery, query)
        }

        // Direct parser tests
        val directResult1 = VoiceCommandEngine.parseDirect("Música - eminem")
        assertTrue(directResult1.command is VoiceCommand.SearchAndPlay)
        assertEquals("eminem", (directResult1.command as VoiceCommand.SearchAndPlay).songQuery)

        val directResult2 = VoiceCommandEngine.parseDirect("eminem")
        assertTrue(directResult2.command is VoiceCommand.SearchAndPlay)
        assertEquals("eminem", (directResult2.command as VoiceCommand.SearchAndPlay).songQuery)
    }

    @Test
    fun testProtocolSerialization() {
        val pingMsg = RemoteNodeMessage(
            type = RemoteMessageType.PING,
            senderName = "Mic-Dormitorio"
        )
        val pingJson = pingMsg.toJson()
        val parsed = RemoteNodeMessage.fromJson(pingJson)
        assertNotNull(parsed)
        assertEquals(RemoteMessageType.PING, parsed?.type)
        assertEquals("Mic-Dormitorio", parsed?.senderName)

        val voiceMsg = RemoteNodeMessage(
            type = RemoteMessageType.VOICE_COMMAND,
            senderName = "Mic-Cocina",
            commandType = "SEARCH_PLAY",
            songQuery = "Queen Bohemian Rhapsody",
            rawSpokenText = "Música pon Queen Bohemian Rhapsody"
        )
        val voiceJson = voiceMsg.toJson()
        val parsedVoice = RemoteNodeMessage.fromJson(voiceJson)
        assertNotNull(parsedVoice)
        assertEquals(RemoteMessageType.VOICE_COMMAND, parsedVoice?.type)
        assertEquals("SEARCH_PLAY", parsedVoice?.commandType)
        assertEquals("Queen Bohemian Rhapsody", parsedVoice?.songQuery)
    }
}
