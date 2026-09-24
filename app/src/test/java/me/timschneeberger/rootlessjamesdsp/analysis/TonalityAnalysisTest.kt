package me.timschneeberger.rootlessjamesdsp.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class TonalityAnalysisTest {
    @Test
    fun officialMNoiseHashesVerify() {
        assertTrue(
            MNoiseFileVerifier.isVerifiedMNoise(
                "50BFCD4E5B9B7FCB07A0D34079198D076F978434C2971D80472C4C4ADE66EC15",
                null
            )
        )
        assertTrue(
            MNoiseFileVerifier.isVerifiedMNoise(
                "bad",
                "6539f08317d36216c3e0c37cf68c2b38"
            )
        )
    }

    @Test
    fun fftBandAnalyzerFindsSineBand() {
        val config = TonalityAnalysisConfig(sampleRate = 48_000)
        val layout = TonalityBandLayout.create(config)
        val analyzer = StereoFftBandAnalyzer(config, layout)
        val samples = FloatArray(config.fftSize * 2)
        for (frame in 0 until config.fftSize) {
            val sample = sin(2.0 * PI * 1_000.0 * frame / config.sampleRate).toFloat()
            samples[frame * 2] = sample
            samples[frame * 2 + 1] = sample
        }

        val frames = analyzer.analyzeWholeInterleavedStereo(samples, samples.size)
        assertEquals(1, frames.size)

        val power = frames.first()
        val maxIndex = power.indices.maxBy { power[it] }
        val detectedHz = layout.centerHz[maxIndex]
        assertTrue("detectedHz=$detectedHz", detectedHz in 890f..1125f)
    }
}
