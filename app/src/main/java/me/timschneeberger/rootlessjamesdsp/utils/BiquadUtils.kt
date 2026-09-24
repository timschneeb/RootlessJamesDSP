package me.timschneeberger.rootlessjamesdsp.utils

import me.timschneeberger.rootlessjamesdsp.model.ParametricEqBand
import me.timschneeberger.rootlessjamesdsp.model.ParametricEqChannel
import me.timschneeberger.rootlessjamesdsp.model.ParametricEqFilterType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.*

/**
 * Biquad filter coefficient computation based on the
 * Robert Bristow-Johnson Audio EQ Cookbook.
 *
 * All filters are second-order IIR (biquad) and inherently minimum phase.
 */
object BiquadUtils {

    data class BiquadCoefficients(
        val b0: Double, val b1: Double, val b2: Double,
        val a0: Double, val a1: Double, val a2: Double
    )

    /**
     * Compute biquad coefficients for a parametric EQ band.
     *
     * @param frequency Center/corner frequency in Hz
     * @param gain Gain in dB
     * @param q Q factor
     * @param filterType PEAKING, LOW_SHELF, or HIGH_SHELF
     * @param sampleRate Sample rate in Hz
     * @return Biquad coefficients (b0, b1, b2, a0, a1, a2)
     */
    fun computeCoefficients(
        frequency: Double,
        gain: Double,
        q: Double,
        filterType: ParametricEqFilterType,
        sampleRate: Double = 48000.0
    ): BiquadCoefficients {
        val A = 10.0.pow(gain / 40.0) // sqrt of linear gain
        val omega = 2.0 * PI * frequency / sampleRate
        val sinOmega = sin(omega)
        val cosOmega = cos(omega)

        return when (filterType) {
            ParametricEqFilterType.PEAKING -> {
                val alpha = sinOmega / (2.0 * q)
                BiquadCoefficients(
                    b0 = 1.0 + alpha * A,
                    b1 = -2.0 * cosOmega,
                    b2 = 1.0 - alpha * A,
                    a0 = 1.0 + alpha / A,
                    a1 = -2.0 * cosOmega,
                    a2 = 1.0 - alpha / A
                )
            }
            ParametricEqFilterType.LOW_SHELF -> {
                val alpha = sinOmega / (2.0 * q)
                val sqrtA = sqrt(A)
                val twoSqrtAAlpha = 2.0 * sqrtA * alpha
                BiquadCoefficients(
                    b0 = A * ((A + 1.0) - (A - 1.0) * cosOmega + twoSqrtAAlpha),
                    b1 = 2.0 * A * ((A - 1.0) - (A + 1.0) * cosOmega),
                    b2 = A * ((A + 1.0) - (A - 1.0) * cosOmega - twoSqrtAAlpha),
                    a0 = (A + 1.0) + (A - 1.0) * cosOmega + twoSqrtAAlpha,
                    a1 = -2.0 * ((A - 1.0) + (A + 1.0) * cosOmega),
                    a2 = (A + 1.0) + (A - 1.0) * cosOmega - twoSqrtAAlpha
                )
            }
            ParametricEqFilterType.HIGH_SHELF -> {
                val alpha = sinOmega / (2.0 * q)
                val sqrtA = sqrt(A)
                val twoSqrtAAlpha = 2.0 * sqrtA * alpha
                BiquadCoefficients(
                    b0 = A * ((A + 1.0) + (A - 1.0) * cosOmega + twoSqrtAAlpha),
                    b1 = -2.0 * A * ((A - 1.0) + (A + 1.0) * cosOmega),
                    b2 = A * ((A + 1.0) + (A - 1.0) * cosOmega - twoSqrtAAlpha),
                    a0 = (A + 1.0) - (A - 1.0) * cosOmega + twoSqrtAAlpha,
                    a1 = 2.0 * ((A - 1.0) - (A + 1.0) * cosOmega),
                    a2 = (A + 1.0) - (A - 1.0) * cosOmega - twoSqrtAAlpha
                )
            }
        }
    }

    /**
     * Compute the magnitude response |H(e^jw)| in dB at a given frequency.
     *
     * Evaluates H(z) = (b0 + b1*z^-1 + b2*z^-2) / (a0 + a1*z^-1 + a2*z^-2)
     * at z = e^(j*2*pi*f/fs).
     */
    fun magnitudeResponse(
        coeffs: BiquadCoefficients,
        frequency: Double,
        sampleRate: Double = 48000.0
    ): Double {
        val omega = 2.0 * PI * frequency / sampleRate
        val cosW = cos(omega)
        val cos2W = cos(2.0 * omega)
        val sinW = sin(omega)
        val sin2W = sin(2.0 * omega)

        // Numerator: b0 + b1*e^(-jw) + b2*e^(-j2w)
        val numReal = coeffs.b0 + coeffs.b1 * cosW + coeffs.b2 * cos2W
        val numImag = -(coeffs.b1 * sinW + coeffs.b2 * sin2W)

        // Denominator: a0 + a1*e^(-jw) + a2*e^(-j2w)
        val denReal = coeffs.a0 + coeffs.a1 * cosW + coeffs.a2 * cos2W
        val denImag = -(coeffs.a1 * sinW + coeffs.a2 * sin2W)

        val numMagSq = numReal * numReal + numImag * numImag
        val denMagSq = denReal * denReal + denImag * denImag

        return if (denMagSq > 0.0) {
            10.0 * log10(numMagSq / denMagSq)
        } else {
            0.0
        }
    }

