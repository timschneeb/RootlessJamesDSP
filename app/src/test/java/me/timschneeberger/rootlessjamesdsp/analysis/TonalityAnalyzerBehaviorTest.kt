package me.timschneeberger.rootlessjamesdsp.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class TonalityAnalyzerBehaviorTest {
    private val sampleRate = 48_000
    private val block = 1920   // samples per offered buffer (960 stereo frames)

    // Paul Kellet's pink noise filter, accurate to about 0.05 dB above 10 Hz
    private fun pinkNoise(seconds: Int, seed: Long = 1): FloatArray {
        val rng = Random(seed)
        val out = FloatArray(seconds * sampleRate)
        var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var b3 = 0.0; var b4 = 0.0; var b5 = 0.0; var b6 = 0.0
        for (i in out.indices) {
            val white = rng.nextGaussian() * 0.05
            b0 = 0.99886 * b0 + white * 0.0555179; b1 = 0.99332 * b1 + white * 0.0750759
            b2 = 0.96900 * b2 + white * 0.1538520; b3 = 0.86650 * b3 + white * 0.3104856
            b4 = 0.55000 * b4 + white * 0.5329522; b5 = -0.7616 * b5 - white * 0.0168980
            out[i] = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362).toFloat()
            b6 = white * 0.115926
        }
        return out
    }

    // Steep windowed-sinc low-pass, like a lossy codec's band limit
    private fun lowPass(x: FloatArray, cutoffHz: Double): FloatArray {
        val taps = 401
        val fc = cutoffHz / sampleRate
        val h = DoubleArray(taps) { n ->
            val m = n - taps / 2
            val sinc = if (m == 0) 2 * fc else sin(2 * PI * fc * m) / (PI * m)
            sinc * (0.42 - 0.5 * kotlin.math.cos(2 * PI * n / (taps - 1)) + 0.08 * kotlin.math.cos(4 * PI * n / (taps - 1)))
        }
        return FloatArray(x.size) { i ->
            var acc = 0.0
            for (k in 0 until taps) {
                val j = i - k
                if (j >= 0) acc += h[k] * x[j]
            }
            acc.toFloat()
        }
    }

    private fun stereo(mono: FloatArray) = FloatArray(mono.size * 2) { mono[it / 2] }

    private class Run(val frame: AtomicReference<TonalityFrame?>, val summaries: MutableList<String>, val analyzer: PreDspTonalityAnalyzer)

    private fun startAnalyzer(reference: TonalityReference): Run {
        val frame = AtomicReference<TonalityFrame?>(null)
        val summaries = java.util.Collections.synchronizedList(mutableListOf<String>())
        val analyzer = PreDspTonalityAnalyzer(reference, onFrame = { if (it != null) frame.set(it) },
            onTrackSummary = { summaries += it })
        analyzer.start(sampleRate, block)
        return Run(frame, summaries, analyzer)
    }

    private fun feed(run: Run, samples: FloatArray) {
        var i = 0
        while (i + block <= samples.size) {
            run.analyzer.offerFloat(samples.copyOfRange(i, i + block), block)
            i += block
            Thread.sleep(1)
        }
        // Feeding runs faster than real time; wait until the worker has caught up
        var last = -1L
        repeat(50) {
            Thread.sleep(300)
            val stamp = run.frame.get()?.timestampNs ?: 0L
            if (stamp == last) return
            last = stamp
        }
    }

    @Test
    fun referenceFedBackReadsFlat() {
        val run = startAnalyzer(TonalityReferenceFactory.pinkNoise(sampleRate))
        feed(run, stereo(pinkNoise(14)))
        val frame = run.frame.get()!!
        run.analyzer.stop()
        for ((band, db) in frame.tonalBandsDb)
            assertEquals("band $band", 0f, db, 1.0f)
        assertEquals("Balanced", frame.verdict)
        assertNull(frame.bandLimitHz)
        assertEquals(0, frame.droppedBuffers)
    }

    @Test
    fun codecLowPassIsReportedAsBandLimit() {
        val run = startAnalyzer(TonalityReferenceFactory.pinkNoise(sampleRate))
        feed(run, stereo(lowPass(pinkNoise(10, seed = 2), 16_000.0)))
        val frame = run.frame.get()!!
        run.analyzer.stop()
        val limit = frame.bandLimitHz
        assertNotNull(limit)
        assertTrue("limit $limit", limit!! in 14_000f..17_500f)
        // The air band lies mostly above the limit and must not be read as a deficit
        val air = frame.tonalBandsDb[TonalBand.Air]!!
        assertTrue("air $air", !air.isFinite() || abs(air) < 2f)
    }

    // Cascaded second-order Butterworth high-pass sections
    private fun highPass(x: FloatArray, cutoffHz: Double, sections: Int): FloatArray {
        val w0 = 2 * PI * cutoffHz / sampleRate
        val alpha = sin(w0) / (2 * sqrt(0.5))
        val cos = kotlin.math.cos(w0)
        val a0 = 1 + alpha
        val b0 = (1 + cos) / 2 / a0; val b1 = -(1 + cos) / a0; val b2 = b0
        val a1 = -2 * cos / a0; val a2 = (1 - alpha) / a0
        var y = x
        repeat(sections) {
            val input = y
            var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0
            y = FloatArray(input.size) { i ->
                val out = b0 * input[i] + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
                x2 = x1; x1 = input[i].toDouble(); y2 = y1; y1 = out
                out.toFloat()
            }
        }
        return y
    }

    @Test
    fun missingDeepBassIsReportedAsContentLimitNotAsDeficit() {
        val run = startAnalyzer(TonalityReferenceFactory.pinkNoise(sampleRate))
        feed(run, stereo(highPass(pinkNoise(10, seed = 6), 50.0, 6)))
        val frame = run.frame.get()!!
        run.analyzer.stop()
        val limit = frame.lowLimitHz
        assertNotNull(limit)
        assertTrue("limit $limit", limit!! in 45f..85f)
        assertTrue("sub ${frame.tonalBandsDb[TonalBand.Sub]}", frame.tonalBandsDb[TonalBand.Sub]!!.isNaN())
        assertEquals("Balanced", frame.verdict)
    }

    @Test
    fun manualResetStartsANewSongAndLogsTheOldOne() {
        val run = startAnalyzer(TonalityReferenceFactory.pinkNoise(sampleRate))
        feed(run, stereo(pinkNoise(10, seed = 3)))
        run.frame.get()!!.let { assertTrue("analyzed ${it.analyzedSeconds} s, reset ${it.lastResetReason}, dropped ${it.droppedBuffers}", it.analyzedSeconds > 5f) }

        run.analyzer.requestReset(TrackBoundaryEvent(System.nanoTime(), null, 1f, TrackBoundaryReason.ManualReset))
        feed(run, stereo(pinkNoise(1, seed = 4)))
        val frame = run.frame.get()!!
        run.analyzer.stop()
        assertEquals(0f, frame.analyzedSeconds, 0.01f)
        // No song numbers until the new song has audio past its first seconds
        assertNull(frame.trackDeviationDb)
        assertTrue(frame.tonalBandsDb.values.all { it.isNaN() })
        assertEquals(TrackBoundaryReason.ManualReset, frame.lastResetReason)
        assertEquals(1, run.summaries.size)
        assertTrue(run.summaries[0].contains("\"ev\":\"track_summary\""))
    }

    @Test
    fun nanBufferDoesNotStopAnalysis() {
        val run = startAnalyzer(TonalityReferenceFactory.pinkNoise(sampleRate))
        run.analyzer.offerFloat(FloatArray(block) { Float.NaN }, block)
        feed(run, stereo(pinkNoise(8, seed = 5)))
        val frame = run.frame.get()!!
        run.analyzer.stop()
        assertTrue(frame.analyzedSeconds > 3f)
        assertTrue(frame.trackDeviationDb!!.all { it.isFinite() })
    }

    @Test
    fun resamplerKeepsLevelAndRemovesAliases() {
        fun tone(hz: Double) = FloatArray(96_000 * 2) { i -> sin(2 * PI * hz * (i / 2) / 96_000).toFloat() }
        fun rmsDb(x: FloatArray): Double {
            val body = x.copyOfRange(x.size / 4, x.size * 3 / 4)
            return 20 * log10(sqrt(body.sumOf { (it * it).toDouble() } / body.size))
        }
        val inBand = PcmResampler.resampleInterleavedStereo(tone(1_000.0), 96_000, 48_000)
        val alias = PcmResampler.resampleInterleavedStereo(tone(30_000.0), 96_000, 48_000)
        assertEquals(rmsDb(tone(1_000.0)), rmsDb(inBand), 0.1)
        assertTrue("alias ${rmsDb(alias)} dB", rmsDb(alias) < rmsDb(inBand) - 60)
    }

    @Test
    fun decodesExtensible24BitWav() {
        val frames = 4_800
        val data = ByteBuffer.allocate(frames * 6).order(ByteOrder.LITTLE_ENDIAN)
        repeat(frames) {
            for (value in intArrayOf(4_194_304, -4_194_304)) {  // +0.5 left, -0.5 right
                data.put((value and 0xff).toByte()); data.put((value shr 8 and 0xff).toByte()); data.put((value shr 16 and 0xff).toByte())
            }
        }
        val fmt = ByteBuffer.allocate(40).order(ByteOrder.LITTLE_ENDIAN)
            .putShort(0xFFFE.toShort()).putShort(2).putInt(48_000).putInt(48_000 * 6).putShort(6).putShort(24)
            .putShort(22).putShort(24).putInt(3).putShort(1).putShort(0)   // cbSize, valid bits, mask, PCM sub-format
            .put(ByteArray(12))
        val wav = ByteBuffer.allocate(12 + 8 + 40 + 8 + data.capacity()).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(4 + 8 + 40 + 8 + data.capacity()).put("WAVE".toByteArray())
            .put("fmt ".toByteArray()).putInt(40).put(fmt.array())
            .put("data".toByteArray()).putInt(data.capacity()).put(data.array())
        val decoded = WavPcmDecoder.decode(ByteArrayInputStream(wav.array()), "test")
        assertEquals(48_000, decoded.sampleRate)
        assertEquals(frames * 2, decoded.interleavedStereo.size)
        assertEquals(0.5f, decoded.interleavedStereo[0], 1e-4f)
        assertEquals(-0.5f, decoded.interleavedStereo[1], 1e-4f)
    }
}
