package me.timschneeberger.rootlessjamesdsp.androideq

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Maps an EQ curve onto the band gains of Android's DynamicsProcessing effect.
 *
 * DynamicsProcessing (AOSP DPFrequency) works on FFT blocks of [blockSize] samples with 50% overlap
 * and sqrt-Hann windows. Each band multiplies its FFT bins by one gain, so the result is a step
 * function over bins, smoothed by the window. The pre-EQ, MBC (as a static gain) and post-EQ stages
 * give up to 3 x 128 bands. Band gains are fitted by least squares against the effect's actual
 * response, with a curvature penalty that keeps block artifacts at the level of plain band averages.
 */
class AndroidEqFitter(val sampleRate: Int, val blockSize: Int) {

    data class Band(val cutoffHz: Float, val gainDb: Float)

    data class Stages(val preEq: List<Band>, val mbc: List<Band>, val postEq: List<Band>)

    private val half = blockSize / 2
    private val binWidth = 2 * PI / blockSize

    /** Upper bin of each segment (a run of bins sharing one gain). */
    val edges: IntArray = segmentEdges(half, MAX_SEGMENTS)
    private val segmentOfBin = IntArray(half).also { seg ->
        var start = 0
        edges.forEachIndexed { s, stop ->
            for (k in start..stop) seg[k] = s
            start = stop + 1
        }
    }

    private val gridFreqs = DoubleArray(GRID_POINTS) { i ->
        val top = min(20000.0, 0.45 * sampleRate)
        exp(ln(20.0) + i * (ln(top) - ln(20.0)) / (GRID_POINTS - 1))
    }

    // Spectrum of the window lag product, tabulated over the few bins where it is not negligible
    private val kernelStep = binWidth / KERNEL_OVERSAMPLE
    private val kernel: DoubleArray = run {
        val window = DoubleArray(blockSize) { i -> sqrt(0.5 * (1 - cos(2 * PI * i / (blockSize - 1)))) }
        val hop = blockSize / 2.0
        val lag = DoubleArray(blockSize) { d ->
            var sum = 0.0
            for (i in 0 until blockSize - d) sum += window[i] * window[i + d]
            sum / hop
        }
        DoubleArray(KERNEL_BINS * KERNEL_OVERSAMPLE + 2) { j ->
            val x = j * kernelStep
            var sum = lag[0]
            for (d in 1 until blockSize) sum += 2 * lag[d] * cos(x * d)
            sum
        }
    }

    private fun kernelAt(x: Double): Double {
        var y = abs(x) % (2 * PI)
        if (y > PI) y = 2 * PI - y
        val pos = y / kernelStep
        val i = floor(pos).toInt()
        if (i >= kernel.size - 1) return 0.0
        val t = pos - i
        return kernel[i] * (1 - t) + kernel[i + 1] * t
    }

    /** Contribution of FFT bin [k] with unit gain to the response at angular frequency [w]. */
    private fun binResponse(k: Int, w: Double): Double {
        val a = if (k == 0 || k == half) 1.0 else 2.0
        val theta = k * binWidth
        return a / (2 * blockSize) * (kernelAt(w - theta) + kernelAt(w + theta))
    }

    /** Linear magnitude of the effect at [freq] for per-bin gains [binGains] (Nyquist stays at unity). */
    fun response(binGains: DoubleArray, freq: Double): Double {
        val w = 2 * PI * freq / sampleRate
        val centre = (w / binWidth).roundToInt()
        var h = binResponse(half, w)
        for (k in max(0, centre - KERNEL_BINS)..min(half - 1, centre + KERNEL_BINS))
            h += binGains[k] * binResponse(k, w)
        return h
    }

    /** Per-bin linear gains for fitted segment gains in dB. */
    fun binGains(segmentGainsDb: DoubleArray) =
        DoubleArray(half) { k -> 10.0.pow(segmentGainsDb[segmentOfBin[k]] / 20) }

