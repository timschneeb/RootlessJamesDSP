package me.timschneeberger.rootlessjamesdsp.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ln

class ReferenceCurvesTest {

    private fun pink(hz: Float) = -3f * ln(hz / 1_000f) / ln(2f)

    private fun at(reference: TonalityReference, hz: Float): Float {
        val i = reference.frequencyHz.indices.minBy { kotlin.math.abs(reference.frequencyHz[it] - hz) }
        return reference.referenceDb[i]
    }

    @Test
    fun mNoiseFollowsMeasuredShape() {
        fun db(hz: Float) = TonalityReferenceFactory.mNoiseDb(hz)
        // M-Noise measured through the analyzer, relative to 1 kHz
        val measured = mapOf(125f to 10.8f, 500f to 4.3f, 4_000f to -10.5f, 8_000f to -15.6f, 16_000f to -21.4f)
        for ((hz, expected) in measured)
            assertEquals("$hz Hz", expected, db(hz) - db(1_000f), 1.0f)
        assertEquals(-3f, db(100f) - db(50f), 0.2f)
        assertEquals(-5.44f, db(16_000f) - db(8_000f), 0.2f)
    }

    @Test
    fun iecNoiseMatchesTheStandardTable() {
        // Third-octave levels relative to pink noise from IEC 60268-1
        val table = mapOf(20f to -13.4f, 40f to -5.2f, 100f to -0.9f, 1_000f to -0.1f, 4_000f to -3.7f, 10_000f to -11.9f, 20_000f to -21.6f)
        for ((hz, expected) in table) {
            // The preferred center frequencies are rounded, so compare at the exact third-octave points
            val exact = 20f * Math.pow(2.0, Math.round(3 * ln(hz / 20f) / ln(2f)) / 3.0).toFloat()
            assertEquals("$hz Hz", expected, TonalityReferenceFactory.iecProgrammeNoiseDb(exact) - pink(exact), 0.05f)
        }
    }

    @Test
    fun tiltHasTheChosenSlope() {
        val tilt = TonalityReferenceFactory.create(TonalityReferenceKind.Tilt, 48_000, -4.5f)
        assertEquals(-4.5f * 4, at(tilt, 16_000f) - at(tilt, 1_000f), 0.3f)
        assertEquals("-4.5 dB/oct tilt", tilt.displayLabel)
    }

    @Test
    fun musicAveragesAreNormalizedAt1kHzAndOrderedByPercussion() {
        val kinds = listOf(TonalityReferenceKind.MusicLowPercussion, TonalityReferenceKind.MusicAverage, TonalityReferenceKind.MusicHighPercussion)
        val refs = kinds.map { TonalityReferenceFactory.create(it, 48_000) }
        for (ref in refs) {
            assertEquals(0f, at(ref, 1_000f), 0.3f)
            assertEquals(ref.frequencyHz.size, ref.referenceDb.size)
        }
        // More percussion: more sub and more treble
        assertTrue(at(refs[0], 50f) < at(refs[1], 50f) && at(refs[1], 50f) < at(refs[2], 50f))
        assertTrue(at(refs[0], 10_000f) < at(refs[1], 10_000f) && at(refs[1], 10_000f) < at(refs[2], 10_000f))
    }

    @Test
    fun oldSettingValuesMapToTheNewModes() {
        assertEquals(TonalityReferenceStore.MODE_IEC, TonalityReferenceStore.normalizeMode("music"))
        assertEquals(TonalityReferenceStore.MODE_CUSTOM, TonalityReferenceStore.normalizeMode("imported"))
        assertEquals(TonalityReferenceStore.MODE_PINK, TonalityReferenceStore.normalizeMode("pink"))
    }
}
