package com.example.voice

/**
 * Circular ring buffer for Short PCM audio samples.
 * Used for PRE-ROLL audio buffering in voice activity detection to prevent losing
 * initial phonemes/words before the VAD threshold is confirmed.
 */
class CircularAudioBuffer(val capacity: Int) {
    private val buffer = ShortArray(capacity)
    private var writeHead = 0
    private var available = 0

    @Synchronized
    fun write(samples: ShortArray, count: Int) {
        val toWrite = count.coerceAtMost(samples.size)
        for (i in 0 until toWrite) {
            buffer[writeHead] = samples[i]
            writeHead = (writeHead + 1) % capacity
            if (available < capacity) {
                available++
            }
        }
    }

    @Synchronized
    fun readAll(): ShortArray {
        val result = ShortArray(available)
        val readHead = (writeHead - available + capacity) % capacity
        for (i in 0 until available) {
            result[i] = buffer[(readHead + i) % capacity]
        }
        return result
    }

    @Synchronized
    fun available(): Int = available

    @Synchronized
    fun clear() {
        writeHead = 0
        available = 0
    }
}