    /**
     * Compute the combined magnitude response of multiple parametric EQ bands
     * sampled at logarithmically-spaced frequency points.
     *
     * @param bands List of parametric EQ bands
     * @param numPoints Number of sample points (default 512)
     * @param minFreq Minimum frequency in Hz
     * @param maxFreq Maximum frequency in Hz
     * @param sampleRate Sample rate for coefficient computation
     * @return List of (frequency, totalGainDb) pairs
     */
    fun computeCombinedResponse(
        bands: List<ParametricEqBand>,
        numPoints: Int = 512,
        minFreq: Double = 20.0,
        maxFreq: Double = 20000.0,
        sampleRate: Double = 48000.0,
        channel: ParametricEqChannel? = null
    ): List<Pair<Double, Double>> {
        val filteredBands = when (channel) {
            null -> bands
            ParametricEqChannel.LEFT ->
                bands.filter { it.channel == ParametricEqChannel.LEFT_RIGHT || it.channel == ParametricEqChannel.LEFT }
            ParametricEqChannel.RIGHT ->
                bands.filter { it.channel == ParametricEqChannel.LEFT_RIGHT || it.channel == ParametricEqChannel.RIGHT }
            ParametricEqChannel.LEFT_RIGHT ->
                bands.filter { it.channel == ParametricEqChannel.LEFT_RIGHT }
        }

        if (filteredBands.isEmpty()) return emptyList()

        val logMin = ln(minFreq)
        val logMax = ln(maxFreq)
        val result = ArrayList<Pair<Double, Double>>(numPoints)

        // Precompute coefficients for all bands
        val allCoeffs = filteredBands.map { band ->
            computeCoefficients(band.frequency, band.gain, band.q, band.filterType, sampleRate)
        }

        for (i in 0 until numPoints) {
            val t = i.toDouble() / (numPoints - 1).toDouble()
            val freq = exp(logMin + t * (logMax - logMin))
            var totalGain = 0.0

            for (coeffs in allCoeffs) {
                totalGain += magnitudeResponse(coeffs, freq, sampleRate)
            }

            result.add(Pair(freq, totalGain))
        }

        return result
    }

    fun computeAverageStereoResponse(
        bands: List<ParametricEqBand>,
        numPoints: Int = 512,
        minFreq: Double = 20.0,
        maxFreq: Double = 20000.0,
        sampleRate: Double = 48000.0
    ): List<Pair<Double, Double>> {
        val left = computeCombinedResponse(
            bands,
            numPoints = numPoints,
            minFreq = minFreq,
            maxFreq = maxFreq,
            sampleRate = sampleRate,
            channel = ParametricEqChannel.LEFT
        )
        val right = computeCombinedResponse(
            bands,
            numPoints = numPoints,
            minFreq = minFreq,
            maxFreq = maxFreq,
            sampleRate = sampleRate,
            channel = ParametricEqChannel.RIGHT
        )

        if (left.isEmpty() && right.isEmpty()) return emptyList()

        val normalizedLeft = if (left.isEmpty()) right.map { it.first to 0.0 } else left
        val normalizedRight = if (right.isEmpty()) left.map { it.first to 0.0 } else right

        return normalizedLeft.indices.map { index ->
            normalizedLeft[index].first to ((normalizedLeft[index].second + normalizedRight[index].second) * 0.5)
        }
    }

    private val dfFreq = DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
    private val dfGain = DecimalFormat("0.000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH))

    /**
     * Convert a sampled frequency response to GraphicEQ format string
     * suitable for the native ArbitraryResponseEqualizer.
     *
     * @param response List of (frequency, gainDb) pairs
     * @param preampOffset Additional gain offset in dB (e.g. negative preamp to prevent clipping)
     */
    fun toGraphicEqString(response: List<Pair<Double, Double>>, preampOffset: Double = 0.0): String {
        val sb = StringBuilder("GraphicEQ: ")
        for ((freq, gain) in response) {
            sb.append("${dfFreq.format(freq)} ${dfGain.format(gain + preampOffset)}; ")
        }
        return sb.toString()
    }
}
