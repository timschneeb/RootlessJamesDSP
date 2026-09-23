package me.timschneeberger.rootlessjamesdsp.androideq

import me.timschneeberger.rootlessjamesdsp.model.ParametricEqBand
import me.timschneeberger.rootlessjamesdsp.model.ParametricEqFilterType
import me.timschneeberger.rootlessjamesdsp.utils.BiquadUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sqrt

class AndroidEqFitterTest {

    private val peq = listOf(
        ParametricEqBand(105.0, 5.5, 0.7, ParametricEqFilterType.LOW_SHELF),
        ParametricEqBand(45.0, -3.0, 2.0),
        ParametricEqBand(200.0, -2.5, 1.0),
        ParametricEqBand(700.0, 1.5, 1.4),
        ParametricEqBand(1400.0, 2.0, 1.5),
        ParametricEqBand(3000.0, -4.0, 3.0),
        ParametricEqBand(5800.0, 3.5, 4.0),
        ParametricEqBand(8500.0, -5.0, 5.0),
        ParametricEqBand(12000.0, 2.0, 2.0),
        ParametricEqBand(10000.0, -3.0, 0.7, ParametricEqFilterType.HIGH_SHELF),
    )
    private val coeffs = peq.map { BiquadUtils.computeCoefficients(it.frequency, it.gain, it.q, it.filterType) }
    private val target = { f: Double -> coeffs.sumOf { BiquadUtils.magnitudeResponse(it, f) } }

    private fun errors(blockSize: Int): Pair<Double, Double> {
        val fitter = AndroidEqFitter(48000, blockSize)
        val gains = fitter.binGains(fitter.fitSegments(target))
        val errs = (0 until 700).map { i ->
            val f = exp(ln(20.0) + i * (ln(20000.0) - ln(20.0)) / 699)
            20 * log10(fitter.response(gains, f)) - target(f)
        }
        return errs.maxOf { abs(it) } to sqrt(errs.sumOf { it * it } / errs.size)
    }

    @Test
    fun fitMatchesSimulationAt1024() {
        val (max, rms) = errors(1024)
        assertTrue("max error $max dB", max < 1.05)
        assertTrue("rms error $rms dB", rms < 0.32)
    }

    @Test
    fun largerBlockIsMoreAccurate() {
        assertTrue(errors(2048).first < errors(512).first)
    }

    @Test
    fun stagesFitDynamicsProcessingLimits() {
        for (n in intArrayOf(512, 1024, 2048)) {
            val stages = AndroidEqFitter(48000, n).fit(target)
            for (stage in listOf(stages.preEq, stages.mbc, stages.postEq)) {
                assertTrue(stage.size in 1..AndroidEqFitter.STAGE_BANDS)
                assertTrue(stage.zipWithNext().all { (a, b) -> b.cutoffHz > a.cutoffHz })
            }
        }
    }

    @Test
    fun cutoffsLandOnTheirBins() {
        val n = 1024
        val fitter = AndroidEqFitter(48000, n)
        val stages = fitter.fit(target)
        val stops = (stages.preEq.dropLast(1) + stages.mbc.drop(1).dropLast(1) + stages.postEq.drop(1))
            .map { (0.5 + it.cutoffHz * n / 48000).toInt() }
        assertEquals(fitter.edges.toList(), stops)
    }

    @Test
    fun kernelResponseMatchesDirectSum() {
        // Unit gain on a single bin, evaluated with the full lag sum
        val n = 512
        val fitter = AndroidEqFitter(48000, n)
        val gains = DoubleArray(n / 2).also { it[20] = 1.0 }
        val window = DoubleArray(n) { i -> sqrt(0.5 * (1 - cos(2 * PI * i / (n - 1)))) }
        for (f in doubleArrayOf(1800.0, 1875.0, 1950.0)) {
            val w = 2 * PI * f / 48000
            var h = 0.0
            for (d in -(n - 1) until n) {
                var lag = 0.0
                for (i in 0 until n - abs(d)) lag += window[i] * window[i + abs(d)]
                h += lag / (n / 2) * (2.0 / n) * cos(2 * PI * 20 * d / n) * cos(w * d)
            }
            val nyquist = fitter.response(DoubleArray(n / 2), f)
            assertEquals(h, fitter.response(gains, f) - nyquist, 1e-4 + abs(h) * 5e-4)
        }
    }
}
