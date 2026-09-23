package me.timschneeberger.latencyprobe

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Process
import android.util.Log
import org.json.JSONObject
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max

data class ProbeConfig(
    val runId: String,
    val count: Int,
    val intervalMs: Int,
    val leadInMs: Int,
    val tailMs: Int,
    val pulseMs: Int,
    val amplitude: Float,
    val lowLatency: Boolean,
    val bufferMs: Int,
    val acoustic: Boolean,
    val finishWhenDone: Boolean,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("run_id", runId).put("count", count).put("interval_ms", intervalMs)
        .put("lead_in_ms", leadInMs).put("tail_ms", tailMs).put("pulse_ms", pulseMs)
        .put("amplitude", amplitude.toDouble()).put("low_latency", lowLatency)
        .put("buffer_ms", bufferMs).put("acoustic", acoustic)

    companion object {
        fun fromIntent(intent: Intent) = ProbeConfig(
            runId = intent.getStringExtra("run_id") ?: "manual",
            count = intent.getIntExtra("count", 30).coerceIn(1, 100_000),
            intervalMs = intent.getIntExtra("interval_ms", 1000).coerceIn(300, 10_000),
            // The engine needs time to see the new session, mute it and spin up its loop
            leadInMs = intent.getIntExtra("lead_in_ms", 3000).coerceAtLeast(0),
            tailMs = intent.getIntExtra("tail_ms", 2000).coerceAtLeast(0),
            pulseMs = intent.getIntExtra("pulse_ms", 2).coerceIn(1, 50),
            amplitude = intent.getFloatExtra("amplitude", 0.8f).coerceIn(0.05f, 1f),
            lowLatency = intent.getBooleanExtra("low_latency", false),
            bufferMs = intent.getIntExtra("buffer_ms", 0).coerceAtLeast(0),
            acoustic = intent.getBooleanExtra("acoustic", false),
            finishWhenDone = intent.getBooleanExtra("finish", true),
        )
    }
}

/**
 * Plays isolated rectangular pulses through an ordinary media AudioTrack and logs, for each pulse,
 * the presentation time AudioTrack.getTimestamp() reports. That is the clock a video player
 * (ExoPlayer etc.) syncs its frames to, so "rootless engine output time - this time" is exactly
 * the A/V offset a video app suffers.
 *
 * Optional acoustic mode also records the microphone and logs when each pulse is actually heard,
 * as a ground-truth cross-check of the timestamp method.
 *
 * Output: one JSON object per logcat line, tag [TAG]. Times are CLOCK_MONOTONIC nanoseconds.
 */
