package me.timschneeberger.rootlessjamesdsp.analysis

import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Measures the long-term spectrum of captured playback (before JamesDSP processing) and compares
 * it with a reference curve.
 *
 * Audio arrives on the capture thread through a fixed pool of buffers and is analyzed on a worker
 * thread, which owns all analysis state. Everything is averaged in the power domain; decibels
 * are only taken for display. Resets are requested from any thread and applied by the worker.
 */
class PreDspTonalityAnalyzer(
    reference: TonalityReference,
    private val onFrame: (TonalityFrame?) -> Unit,
    private val displayRateHz: Int = 5,
    /** Receives one line of JSON per finished song. */
    private val onTrackSummary: (String) -> Unit = {},
    /** Receives each finished song of at least [LIBRARY_MIN_SONG_SECONDS]; see [FinishedSong]. */
    private val onSongFinished: (PreDspTonalityAnalyzer.FinishedSong) -> Unit = {}
) {
    /** A song's spectrum normalized to its own midrange, valid between [validFromHz] and [validToHz]. */
    class FinishedSong(val spectrumDb: FloatArray, val validFromHz: Float, val validToHz: Float)

    @Volatile
    private var reference = reference
    private val frequencies = reference.frequencyHz.copyOf()
    private val bands = frequencies.size
    private val config = TonalityAnalysisConfig(
        sampleRate = reference.sampleRate,
        fftSize = reference.fftSize,
        hopSize = reference.hopSize,
        bandsPerOctave = reference.bandsPerOctave,
        minHz = reference.minHz,
        maxHz = reference.maxHz
    )
    private val bandAnalyzer = StereoFftBandAnalyzer(config, TonalityBandLayout.create(config))
    private val hopSeconds = config.hopSize.toDouble() / config.sampleRate
    private val liveAlpha = 1.0 - exp(-hopSeconds / LIVE_WINDOW_SECONDS)

    private class Chunk(val data: FloatArray) {
        var length = 0
    }

    private val freeChunks = ArrayBlockingQueue<Chunk>(POOL_SIZE)
    private val filledChunks = ArrayBlockingQueue<Chunk>(POOL_SIZE)
    private val pendingReset = AtomicReference<TrackBoundaryEvent?>(null)
    private val droppedBuffers = AtomicInteger()

    /** Set while song changes are reported through media session metadata. */
    @Volatile
    var metadataTracking = false

    // Worker thread state
    private val livePower = DoubleArray(bands)
    private val trackPower = DoubleArray(bands)
    private var liveHops = 0
    private var trackHops = 0
    private var framesSinceReset = 0L
    private var silentFrames = 0L
    private var noveltyHops = 0
    private var currentTrack: TrackIdentity? = null
    private var lastResetReason: TrackBoundaryReason? = null
    private var verdict = VERDICT_WARMING_UP
    private var verdictCandidate = ""
    private var verdictCandidateCount = 0
    private var sampleRate = config.sampleRate

    private var workerThread: Thread? = null

    @Volatile
    private var running = false

    fun start(sampleRate: Int, maxBlockSamples: Int) {
        stop()
        this.sampleRate = sampleRate
        freeChunks.clear()
        filledChunks.clear()
        repeat(POOL_SIZE) { freeChunks.offer(Chunk(FloatArray(maxBlockSamples))) }
        droppedBuffers.set(0)
        pendingReset.set(null)
        clearTrack()
        livePower.fill(0.0)
        liveHops = 0
        currentTrack = null
        lastResetReason = null
        bandAnalyzer.reset()
        running = true
        onFrame(null)

        workerThread = Thread({ runWorker() }, "PreDspTonalityAnalyzer").also {
            it.priority = Thread.NORM_PRIORITY - 1
            it.start()
        }
    }

    fun stop() {
        running = false
        workerThread?.interrupt()
        try {
            workerThread?.join(STOP_JOIN_TIMEOUT_MS)
        }
        catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        workerThread = null
        filledChunks.clear()
        onFrame(null)
    }

    /** Starts a new song average; safe to call from any thread. */
    fun requestReset(event: TrackBoundaryEvent) {
        pendingReset.set(event)
    }

    /** Swaps the reference; the song average is kept, since it is stored independently of it. */
    fun setReference(newReference: TonalityReference) {
        if (newReference.frequencyHz.size == bands && newReference.sampleRate == reference.sampleRate)
            reference = newReference
    }

    fun offerPcm16(buffer: ShortArray, samplesRead: Int) {
        val chunk = takeChunk(samplesRead) ?: return
        for (i in 0 until chunk.length)
            chunk.data[i] = buffer[i] / PCM_16_SCALE
        filledChunks.offer(chunk)
    }

    fun offerFloat(buffer: FloatArray, samplesRead: Int) {
        val chunk = takeChunk(samplesRead) ?: return
        buffer.copyInto(chunk.data, endIndex = chunk.length)
        filledChunks.offer(chunk)
    }

    // Capture thread: never allocates or blocks; drops the block if the worker fell behind
    private fun takeChunk(samplesRead: Int): Chunk? {
        val usable = samplesRead - (samplesRead % CHANNELS)
        if (!running || usable <= 0) return null
        val chunk = freeChunks.poll()
        if (chunk == null || chunk.data.size < usable) {
            chunk?.let { freeChunks.offer(it) }
            droppedBuffers.incrementAndGet()
            return null
        }
        chunk.length = usable
        return chunk
    }

    private fun runWorker() {
        val minFrameIntervalNs = 1_000_000_000L / max(1, displayRateHz)
        var lastFrameNs = 0L
        // Set when analysis moved on since the last published frame
        var unpublished = false

        while (running) {
            val chunk = try {
                filledChunks.poll(100, TimeUnit.MILLISECONDS)
            }
            catch (_: InterruptedException) {
                break
            }

            val reset = pendingReset.getAndSet(null)
            if (reset != null) {
                applyReset(reset)
                lastFrameNs = 0L
            }
            if (chunk == null) {
                // Audio stopped or paused: show the final state
                if (reset != null || (unpublished && liveHops > 0)) {
                    publish()
                    unpublished = false
                }
                continue
            }
            unpublished = true

            try {
                process(chunk)
            }
            catch (ex: Exception) {
                // A bad block must not take the whole app down
                droppedBuffers.incrementAndGet()
            }
            finally {
                freeChunks.offer(chunk)
            }

            val now = System.nanoTime()
            if (now - lastFrameNs >= minFrameIntervalNs && liveHops > 0) {
                lastFrameNs = now
                publish()
                unpublished = false
            }
        }
    }

    private fun process(chunk: Chunk) {
        val frames = chunk.length / CHANNELS
        var sum = 0.0
        for (i in 0 until chunk.length) {
            val v = chunk.data[i]
            if (!v.isFinite()) return
            sum += v * v
        }
        val rmsDb = 10.0 * log10(max(sum / chunk.length, POWER_FLOOR))

        if (rmsDb < SILENCE_THRESHOLD_DB) {
            silentFrames += frames
            return
        }
        if (silentFrames > sampleRate * MIN_SILENCE_MS / 1000 &&
            trackHops * hopSeconds > MIN_SONG_SECONDS_FOR_SILENCE_RESET) {
            applyReset(TrackBoundaryEvent(System.nanoTime(), currentTrack, 0.55f, TrackBoundaryReason.SilenceThenResume))
        }
        silentFrames = 0
        framesSinceReset += frames

        bandAnalyzer.offerInterleavedStereo(chunk.data, chunk.length) { power -> accumulate(power) }
    }

    private fun accumulate(power: FloatArray) {
        for (p in power) if (!p.isFinite()) return

        for (i in 0 until bands) {
            livePower[i] = if (liveHops == 0) power[i].toDouble()
            else livePower[i] + liveAlpha * (power[i] - livePower[i])
        }
        ++liveHops

        // Skip the first seconds after a reset: crossfades and the previous song's tail
        if (framesSinceReset > sampleRate * TRACK_TRANSITION_MS / 1000) {
            for (i in 0 until bands) trackPower[i] += power[i]
            ++trackHops
        }

        if (!metadataTracking)
            checkNovelty()
    }

    // Without song metadata: a sustained, large change of the spectral shape starts a new song
    private fun checkNovelty() {
        if (trackHops * hopSeconds < NOVELTY_MIN_SONG_SECONDS) {
            noveltyHops = 0
            return
        }
        var count = 0
        var mean = 0.0
        val diff = DoubleArray(bands)
        for (i in 0 until bands) {
            if (frequencies[i] !in NOVELTY_MIN_HZ..NOVELTY_MAX_HZ) continue
            diff[i] = toDb(livePower[i]) - toDb(trackPower[i] / trackHops)
            mean += diff[i]
            ++count
        }
        if (count == 0) return
        mean /= count
        var sq = 0.0
        for (i in 0 until bands) {
            if (frequencies[i] !in NOVELTY_MIN_HZ..NOVELTY_MAX_HZ) continue
            sq += (diff[i] - mean) * (diff[i] - mean)
        }
        val distance = sqrt(sq / count)
        noveltyHops = if (distance > NOVELTY_DISTANCE_DB) noveltyHops + 1 else 0
        if (noveltyHops * hopSeconds > NOVELTY_HOLD_SECONDS)
            applyReset(TrackBoundaryEvent(System.nanoTime(), currentTrack, 0.4f, TrackBoundaryReason.SpectralNovelty))
    }

    private fun applyReset(event: TrackBoundaryEvent) {
        if (trackHops > 0)
            onTrackSummary(summaryJson(event.reason))
        if (trackHops * hopSeconds >= LIBRARY_MIN_SONG_SECONDS)
            finishedSong()?.let(onSongFinished)
        if (event.reason != TrackBoundaryReason.ManualReset || event.identity != null)
            currentTrack = event.identity ?: currentTrack
        lastResetReason = event.reason
        clearTrack()
    }

    private fun clearTrack() {
        trackPower.fill(0.0)
        trackHops = 0
        framesSinceReset = 0L
        silentFrames = 0L
        noveltyHops = 0
        verdict = VERDICT_WARMING_UP
        verdictCandidate = ""
        verdictCandidateCount = 0
    }

    private fun publish() {
        val ref = reference
        val live = if (liveHops > 0) DoubleArray(bands) { livePower[it] } else null
        val song = if (trackHops > 0) DoubleArray(bands) { trackPower[it] / trackHops } else null

        // The source's limits come from the song average once there is one; it is steadier
        val bandLimitHz = detectBandLimit(song ?: live, ref)
        val lowLimitHz = detectLowLimit(song ?: live, ref)
        val liveDeviation = deviation(live, ref, lowLimitHz, bandLimitHz)
        val songDeviation = song?.let { deviation(it, ref, lowLimitHz, bandLimitHz) }
        val songBands = songDeviation?.let { tonalBands(it, lowLimitHz, bandLimitHz) } ?: TonalBand.entries.associateWith { Float.NaN }
        val liveBands = tonalBands(liveDeviation, lowLimitHz, bandLimitHz)
        val analyzedSeconds = (trackHops * hopSeconds).toFloat()
        updateVerdict(songBands, analyzedSeconds)

        val refAt1k = ref.referenceDb[frequencies.indices.minBy { abs(frequencies[it] - 1_000f) }]
        onFrame(
            TonalityFrame(
                timestampNs = System.nanoTime(),
                track = currentTrack,
                validAudioMs = framesSinceReset * 1000 / max(1, sampleRate),
                analyzedSeconds = analyzedSeconds,
                referenceName = ref.referenceName,
                referenceKind = ref.referenceKind,
                referenceIsVerified = ref.isVerified,
                referenceLabel = ref.displayLabel,
                frequencyHz = frequencies.copyOf(),
                referenceDb = FloatArray(bands) { ref.referenceDb[it] - refAt1k },
                liveDeviationDb = liveDeviation,
                trackDeviationDb = songDeviation,
                tonalBandsDb = songBands,
                liveTonalBandsDb = liveBands,
                verdict = verdict,
                descriptor = if (songDeviation == null || verdict == VERDICT_WARMING_UP) "" else describe(songBands),
                deviationScoreDb = songDeviation?.let { score(it, lowLimitHz, bandLimitHz) } ?: 0f,
                confidence = (analyzedSeconds / FULL_CONFIDENCE_SECONDS).coerceIn(0.05f, 1f),
                isTransition = trackHops == 0,
                bandLimitHz = bandLimitHz,
                lowLimitHz = lowLimitHz,
                lastResetReason = lastResetReason,
                droppedBuffers = droppedBuffers.get()
            )
        )
    }

    /** dB relative to the reference, aligned so the median of 40 Hz-12 kHz (within the source's range) sits at 0 dB. */
    private fun deviation(power: DoubleArray?, ref: TonalityReference, lowLimitHz: Float?, bandLimitHz: Float?): FloatArray {
        if (power == null) return FloatArray(bands)
        val raw = FloatArray(bands) { i -> (toDb(power[i]) - ref.referenceDb[i]).toFloat() }
        val lower = maxOf(ANCHOR_MIN_HZ, lowLimitHz ?: 0f)
        val upper = minOf(ANCHOR_MAX_HZ, bandLimitHz ?: Float.MAX_VALUE)
        val anchor = median(raw.filterIndexed { i, v -> frequencies[i] in lower..upper && v.isFinite() })
        return FloatArray(bands) { i -> raw[i] - anchor }
    }

    /**
     * Many recordings have nothing in the deep bass (acoustic songs, old masters). Returns the
     * frequency the content starts at when every band below it is at least [LOW_DROP_DB] under the
     * midrange, relative to the reference, so a missing sub reads as "no content" and not as a
     * huge deficit.
     */
    private fun detectLowLimit(power: DoubleArray?, ref: TonalityReference): Float? {
        if (power == null) return null
        val relative = DoubleArray(bands) { i -> toDb(power[i]) - ref.referenceDb[i] }
        val midrange = median(relative.filterIndexed { i, _ -> frequencies[i] in LOW_LIMIT_MID_MIN_HZ..LOW_LIMIT_MID_MAX_HZ }
            .map { it.toFloat() })
        var limit: Float? = null
        for (k in 0 until bands) {
            if (frequencies[k] > LOW_LIMIT_SEARCH_MAX_HZ || relative[k] >= midrange - LOW_DROP_DB) break
            limit = frequencies[k] * 2.0.pow(0.5 / config.bandsPerOctave).toFloat()
        }
        return limit?.takeIf { it > LOW_LIMIT_MIN_REPORT_HZ }
    }

    // Spectrum normalized to the song's own midrange, for the library average
    private fun finishedSong(): FinishedSong? {
        val song = DoubleArray(bands) { trackPower[it] / trackHops }
        val ref = reference
        val db = FloatArray(bands) { toDb(song[it]).toFloat() }
        val mid = db.filterIndexed { i, _ -> frequencies[i] in LIBRARY_NORM_MIN_HZ..LIBRARY_NORM_MAX_HZ }.average().toFloat()
        if (!mid.isFinite()) return null
        return FinishedSong(
            FloatArray(bands) { db[it] - mid },
            detectLowLimit(song, ref) ?: 0f,
            detectBandLimit(song, ref) ?: Float.MAX_VALUE
        )
    }

    /**
     * Lossy codecs end the spectrum with a brick-wall low-pass; real recordings roll off gradually.
     * Returns the cut-off when a band above 10 kHz is followed by a drop of at least 15 dB within a
     * third of an octave that never recovers.
     */
    private fun detectBandLimit(power: DoubleArray?, ref: TonalityReference): Float? {
        if (power == null) return null
        val relative = DoubleArray(bands) { i -> toDb(power[i]) - ref.referenceDb[i] }
        for (k in 0 until bands - CUTOFF_SPAN_BANDS) {
            if (frequencies[k] < CUTOFF_SEARCH_MIN_HZ) continue
            val edge = relative[k]
            if (edge - relative[k + CUTOFF_SPAN_BANDS] < CUTOFF_DROP_DB) continue
            var staysLow = true
            for (j in k + CUTOFF_SPAN_BANDS until bands) {
                if (relative[j] > edge - CUTOFF_DROP_DB) {
                    staysLow = false
                    break
                }
            }
            if (staysLow) return frequencies[k] * 2.0.pow(0.5 / config.bandsPerOctave).toFloat()
        }
        return null
    }

    // Average of each tonal band in the power domain; NaN if the band lies outside the source's range
    private fun tonalBands(deviationDb: FloatArray, lowLimitHz: Float?, bandLimitHz: Float?): Map<TonalBand, Float> {
        return TonalBand.entries.associateWith { band ->
            val lower = maxOf(band.centerHz / SQRT2, lowLimitHz ?: 0f)
            val upper = minOf(band.centerHz * SQRT2, bandLimitHz ?: Float.MAX_VALUE)
            var sum = 0.0
            var count = 0
            for (i in frequencies.indices) {
                if (frequencies[i] in lower..upper) {
                    sum += 10.0.pow(deviationDb[i] / 10.0)
                    ++count
                }
            }
            if (count == 0 || upper < band.centerHz || lower > band.centerHz) Float.NaN else toDb(sum / count).toFloat()
        }
    }

    private fun score(deviationDb: FloatArray, lowLimitHz: Float?, bandLimitHz: Float?): Float {
        var sum = 0.0
        var count = 0
        for (i in deviationDb.indices) {
            if (bandLimitHz != null && frequencies[i] > bandLimitHz) continue
            if (lowLimitHz != null && frequencies[i] < lowLimitHz) continue
            sum += deviationDb[i] * deviationDb[i]
            ++count
        }
        return if (count == 0) 0f else sqrt(sum / count).toFloat()
    }

    // The verdict only changes after the same candidate came up for a while, so it doesn't flicker
    private fun updateVerdict(bandsDb: Map<TonalBand, Float>, analyzedSeconds: Float) {
        if (analyzedSeconds < VERDICT_MIN_SECONDS) {
            verdict = VERDICT_WARMING_UP
            return
        }
        val candidate = classify(bandsDb)
        if (candidate == verdictCandidate) ++verdictCandidateCount
        else {
            verdictCandidate = candidate
            verdictCandidateCount = 1
        }
        if (verdictCandidateCount >= VERDICT_HOLD_FRAMES || verdict == VERDICT_WARMING_UP)
            verdict = candidate
    }

    // Judged against each band's normal range: z > 1 means beyond it
    private fun classify(bandsDb: Map<TonalBand, Float>): String {
        val z = bandsDb.filterValues { it.isFinite() }.mapValues { (band, db) -> db / band.toleranceDb }
        if (z.isEmpty()) return VERDICT_WARMING_UP
        if (z.values.all { abs(it) <= 1f }) return "Balanced"
        val low = listOf(TonalBand.Sub, TonalBand.Bass)
        val high = listOf(TonalBand.Treble, TonalBand.Air)
        fun any(group: List<TonalBand>, test: (Float) -> Boolean) = group.any { band -> z[band]?.let(test) == true }
        val worst = z.maxBy { abs(it.value) }
        val boost = worst.value > 0
        return when {
            any(low) { it > 1f } && any(high) { it < -1f } -> "Warm"
            any(high) { it > 1f } && any(low) { it < -1f } -> "Bright"
            worst.key in low -> if (boost) "Bass-heavy" else "Thin"
            worst.key in high -> if (boost) "Bright" else "Dark"
            worst.key == TonalBand.LowMid -> if (boost) "Full" else "Lean"
            else -> if (boost) "Forward" else "Scooped"
        }
    }

    // Names the bands beyond their normal range, largest first
    private fun describe(bandsDb: Map<TonalBand, Float>): String {
        val outside = bandsDb.filter { (band, db) -> db.isFinite() && abs(db) > band.toleranceDb }
            .entries.sortedByDescending { (band, db) -> abs(db) / band.toleranceDb }
        if (outside.isEmpty())
            return "Every band is within its normal range."
        val parts = outside.take(2).map { (band, db) -> "%s %+.1f dB".format(Locale.ROOT, band.shortLabel, db) }
        return parts.joinToString(", ") + " beyond the normal range."
    }

    private fun summaryJson(reason: TrackBoundaryReason): String {
        val song = DoubleArray(bands) { trackPower[it] / trackHops }
        val ref = reference
        val limit = detectBandLimit(song, ref)
        val lowLimit = detectLowLimit(song, ref)
        val bandsDb = tonalBands(deviation(song, ref, lowLimit, limit), lowLimit, limit)
        fun esc(s: String?) = s?.replace("\\", "\\\\")?.replace("\"", "\\\"")
        val bandJson = bandsDb.entries.joinToString(",") { (band, db) ->
            "\"${band.label}\":" + if (db.isFinite()) "%.2f".format(Locale.ROOT, db) else "null"
        }
        // Absolute song spectrum at third-octave steps, so any reference can be applied later
        val thirdOctaves = (0 until bands step config.bandsPerOctave / 3).joinToString(",") { "%.1f".format(Locale.ROOT, toDb(song[it])) }
        return "{\"ev\":\"track_summary\",\"title\":\"${esc(currentTrack?.title) ?: ""}\"," +
            "\"artist\":\"${esc(currentTrack?.artist) ?: ""}\",\"ended_by\":\"$reason\"," +
            "\"seconds\":%.1f,\"reference\":\"${esc(ref.displayLabel)}\",".format(Locale.ROOT, trackHops * hopSeconds) +
            "\"band_limit_hz\":${limit?.let { "%.0f".format(Locale.ROOT, it) } ?: "null"}," +
            "\"low_limit_hz\":${lowLimit?.let { "%.0f".format(Locale.ROOT, it) } ?: "null"},\"bands\":{$bandJson}," +
            "\"third_octave_db_from_20hz\":[$thirdOctaves]}"
    }

    private fun toDb(power: Double) = 10.0 * log10(max(power, POWER_FLOOR))

    private fun median(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[middle - 1] + sorted[middle]) / 2f else sorted[middle]
    }

    companion object {
        private const val CHANNELS = 2
        private const val POOL_SIZE = 64
        private const val STOP_JOIN_TIMEOUT_MS = 500L
        private const val PCM_16_SCALE = 32768f
        private const val POWER_FLOOR = 1.0e-20
        private const val SQRT2 = 1.4142135f

        private const val LIVE_WINDOW_SECONDS = 3.0
        private const val TRACK_TRANSITION_MS = 3_000L
        private const val FULL_CONFIDENCE_SECONDS = 30f

        private const val SILENCE_THRESHOLD_DB = -50.0
        private const val MIN_SILENCE_MS = 400L
        private const val MIN_SONG_SECONDS_FOR_SILENCE_RESET = 5.0

        private const val NOVELTY_MIN_SONG_SECONDS = 30.0
        private const val NOVELTY_DISTANCE_DB = 5.0
        private const val NOVELTY_HOLD_SECONDS = 8.0
        private const val NOVELTY_MIN_HZ = 60f
        private const val NOVELTY_MAX_HZ = 10_000f

        private const val ANCHOR_MIN_HZ = 40f
        private const val ANCHOR_MAX_HZ = 12_000f
        private const val CUTOFF_SEARCH_MIN_HZ = 10_000f
        private const val CUTOFF_SPAN_BANDS = 4
        private const val CUTOFF_DROP_DB = 15.0
        private const val LOW_DROP_DB = 15.0
        private const val LOW_LIMIT_SEARCH_MAX_HZ = 150f
        private const val LOW_LIMIT_MIN_REPORT_HZ = 25f
        private const val LOW_LIMIT_MID_MIN_HZ = 200f
        private const val LOW_LIMIT_MID_MAX_HZ = 5_000f

        private const val LIBRARY_MIN_SONG_SECONDS = 30.0
        private const val LIBRARY_NORM_MIN_HZ = 250f
        private const val LIBRARY_NORM_MAX_HZ = 4_000f

        private const val VERDICT_MIN_SECONDS = 5f
        private const val VERDICT_HOLD_FRAMES = 5
        const val VERDICT_WARMING_UP = "Measuring"
    }
}
