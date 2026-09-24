package me.timschneeberger.rootlessjamesdsp.latency

import android.media.AudioTrack
import kotlin.math.min

/**
 * Keeps the output track's unplayed queue at or below [maxQueueFrames].
 *
 * Once the output queue fills up (slow start, a stall), the blocking write paces the loop and the
 * extra latency never drains. When the queue would exceed the target, the current block fades out,
 * the next one is dropped and the one after fades in. Every block is still processed, so the DSP
 * state stays continuous. [maxQueueFrames] = 0 disables it.
 */
class QueueController(private val maxQueueFrames: Int) {
    private var framesWritten = 0L
    private var dropNext = false
    private var fadeInNext = false

    val enabled get() = maxQueueFrames > 0

    fun onTrackStarted() {
        framesWritten = 0
        dropNext = false
        fadeInNext = false
    }

    /** True if this block must be discarded instead of written. */
    fun shouldDrop(): Boolean {
        if (!dropNext) return false
        dropNext = false
        fadeInNext = true
        return true
    }

    fun onWritten(samplesWritten: Int) {
        if (samplesWritten > 0) framesWritten += samplesWritten / 2
    }

    fun shape(track: AudioTrack, buffer: FloatArray, samples: Int) {
        if (!enabled) return
        val frames = samples / 2
        if (fadeInNext) {
            fadeInNext = false
            val n = min(FADE_FRAMES, frames)
            for (f in 0 until n) {
                val g = f.toFloat() / n
                buffer[2 * f] *= g
                buffer[2 * f + 1] *= g
            }
        }
        if (overTarget(track, frames)) {
            dropNext = true
            val n = min(FADE_FRAMES, frames)
            for (k in 0 until n) {
                val f = frames - n + k
                val g = 1f - (k + 1).toFloat() / n
                buffer[2 * f] *= g
                buffer[2 * f + 1] *= g
            }
        }
    }

    fun shape(track: AudioTrack, buffer: ShortArray, samples: Int) {
        if (!enabled) return
        val frames = samples / 2
        if (fadeInNext) {
            fadeInNext = false
            val n = min(FADE_FRAMES, frames)
            for (f in 0 until n) {
                val g = f.toFloat() / n
                buffer[2 * f] = (buffer[2 * f] * g).toInt().toShort()
                buffer[2 * f + 1] = (buffer[2 * f + 1] * g).toInt().toShort()
            }
        }
        if (overTarget(track, frames)) {
            dropNext = true
            val n = min(FADE_FRAMES, frames)
            for (k in 0 until n) {
                val f = frames - n + k
                val g = 1f - (k + 1).toFloat() / n
                buffer[2 * f] = (buffer[2 * f] * g).toInt().toShort()
                buffer[2 * f + 1] = (buffer[2 * f + 1] * g).toInt().toShort()
            }
        }
    }

    private fun overTarget(track: AudioTrack, blockFrames: Int): Boolean {
        // Frames handed to the track that its mixer has not consumed yet; the head wraps as a uint32
        val head = track.playbackHeadPosition.toLong() and 0xFFFFFFFFL
        val queued = framesWritten - head
        return queued + blockFrames > maxQueueFrames
    }

    companion object {
        private const val FADE_FRAMES = 96    // 2 ms at 48 kHz
    }
}
