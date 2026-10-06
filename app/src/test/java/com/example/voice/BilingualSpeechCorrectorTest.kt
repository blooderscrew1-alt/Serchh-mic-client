package com.example.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BilingualSpeechCorrectorTest {

    @Test
    fun `test phonetic artist correction with musical context`() {
        val input = "pon musica de kuin"
        val result = BilingualSpeechCorrector.correctWithConfidence(input)

        assertTrue("Expected correction to be applied", result.applied)
        assertTrue("Confidence should be >= 0.75", result.confidence >= 0.75f)
        assertEquals("pon musica de Queen", result.correctedText)
        assertEquals("Queen", result.matchedEntity)
    }

    @Test
    fun `test canonical artist is preserved without alteration`() {
        val input = "pon musica de Queen"
        val result = BilingualSpeechCorrector.correctWithConfidence(input)

        assertFalse("Canonical artist should not be re-corrected", result.applied)
        assertEquals(input, result.correctedText)
        assertEquals(1.0f, result.confidence, 0.01f)
    }

    @Test
    fun `test common Spanish word protection`() {
        val input = "pon rosa"
        val result = BilingualSpeechCorrector.correctWithConfidence(input)

        assertFalse("Common Spanish word should not be transformed to an artist", result.applied)
        assertEquals(input, result.correctedText)
    }

    @Test
    fun `test English song title preserved`() {
        val input = "pon Nothing Else Matters de Metallica"
        val result = BilingualSpeechCorrector.correctWithConfidence(input)

        assertFalse("Known valid canonical entity should not be altered", result.applied)
        assertEquals(input, result.correctedText)
    }

    @Test
    fun `test multi-word phonetic artist correction`() {
        val input = "musica de col plei"
        val result = BilingualSpeechCorrector.correctWithConfidence(input)

        assertTrue(result.applied)
        assertTrue(result.confidence >= 0.75f)
        assertEquals("musica de Coldplay", result.correctedText)
    }

    @Test
    fun `test multi-word phonetic Michael Jackson`() {
        val input = "pon canciones de maicol yacson"
        val result = BilingualSpeechCorrector.correctWithConfidence(input)

        assertTrue(result.applied)
        assertEquals("pon canciones de Michael Jackson", result.correctedText)
    }

    @Test
    fun `test media control word is not distorted`() {
        val input = "pausa"
        val result = BilingualSpeechCorrector.correctWithConfidence(input)

        assertFalse(result.applied)
        assertEquals("pausa", result.correctedText)
    }

    @Test
    fun `test local intent resolution classifies clear commands properly`() {
        val pauseResult = VoiceCommandEngine.parseDirect("pausa")
        assertTrue("Pause must be a clear local command", pauseResult.isClearLocalCommand)
        assertEquals("PAUSE", pauseResult.intent)

        val nextResult = VoiceCommandEngine.parseDirect("siguiente")
        assertTrue("Next track must be a clear local command", nextResult.isClearLocalCommand)
        assertEquals("NEXT_TRACK", nextResult.intent)

        val queenResult = VoiceCommandEngine.parseDirect("pon Queen")
        assertTrue("Simple search must be a clear local command without Gemini", queenResult.isClearLocalCommand)
        assertEquals("SEARCH_AND_PLAY", queenResult.intent)

        val conversationalText = "recomiendame algo parecido a rock de los ochenta"
        val complexResult = VoiceCommandEngine.parseDirect(conversationalText)
        assertFalse("Conversational query should be flagged for Gemini enhancement", complexResult.isClearLocalCommand)
    }
}
