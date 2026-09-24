package me.timschneeberger.rootlessjamesdsp.analysis

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * Band-limited resampling (Blackman-windowed sinc). Content above the new Nyquist frequency is
 * filtered out instead of folding back into the audible range, and the top octave keeps its level.
 * Meant for offline use (reference import), not the real-time path.
 */
object PcmResampler {
    fun resampleInterleavedStereo(samples: FloatArray, sourceSampleRate: Int, targetSampleRate: Int): FloatArray {
        if (sourceSampleRate == targetSampleRate) return samples.copyOf()

        val sourceFrames = samples.size / CHANNELS
        if (sourceFrames <= 1) return samples.copyOf()

        val targetFrames = ((sourceFrames.toLong() * targetSampleRate) / sourceSampleRate).toInt().coerceAtLeast(1)
        val ratio = sourceSampleRate.toDouble() / targetSampleRate
        // Cut-off in cycles per source sample, a little below the lower of the two Nyquist frequencies
        val cutoff = 0.5 * min(1.0, 1.0 / ratio) * CUTOFF_MARGIN
        val halfWidth = ceil(ZERO_CROSSINGS / (2.0 * cutoff)).toInt()
        val output = FloatArray(targetFrames * CHANNELS)

        for (frame in 0 until targetFrames) {
            val position = frame * ratio
            val center = floor(position).toInt()
            var left = 0.0
            var right = 0.0
            var norm = 0.0
            for (n in (center - halfWidth + 1)..(center + halfWidth)) {
                if (n < 0 || n >= sourceFrames) continue
                val distance = position - n
                val u = distance / halfWidth
                if (abs(u) >= 1.0) continue
                val window = 0.42 + 0.5 * cos(PI * u) + 0.08 * cos(2 * PI * u)
                val weight = sinc(2.0 * cutoff * distance) * window
                left += samples[n * CHANNELS] * weight
                right += samples[n * CHANNELS + 1] * weight
                norm += weight
            }
            if (norm != 0.0) {
                output[frame * CHANNELS] = (left / norm).toFloat()
                output[frame * CHANNELS + 1] = (right / norm).toFloat()
            }
        }

        return output
    }

    private fun sinc(x: Double): Double = if (abs(x) < 1e-9) 1.0 else sin(PI * x) / (PI * x)

    private const val CHANNELS = 2
    private const val ZERO_CROSSINGS = 16
    private const val CUTOFF_MARGIN = 0.95
}
