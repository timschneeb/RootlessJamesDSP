package me.timschneeberger.rootlessjamesdsp.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioSampleRateDetectorTest {

    @Test
    fun `detected mixer rates are sorted and retained`() {
        assertEquals(
            listOf(44_100, 48_000, 96_000, 192_000),
            AudioSampleRateDetector.normalizeSampleRates(
                detectedRates = listOf(48_000, 96_000, 44_100, 192_000),
                fallbackRate = 48_000,
            )
        )
    }

    @Test
    fun `native rate is not added to reported mixer capabilities`() {
        assertEquals(
            listOf(96_000),
            AudioSampleRateDetector.normalizeSampleRates(
                detectedRates = listOf(96_000),
                fallbackRate = 48_000,
            )
        )
    }

    @Test
    fun `invalid rates are ignored and default is always available`() {
        assertEquals(
            listOf(48_000),
            AudioSampleRateDetector.normalizeSampleRates(
                detectedRates = listOf(-1, 0, 384_001, 768_000),
                fallbackRate = 0,
            )
        )
    }

    @Test
    fun `every detected rate through 384 kHz is retained`() {
        val detectedRates = listOf(
            8_000, 11_025, 12_000, 16_000, 22_050, 24_000, 32_000,
            44_100, 48_000, 64_000, 88_200, 96_000, 176_400, 192_000, 384_000,
        )

        val result = AudioSampleRateDetector.normalizeSampleRates(
            detectedRates = detectedRates,
            fallbackRate = 48_000,
        )

        assertEquals(detectedRates.toSet(), result.toSet())
    }
}