    /** Segment gains in dB that best reproduce [targetDb] (a function of frequency in Hz). */
    fun fitSegments(targetDb: (Double) -> Double): DoubleArray {
        val m = edges.size

        // Plain band averages, used for scaling and as the curvature reference
        val sums = DoubleArray(m)
        val counts = IntArray(m)
        for (k in 0 until half) {
            val s = segmentOfBin[k]
            sums[s] += targetDb(max(k.toDouble() * sampleRate / blockSize, 1.0))
            counts[s]++
        }
        val reference = DoubleArray(m) { s -> 10.0.pow(sums[s] / counts[s] / 20) }

        // Normal equations of min |A g - b|^2 + lambda^2 |D2 (g / reference)|^2, rows relative to target
        val normal = Array(m) { DoubleArray(m) }
        val rhs = DoubleArray(m)
        val rowSegments = IntArray(m)
        val rowValues = DoubleArray(m)
        for (freq in gridFreqs) {
            val target = 10.0.pow(targetDb(freq) / 20)
            val w = 2 * PI * freq / sampleRate
            val centre = (w / binWidth).roundToInt()
            var used = 0
            for (k in max(0, centre - KERNEL_BINS)..min(half - 1, centre + KERNEL_BINS)) {
                val s = segmentOfBin[k]
                val v = binResponse(k, w) / target
                val slot = (0 until used).firstOrNull { rowSegments[it] == s }
                if (slot != null) rowValues[slot] += v
                else { rowSegments[used] = s; rowValues[used] = v; used++ }
            }
            val b = 1 - binResponse(half, w) / target
            for (i in 0 until used) {
                rhs[rowSegments[i]] += rowValues[i] * b
                for (j in 0 until used) normal[rowSegments[i]][rowSegments[j]] += rowValues[i] * rowValues[j]
            }
        }
        val l2 = LAMBDA * LAMBDA
        for (j in 0 until m - 2) {
            val idx = intArrayOf(j, j + 1, j + 2)
            val coeff = doubleArrayOf(1 / reference[j], -2 / reference[j + 1], 1 / reference[j + 2])
            for (p in 0..2) for (q in 0..2) normal[idx[p]][idx[q]] += l2 * coeff[p] * coeff[q]
        }

        val gains = choleskySolve(normal, rhs)
        return DoubleArray(m) { s -> 20 * log10(gains[s].coerceIn(1e-3, 1e3)) }
    }

    /** Splits fitted segments over the three DynamicsProcessing stages (128 bands each). */
    fun fit(targetDb: (Double) -> Double): Stages = toStages(fitSegments(targetDb))

    fun toStages(segmentGainsDb: DoubleArray): Stages {
        val nyquist = sampleRate / 2f
        fun cutoff(edge: Int) = ((edge + 0.25) * sampleRate / blockSize).toFloat()
        fun stage(from: Int, to: Int): List<Band> {
            val bands = ArrayList<Band>(STAGE_BANDS)
            if (from > 0 && from < edges.size) bands += Band(cutoff(edges[from - 1]), 0f)
            for (s in from until min(to, edges.size)) bands += Band(cutoff(edges[s]), segmentGainsDb[s].toFloat())
            if (bands.isEmpty() || to < edges.size) bands += Band(nyquist, 0f)
            return bands
        }
        val first = STAGE_BANDS - 1
        val second = first + STAGE_BANDS - 2
        return Stages(stage(0, first), stage(first, second), stage(second, edges.size))
    }

    companion object {
        const val STAGE_BANDS = 128
        const val MAX_SEGMENTS = 3 * STAGE_BANDS - 4
        private const val GRID_POINTS = 700
        private const val KERNEL_BINS = 10
        private const val KERNEL_OVERSAMPLE = 64
        private const val LAMBDA = 30.0

        /** One bin per segment at the bottom, log-spaced above, at most [maxSegments] segments. */
        fun segmentEdges(half: Int, maxSegments: Int): IntArray {
            if (half <= maxSegments) return IntArray(half) { it }
            var best = IntArray(0)
            for (step in 0 until 8000) {
                val base = 1.0 + step * (199.0 / 7999)
                val logged = (0 until maxSegments).map { i ->
                    exp(ln(base) + i * (ln(half - 1.0) - ln(base)) / (maxSegments - 1)).roundToInt()
                }
                val edges = ((0 until logged.first()) + logged).distinct().sorted()
                if (edges.size <= maxSegments) best = edges.toIntArray() else break
            }
            return best
        }

        private fun choleskySolve(a: Array<DoubleArray>, b: DoubleArray): DoubleArray {
            val n = b.size
            val l = Array(n) { DoubleArray(n) }
            for (i in 0 until n) {
                for (j in 0..i) {
                    var sum = a[i][j]
                    for (k in 0 until j) sum -= l[i][k] * l[j][k]
                    l[i][j] = if (i == j) sqrt(max(sum, 1e-12)) else sum / l[j][j]
                }
            }
            val y = DoubleArray(n)
            for (i in 0 until n) {
                var sum = b[i]
                for (k in 0 until i) sum -= l[i][k] * y[k]
                y[i] = sum / l[i][i]
            }
            val x = DoubleArray(n)
            for (i in n - 1 downTo 0) {
                var sum = y[i]
                for (k in i + 1 until n) sum -= l[k][i] * x[k]
                x[i] = sum / l[i][i]
            }
            return x
        }
    }
}
