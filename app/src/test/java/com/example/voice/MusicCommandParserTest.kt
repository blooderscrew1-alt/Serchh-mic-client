package com.example.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicCommandParserTest {

    @Test
    fun `test parse Spanish search and play command`() {
        val input = "Oye Música, pon Blinding Lights de The Weeknd"
        val result = MusicCommandParser.parse(input)

        assertEquals("poner", result.action)
        assertEquals("Blinding Lights de The Weeknd", result.searchQuery)
        assertEquals("The Weeknd", result.artist)
        assertEquals("Blinding Lights", result.track)
        assertFalse(result.isMediaControl)
    }

    @Test
    fun `test parse Spanish Bad Bunny request`() {
        val input = "Reproduce música de Bad Bunny"
        val result = MusicCommandParser.parse(input)

        assertEquals("reproducir", result.action)
        assertEquals("Bad Bunny", result.searchQuery)
        assertFalse(result.isMediaControl)
    }

    @Test
    fun `test parse English play request`() {
        val input = "Hey Player, play Starboy by The Weeknd"
        val result = MusicCommandParser.parse(input)

        assertEquals("play", result.action)
        assertEquals("Starboy by The Weeknd", result.searchQuery)
        assertEquals("The Weeknd", result.artist)
        assertEquals("Starboy", result.track)
        assertFalse(result.isMediaControl)
    }

    @Test
    fun `test parse pause and resume media controls`() {
        val pauseResult = MusicCommandParser.parse("Oye Música, pausa")
        assertTrue(pauseResult.isMediaControl)
        assertEquals(MediaControlType.PAUSE, pauseResult.controlType)

        val resumeResult = MusicCommandParser.parse("Hey Player, resume")
        assertTrue(resumeResult.isMediaControl)
        assertEquals(MediaControlType.PLAY_RESUME, resumeResult.controlType)
    }

    @Test
    fun `test parse next and previous track`() {
        val nextResult = MusicCommandParser.parse("siguiente canción")
        assertTrue(nextResult.isMediaControl)
        assertEquals(MediaControlType.NEXT_TRACK, nextResult.controlType)

        val prevResult = MusicCommandParser.parse("anterior canción")
        assertTrue(prevResult.isMediaControl)
        assertEquals(MediaControlType.PREVIOUS_TRACK, prevResult.controlType)
    }

    @Test
    fun `test parse volume controls`() {
        val volUp = MusicCommandParser.parse("sube el volumen")
        assertTrue(volUp.isMediaControl)
        assertEquals(MediaControlType.VOLUME_UP, volUp.controlType)

        val volDown = MusicCommandParser.parse("baja el volumen")
        assertTrue(volDown.isMediaControl)
        assertEquals(MediaControlType.VOLUME_DOWN, volDown.controlType)

        val mute = MusicCommandParser.parse("silencio")
        assertTrue(mute.isMediaControl)
        assertEquals(MediaControlType.MUTE, mute.controlType)
    }
}
