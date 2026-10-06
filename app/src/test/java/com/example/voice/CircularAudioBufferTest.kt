package com.example.voice

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CircularAudioBufferTest {

    @Test
    fun `test buffer write within capacity`() {
        val buffer = CircularAudioBuffer(10)
        val data = shortArrayOf(1, 2, 3, 4, 5)
        buffer.write(data, data.size)

        assertEquals(5, buffer.available())
        assertArrayEquals(data, buffer.readAll())
    }

    @Test
    fun `test circular overwrite when exceeding capacity`() {
        val buffer = CircularAudioBuffer(5)
        val data1 = shortArrayOf(1, 2, 3)
        buffer.write(data1, data1.size)

        val data2 = shortArrayOf(4, 5, 6, 7)
        buffer.write(data2, data2.size)

        // Capacity is 5, wrote 7 items total, so the last 5 items should be [3, 4, 5, 6, 7]
        assertEquals(5, buffer.available())
        assertArrayEquals(shortArrayOf(3, 4, 5, 6, 7), buffer.readAll())
    }

    @Test
    fun `test buffer clear`() {
        val buffer = CircularAudioBuffer(8)
        buffer.write(shortArrayOf(10, 20, 30), 3)
        assertEquals(3, buffer.available())
        buffer.clear()
        assertEquals(0, buffer.available())
        assertArrayEquals(shortArrayOf(), buffer.readAll())
    }

    @Test
    fun `test pre roll simulation 650ms at 16kHz`() {
        val capacity = (16000 * 650) / 1000 // 10,400 samples
        val buffer = CircularAudioBuffer(capacity)

        // Write 20,000 samples in chunks of 320 (20ms frames)
        val frame = ShortArray(320) { it.toShort() }
        repeat(20000 / 320) {
            buffer.write(frame, frame.size)
        }

        assertEquals(capacity, buffer.available())
        val retrieved = buffer.readAll()
        assertEquals(capacity, retrieved.size)
    }
}