class ProbeRunner(
    private val context: Context,
    private val config: ProbeConfig,
    private val onDone: (String) -> Unit,
) {
    @Volatile private var stopped = false
    @Volatile private var playbackFinishedNs = 0L
    private var playThread: Thread? = null
    private var micThread: Thread? = null

    fun start() {
        val sampleRate = outputSampleRate()
        if(config.acoustic)
            micThread = thread(name = "probe-mic") { runMicrophone(sampleRate) }
        playThread = thread(name = "probe-play") { runPlayback(sampleRate) }
    }

    fun stop() {
        stopped = true
        playThread?.join(1000)
        micThread?.join(1000)
    }

    private fun runPlayback(sampleRate: Int) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val nsPerFrame = 1e9 / sampleRate
        val minBufferBytes = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)
        val bufferBytes = if(config.bufferMs > 0)
            max(minBufferBytes, config.bufferMs * sampleRate / 1000 * FRAME_BYTES)
        else
            minBufferBytes * 2

        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferBytes)
            .apply {
                if(config.lowLatency)
                    setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }
            .build()

        val chunkFrames = sampleRate / 100
        val chunk = FloatArray(chunkFrames * 2)
        val intervalFrames = config.intervalMs.toLong() * sampleRate / 1000
        val leadFrames = config.leadInMs.toLong() * sampleRate / 1000
        val pulseFrames = max(1L, config.pulseMs.toLong() * sampleRate / 1000)
        val pulseStarts = LongArray(config.count) { leadFrames + it * intervalFrames }
        val totalFrames = pulseStarts.last() + pulseFrames + config.tailMs.toLong() * sampleRate / 1000
        val resolveDeadlineFrames = totalFrames + 3L * sampleRate

        emit(config.toJson()
            .put("ev", "probe_config").put("t_ns", System.nanoTime())
            .put("sample_rate", sampleRate)
            .put("trk_buffer_frames", track.bufferSizeInFrames)
            .put("trk_perf_mode", track.performanceMode)
            .put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            .put("sdk", Build.VERSION.SDK_INT))

        val ts = AudioTimestamp()
        var written = 0L
        var resolved = 0
        var queueFramesSum = 0L
        var queueSamples = 0
        track.play()
        try {
            while(!stopped && written < resolveDeadlineFrames &&
                (written < totalFrames || resolved < config.count)) {
                for(i in 0 until chunkFrames) {
                    val frame = written + i
                    val value = if(frame >= leadFrames) {
                        val index = (frame - leadFrames) / intervalFrames
                        val offset = (frame - leadFrames) % intervalFrames
                        if(index < config.count && offset < pulseFrames) config.amplitude else 0f
                    } else 0f
                    chunk[2 * i] = value
                    chunk[2 * i + 1] = value
                }
                val result = track.write(chunk, 0, chunk.size, AudioTrack.WRITE_BLOCKING)
                if(result < 0) {
                    emit(JSONObject().put("ev", "probe_error").put("what", "write").put("code", result))
                    break
                }
                written += result / 2

                if(!track.getTimestamp(ts))
                    continue
                queueFramesSum += written - ts.framePosition
                queueSamples++
                // Interpolate backwards from a timestamp at or past the pulse onset
                while(resolved < config.count && ts.framePosition >= pulseStarts[resolved]) {
                    val presentNs = ts.nanoTime + ((pulseStarts[resolved] - ts.framePosition) * nsPerFrame).toLong()
                    emit(JSONObject()
                        .put("ev", "probe_marker").put("seq", resolved)
                        .put("frame", pulseStarts[resolved]).put("t_present_ns", presentNs))
                    resolved++
                }
            }
        }
        finally {
            track.stop()
            track.release()
        }

        playbackFinishedNs = System.nanoTime()
        val queueMs = if(queueSamples > 0) queueFramesSum.toDouble() / queueSamples * nsPerFrame / 1e6 else -1.0
        val summary = JSONObject()
            .put("ev", "probe_done").put("t_ns", playbackFinishedNs)
            .put("markers_resolved", resolved).put("markers_planned", config.count)
            .put("probe_queue_ms_avg", queueMs)
        micThread?.join(MIC_GRACE_MS + 1000)
        emit(summary)
        onDone(summary.toString(2))
    }

    private fun runMicrophone(sampleRate: Int) {
        if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            emit(JSONObject().put("ev", "probe_error").put("what", "RECORD_AUDIO not granted"))
            return
        }

        val audioManager = context.getSystemService(AudioManager::class.java)
        val unprocessed = audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val source = if(unprocessed) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
        val minBufferBytes = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)

        val recorder = try {
            AudioRecord.Builder()
                .setAudioSource(source)
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build())
                .setBufferSizeInBytes(max(minBufferBytes, sampleRate / 5 * Float.SIZE_BYTES))
                .build()
        }
        catch(ex: Exception) {
            emit(JSONObject().put("ev", "probe_error").put("what", "mic: ${ex.message}"))
            return
        }

        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val nsPerFrame = 1e9 / sampleRate
        val minSilenceFrames = sampleRate.toLong() * MIC_MIN_SILENCE_MS / 1000
        val buffer = FloatArray(sampleRate / 200)
        val ts = AudioTimestamp()
        var framesRead = 0L
        var silenceFrames = 0L
        var noiseFloor = 0.001f

        emit(JSONObject().put("ev", "probe_mic_config").put("source", if(unprocessed) "UNPROCESSED" else "VOICE_RECOGNITION"))
        recorder.startRecording()
        try {
            while(!stopped) {
                val finishedAt = playbackFinishedNs
                if(finishedAt != 0L && System.nanoTime() - finishedAt > MIC_GRACE_MS * 1_000_000L)
                    break

                val count = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if(count <= 0)
                    continue
                val tsValid = recorder.getTimestamp(ts, AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS

                for(i in 0 until count) {
                    val level = abs(buffer[i])
                    val threshold = max(MIC_MIN_LEVEL, noiseFloor * MIC_NOISE_RATIO)
                    if(level >= threshold && silenceFrames >= minSilenceFrames) {
                        val frame = framesRead + i
                        val captureNs = if(tsValid) ts.nanoTime + ((frame - ts.framePosition) * nsPerFrame).toLong() else -1L
                        emit(JSONObject()
                            .put("ev", "probe_mic").put("t_capture_ns", captureNs)
                            .put("level", level.toDouble()).put("noise_floor", noiseFloor.toDouble()))
                    }
                    if(level < threshold) {
                        silenceFrames++
                        noiseFloor = noiseFloor * 0.999f + level * 0.001f
                    }
                    else {
                        silenceFrames = 0
                    }
                }
                framesRead += count
            }
        }
        finally {
            recorder.stop()
            recorder.release()
        }
    }

    private fun outputSampleRate(): Int {
        val audioManager = context.getSystemService(AudioManager::class.java)
        return audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()?.takeIf { it > 0 } ?: 48000
    }

    private fun emit(json: JSONObject) {
        Log.i(TAG, json.put("src", "probe").put("run_id", config.runId).toString())
    }

    companion object {
        const val TAG = "JdspProbe"

        private const val FRAME_BYTES = 2 * Float.SIZE_BYTES
        private const val MIC_MIN_LEVEL = 0.02f
        private const val MIC_NOISE_RATIO = 10f
        private const val MIC_MIN_SILENCE_MS = 300
        private const val MIC_GRACE_MS = 1500L
    }
}
