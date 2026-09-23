package me.timschneeberger.rootlessjamesdsp.latency

import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max

/**
 * Structured latency telemetry for the rootless capture loop: one JSON object per logcat line under
 * [TAG]. tools/latency/run_latency_test.py pairs it with the output of the latency probe app.
 *
 * All times are CLOCK_MONOTONIC nanoseconds (System.nanoTime() and both AudioTimestamp sources use
 * that clock), so they are comparable across processes.
 *
 * Frame bookkeeping: AudioRecord/AudioTrack timestamp positions count frames since the last
 * startRecording()/play() from a stopped state, so the loop calls [onRecorderStarted] and
 * [onTrackStarted] at exactly those points. Each block that is read is processed and written whole,
 * so an onset at offset k of the read block sits at offset k of the written block.
 *
 * Markers: the probe plays isolated rectangular pulses. A pre-DSP frame whose peak reaches
 * [MARKER_LEVEL] after at least [MIN_SILENCE_MS] of near-silence is a marker onset. The post-DSP
 * onset (first frame reaching half the window peak) gives the algorithmic delay of the DSP chain.
 *
 * Every call is a no-op when [enabled] is false.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class LatencyTracer(private val sampleRate: Int, val enabled: Boolean) {
    private class Marker(
        val seq: Int,
        val inFrame: Long,
        val outFrame: Long,
        val captureNs: Long,
        val readNs: Long,
        val writtenNs: Long,
    ) {
        var postSearchDone = false
        var dspDelayFrames = -1
        var outPeak = 0f
    }

    private val nsPerFrame = 1e9 / sampleRate
    private val minSilenceFrames = sampleRate.toLong() * MIN_SILENCE_MS / 1000

    // Frame counters relative to the last recorder/track start
    private var framesRead = 0L
    private var framesWritten = 0L
    private var silenceFrames = 0L

    private val recTs = AudioTimestamp()
    private val trackTs = AudioTimestamp()
    private var recTsValid = false
    private var trackTsValid = false

    // Onsets detected in the block currently between read and write
    private val blockOnsets = IntArray(MAX_ONSETS_PER_BLOCK)
    private val blockOnsetCaptureNs = LongArray(MAX_ONSETS_PER_BLOCK)
    private val blockOnsetInFrame = LongArray(MAX_ONSETS_PER_BLOCK)
    private var blockOnsetCount = 0

    private val markers = ArrayList<Marker>()
    private var markerSeq = 0

    // Post-DSP onset search, one marker at a time (markers are >= MIN_SILENCE_MS apart)
    private val postWindow = FloatArray(POST_WINDOW_FRAMES)
    private var postMarker: Marker? = null
    private var postFilled = 0

    // Per-block timing
    private var tBlockStart = 0L
    private var tRead = 0L
    private var tProcessed = 0L

    // Health window accumulators
    private var windowStart = 0L
    private var blocks = 0
    private var blockFramesSum = 0L
    private var readWaitSum = 0L
    private var readWaitMax = 0L
    private var dspSum = 0L
    private var dspMax = 0L
    private var writeWaitSum = 0L
    private var writeWaitMax = 0L
    private var lastUnderruns = -1
    private var prePeakMax = 0f
    private var onsets = 0
    private var armedPeaks = 0
    private var droppedFrames = 0L

    // Repeated periodically since logd may drop the first one at startup
    private var config: JSONObject? = null
    private var healthCount = 0

    fun logConfig(fields: JSONObject) {
        if (!enabled) return
        config = fields.put("src", "jdsp").put("ev", "config")
        emit(fields.put("t_ns", System.nanoTime()))
    }

    fun logLoopStop() {
        if (!enabled) return
        emit(JSONObject().put("src", "jdsp").put("ev", "loop_stop").put("t_ns", System.nanoTime()))
    }

    fun onRecorderStarted() {
        if (!enabled) return
        framesRead = 0
        recTsValid = false
        silenceFrames = 0
        blockOnsetCount = 0
    }

    /** A block that was read and processed but not written (latency controller drop). */
    fun onDropped(frames: Int) {
        if (!enabled) return
        blockOnsetCount = 0
        droppedFrames += frames
    }

    fun onTrackStarted() {
        if (!enabled) return
        // Pending markers refer to the previous track position domain; report and forget them
        for (m in markers)
            emitMarker(m, presentNs = -1, resolved = false, reason = "track_restarted")
        markers.clear()
        postMarker = null
        framesWritten = 0
        trackTsValid = false
        lastUnderruns = -1
    }

    fun beginBlock() {
        if (!enabled) return
        tBlockStart = System.nanoTime()
    }

    fun onRead(recorder: AudioRecord, buffer: FloatArray, samples: Int) {
        if (!enabled) return
        tRead = System.nanoTime()
        val frames = samples / 2
        blockOnsetCount = 0
        for (f in 0 until frames)
            scanFrame(max(abs(buffer[2 * f]), abs(buffer[2 * f + 1])), f)
        finishRead(recorder, frames)
    }

    fun onRead(recorder: AudioRecord, buffer: ShortArray, samples: Int) {
        if (!enabled) return
        tRead = System.nanoTime()
        val frames = samples / 2
        blockOnsetCount = 0
        for (f in 0 until frames)
            scanFrame(max(abs(buffer[2 * f].toInt()), abs(buffer[2 * f + 1].toInt())) / 32768f, f)
        finishRead(recorder, frames)
    }

    fun onProcessed() {
        if (!enabled) return
        tProcessed = System.nanoTime()
    }

    fun onWritten(track: AudioTrack, output: FloatArray, samplesWritten: Int) {
        if (!enabled) return
        val tWritten = System.nanoTime()
        val frames = max(samplesWritten, 0) / 2
        val blockStart = beginWritten(frames, tWritten)
        feedPost(blockStart, frames) { f -> max(abs(output[2 * f]), abs(output[2 * f + 1])) }
        endWritten(track, frames, tWritten)
    }

    fun onWritten(track: AudioTrack, output: ShortArray, samplesWritten: Int) {
        if (!enabled) return
        val tWritten = System.nanoTime()
        val frames = max(samplesWritten, 0) / 2
        val blockStart = beginWritten(frames, tWritten)
        feedPost(blockStart, frames) { f ->
            max(abs(output[2 * f].toInt()), abs(output[2 * f + 1].toInt())) / 32768f
        }
        endWritten(track, frames, tWritten)
    }

    private fun scanFrame(peak: Float, frameInBlock: Int) {
        if (peak > prePeakMax) prePeakMax = peak
        // Any onset after silence, even below MARKER_LEVEL
        if (peak >= SILENCE_LEVEL && silenceFrames >= minSilenceFrames) armedPeaks++
        if (peak >= MARKER_LEVEL && silenceFrames >= minSilenceFrames && blockOnsetCount < MAX_ONSETS_PER_BLOCK) {
            blockOnsets[blockOnsetCount++] = frameInBlock
            onsets++
        }
        silenceFrames = if (peak < SILENCE_LEVEL) silenceFrames + 1 else 0
    }

    private fun finishRead(recorder: AudioRecord, frames: Int) {
        val blockStart = framesRead
        framesRead += frames
        recTsValid = recorder.getTimestamp(recTs, AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS
        for (k in 0 until blockOnsetCount) {
            blockOnsetInFrame[k] = blockStart + blockOnsets[k]
            blockOnsetCaptureNs[k] = captureTimeOf(blockOnsetInFrame[k])
        }
    }

    private fun beginWritten(frames: Int, tWritten: Long): Long {
        val blockStart = framesWritten
        for (k in 0 until blockOnsetCount) {
            if (blockOnsets[k] < frames)
                markers += Marker(markerSeq++, blockOnsetInFrame[k], blockStart + blockOnsets[k],
                    blockOnsetCaptureNs[k], tRead, tWritten)
        }
        blockOnsetCount = 0
        return blockStart
    }

    private fun endWritten(track: AudioTrack, frames: Int, tWritten: Long) {
        framesWritten += frames
        trackTsValid = track.getTimestamp(trackTs)
        resolveMarkers(tWritten)

        blocks++
        blockFramesSum += frames
        val readWait = tRead - tBlockStart
        val dsp = tProcessed - tRead
        val writeWait = tWritten - tProcessed
        readWaitSum += readWait; readWaitMax = max(readWaitMax, readWait)
        dspSum += dsp; dspMax = max(dspMax, dsp)
        writeWaitSum += writeWait; writeWaitMax = max(writeWaitMax, writeWait)
        maybeEmitHealth(track, tWritten)
    }

    private inline fun feedPost(blockStart: Long, frames: Int, peakAt: (Int) -> Float) {
        val blockEnd = blockStart + frames
        while (true) {
            val m = postMarker
                ?: markers.firstOrNull { !it.postSearchDone }?.also { postMarker = it; postFilled = 0 }
                ?: return
            var g = max(m.outFrame + postFilled, blockStart)
            while (g < blockEnd && postFilled < POST_WINDOW_FRAMES) {
                postWindow[postFilled++] = peakAt((g - blockStart).toInt())
                g++
            }
            if (postFilled < POST_WINDOW_FRAMES)
                return

            var peak = 0f
            for (v in postWindow) if (v > peak) peak = v
            m.outPeak = peak
            m.dspDelayFrames = if (peak < MIN_OUTPUT_PEAK) -1 else postWindow.indexOfFirst { it >= 0.5f * peak }
            m.postSearchDone = true
            postMarker = null
        }
    }

    private fun resolveMarkers(now: Long) {
        val iterator = markers.iterator()
        while (iterator.hasNext()) {
            val m = iterator.next()
            val timedOut = now - m.writtenNs > MARKER_TIMEOUT_NS
            if (!m.postSearchDone && !timedOut)
                continue
            val presentFrame = m.outFrame + max(m.dspDelayFrames, 0)
            val presented = trackTsValid && trackTs.framePosition >= presentFrame
            if (!presented && !timedOut)
                continue
            emitMarker(m, presentNs = presentTimeOf(presentFrame), resolved = presented,
                reason = if (presented) null else "timeout")
            if (postMarker === m)
                postMarker = null
            iterator.remove()
        }
    }

    private fun maybeEmitHealth(track: AudioTrack, now: Long) {
        if (windowStart == 0L) {
            windowStart = now
            return
        }
        if (now - windowStart < HEALTH_INTERVAL_NS || blocks == 0)
            return

        val underruns = track.underrunCount
        val json = JSONObject()
            .put("src", "jdsp").put("ev", "health").put("t_ns", now)
            .put("blocks", blocks)
            .put("block_frames_avg", blockFramesSum / blocks)
            .put("read_wait_ms_avg", ms(readWaitSum / blocks)).put("read_wait_ms_max", ms(readWaitMax))
            .put("dsp_ms_avg", ms(dspSum / blocks)).put("dsp_ms_max", ms(dspMax))
            .put("write_wait_ms_avg", ms(writeWaitSum / blocks)).put("write_wait_ms_max", ms(writeWaitMax))
            .put("underruns", underruns)
            .put("underruns_delta", if (lastUnderruns < 0) 0 else underruns - lastUnderruns)
            .put("rec_ts_ok", recTsValid).put("trk_ts_ok", trackTsValid)
            .put("pre_peak_max", prePeakMax.toDouble()).put("onsets", onsets).put("armed", armedPeaks)
            .put("pending_markers", markers.size)
            .put("dropped_frames", droppedFrames)

        // The end of the newest block, located in each stream's own position domain
        if (recTsValid && trackTsValid) {
            json.put("internal_ms", ms(presentTimeOf(framesWritten) - captureTimeOf(framesRead)))
            val presentedNow = trackTs.framePosition + (now - trackTs.nanoTime) / nsPerFrame
            json.put("track_queue_ms", (framesWritten - presentedNow) * nsPerFrame / 1e6)
            val capturedNow = recTs.framePosition + (now - recTs.nanoTime) / nsPerFrame
            json.put("rec_backlog_ms", (capturedNow - framesRead) * nsPerFrame / 1e6)
        }
        emit(json)
        if (++healthCount % CONFIG_REPEAT_HEALTH == 0)
            config?.let { emit(it.put("t_ns", now)) }

        lastUnderruns = underruns
        windowStart = now
        blocks = 0
        blockFramesSum = 0
        readWaitSum = 0; readWaitMax = 0
        dspSum = 0; dspMax = 0
        writeWaitSum = 0; writeWaitMax = 0
        prePeakMax = 0f
        onsets = 0
        armedPeaks = 0
    }

    private fun emitMarker(m: Marker, presentNs: Long, resolved: Boolean, reason: String?) {
        val json = JSONObject()
            .put("src", "jdsp").put("ev", "marker").put("seq", m.seq)
            .put("in_frame", m.inFrame).put("out_frame", m.outFrame)
            .put("t_capture_ns", m.captureNs).put("t_read_ns", m.readNs)
            .put("t_written_ns", m.writtenNs).put("t_present_ns", presentNs)
            .put("dsp_delay_frames", m.dspDelayFrames).put("out_peak", m.outPeak.toDouble())
            .put("resolved", resolved)
        reason?.let { json.put("reason", it) }
        emit(json)
    }

    private fun captureTimeOf(frame: Long): Long =
        if (recTsValid) recTs.nanoTime + ((frame - recTs.framePosition) * nsPerFrame).toLong() else -1L

    private fun presentTimeOf(frame: Long): Long =
        if (trackTsValid) trackTs.nanoTime + ((frame - trackTs.framePosition) * nsPerFrame).toLong() else -1L

    private fun ms(ns: Long) = ns / 1e6

    private fun emit(json: JSONObject) {
        Log.i(TAG, json.toString())
    }

    companion object {
        const val TAG = "JdspLatency"

        private const val MARKER_LEVEL = 0.4f          // probe pulse amplitude is 0.8
        private const val SILENCE_LEVEL = 0.003f       // about -50 dBFS
        private const val MIN_SILENCE_MS = 150
        private const val MIN_OUTPUT_PEAK = 1e-4f
        private const val MAX_ONSETS_PER_BLOCK = 8
        private const val POST_WINDOW_FRAMES = 8192
        private const val MARKER_TIMEOUT_NS = 3_000_000_000L
        private const val HEALTH_INTERVAL_NS = 1_000_000_000L
        private const val CONFIG_REPEAT_HEALTH = 5
    }
}
