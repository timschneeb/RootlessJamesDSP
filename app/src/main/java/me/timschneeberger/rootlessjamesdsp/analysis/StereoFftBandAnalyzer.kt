package me.timschneeberger.rootlessjamesdsp.analysis

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

class StereoFftBandAnalyzer(
    private val config: TonalityAnalysisConfig,
    private val layout: TonalityBandLayout = TonalityBandLayout.create(config)
) {
    private val leftWindow = FloatArray(config.fftSize)
    private val rightWindow = FloatArray(config.fftSize)
    private val hann = FloatArray(config.fftSize) { i ->
        (0.5 - 0.5 * cos(2.0 * PI * i / (config.fftSize - 1))).toFloat()
    }
    private val fft = Radix2Fft(config.fftSize)
    private var filledFrames = 0

    // Reused for every hop, so real-time analysis doesn't allocate
    private val leftReal = DoubleArray(config.fftSize)
    private val leftImag = DoubleArray(config.fftSize)
    private val rightReal = DoubleArray(config.fftSize)
    private val rightImag = DoubleArray(config.fftSize)
    private val bandPower = FloatArray(layout.centerHz.size)

    /**
     * Feeds interleaved stereo samples; [onBandPower] receives the mean power per band of every
     * completed FFT frame. The array is reused, so consume or copy it inside the callback.
     */
    fun offerInterleavedStereo(samples: FloatArray, samplesRead: Int, onBandPower: (FloatArray) -> Unit) {
        val usableSamples = samplesRead - (samplesRead % CHANNELS)
        var sampleIndex = 0
        while (sampleIndex < usableSamples) {
            if (pushFrame(samples[sampleIndex], samples[sampleIndex + 1]))
                onBandPower(bandPower)
            sampleIndex += CHANNELS
        }
    }

    fun offerInterleavedStereo(samples: FloatArray, samplesRead: Int): List<FloatArray> {
        val frames = ArrayList<FloatArray>()
        offerInterleavedStereo(samples, samplesRead) { frames += it.copyOf() }
        return frames
    }

    fun analyzeWholeInterleavedStereo(samples: FloatArray, samplesRead: Int): List<FloatArray> {
        reset()
        return offerInterleavedStereo(samples, samplesRead)
    }

    /** Returns true when a frame completed and [bandPower] holds its result. */

    private fun pushFrame(left: Float, right: Float): Boolean {
        leftWindow[filledFrames] = left
        rightWindow[filledFrames] = right
        if (++filledFrames < config.fftSize)
            return false
        computeBandPower()
        shiftByHop()
        return true
    }

    fun reset() {
        filledFrames = 0
        leftWindow.fill(0f)
        rightWindow.fill(0f)
    }

    private fun shiftByHop() {
        val remaining = config.fftSize - config.hopSize
        leftWindow.copyInto(leftWindow, destinationOffset = 0, startIndex = config.hopSize, endIndex = config.fftSize)
        rightWindow.copyInto(rightWindow, destinationOffset = 0, startIndex = config.hopSize, endIndex = config.fftSize)
        leftWindow.fill(0f, remaining, config.fftSize)
        rightWindow.fill(0f, remaining, config.fftSize)
        filledFrames = remaining
    }

    private fun computeBandPower() {
        for (i in 0 until config.fftSize) {
            leftReal[i] = leftWindow[i] * hann[i].toDouble()
            rightReal[i] = rightWindow[i] * hann[i].toDouble()
        }
        leftImag.fill(0.0)
        rightImag.fill(0.0)

        fft.transform(leftReal, leftImag)
        fft.transform(rightReal, rightImag)

        for (band in layout.centerHz.indices) {
            var sum = 0.0
            var count = 0
            var bin = layout.binStart[band]
            while (bin < layout.binEndExclusive[band]) {
                val leftPower = leftReal[bin] * leftReal[bin] + leftImag[bin] * leftImag[bin]
                val rightPower = rightReal[bin] * rightReal[bin] + rightImag[bin] * rightImag[bin]
                sum += 0.5 * (leftPower + rightPower)
                ++count
                ++bin
            }
            bandPower[band] = (sum / max(1, count)).toFloat()
        }
    }

    companion object {
        private const val CHANNELS = 2
        const val POWER_FLOOR = 1.0e-20f

        fun powerToDb(power: Float): Float {
            return 10f * log10(power.coerceAtLeast(POWER_FLOOR))
        }

        fun rms(values: FloatArray): Float {
            var sum = 0.0
            for (value in values) {
                sum += value * value.toDouble()
            }
            return sqrt(sum / values.size).toFloat()
        }
    }
}

private class Radix2Fft(private val size: Int) {
    init {
        require(size > 0 && size and (size - 1) == 0) { "FFT size must be a power of two" }
    }

    fun transform(real: DoubleArray, imag: DoubleArray) {
        var j = 0
        for (i in 1 until size) {
            var bit = size shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tempReal = real[i]
                real[i] = real[j]
                real[j] = tempReal
                val tempImag = imag[i]
                imag[i] = imag[j]
                imag[j] = tempImag
            }
        }

        var length = 2
        while (length <= size) {
            val angle = -2.0 * PI / length
            val wLengthReal = cos(angle)
            val wLengthImag = kotlin.math.sin(angle)
            var i = 0
            while (i < size) {
                var wReal = 1.0
                var wImag = 0.0
                val half = length / 2
                for (k in 0 until half) {
                    val even = i + k
                    val odd = even + half
                    val oddReal = real[odd] * wReal - imag[odd] * wImag
                    val oddImag = real[odd] * wImag + imag[odd] * wReal

                    real[odd] = real[even] - oddReal
                    imag[odd] = imag[even] - oddImag
                    real[even] += oddReal
                    imag[even] += oddImag

                    val nextReal = wReal * wLengthReal - wImag * wLengthImag
                    wImag = wReal * wLengthImag + wImag * wLengthReal
                    wReal = nextReal
                }
                i += length
            }
            length = length shl 1
        }
    }
}
