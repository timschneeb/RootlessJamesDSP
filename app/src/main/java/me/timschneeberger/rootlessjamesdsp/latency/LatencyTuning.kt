package me.timschneeberger.rootlessjamesdsp.latency

import android.content.Context
import org.json.JSONObject

/**
 * Buffer settings for the rootless capture loop.
 *
 * The constructor defaults match the legacy loop, [LOW_LATENCY] is used by the "Low-latency mode"
 * setting. Debug builds can override the values over adb (see tools/latency/README.md).
 * Values are read in startRecording(), so a hard core reboot applies them.
 */
data class LatencyTuning(
    /** Emit JdspLatency telemetry (config/health/marker lines) to logcat. */
    val trace: Boolean = false,
    /** Frames per AudioRecord.read(); 0 = legacy (whole buffer, bufferSize / 2 frames). */
    val readFrames: Int = 0,
    /** AudioTrack.setBufferSizeInFrames() target; 0 = legacy (full track capacity). */
    val trackBufferFrames: Int = 0,
    /** Request AudioTrack.PERFORMANCE_MODE_LOW_LATENCY for the output track. */
    val lowLatencyTrack: Boolean = false,
    /** Run the recorder thread at THREAD_PRIORITY_URGENT_AUDIO. */
    val urgentPriority: Boolean = false,
    /** Drop a block when the output track holds more than this many frames; 0 = off. */
    val maxQueueFrames: Int = 0,
    /** Use the stored values instead of the app setting (debug builds only). */
    val override: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject()
        .put(KEY_TRACE, trace)
        .put(KEY_READ_FRAMES, readFrames)
        .put(KEY_TRACK_BUFFER_FRAMES, trackBufferFrames)
        .put(KEY_LOW_LATENCY_TRACK, lowLatencyTrack)
        .put(KEY_URGENT_PRIORITY, urgentPriority)
        .put(KEY_MAX_QUEUE_FRAMES, maxQueueFrames)
        .put(KEY_OVERRIDE, override)

    fun save(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_TRACE, trace)
            .putInt(KEY_READ_FRAMES, readFrames)
            .putInt(KEY_TRACK_BUFFER_FRAMES, trackBufferFrames)
            .putBoolean(KEY_LOW_LATENCY_TRACK, lowLatencyTrack)
            .putBoolean(KEY_URGENT_PRIORITY, urgentPriority)
            .putInt(KEY_MAX_QUEUE_FRAMES, maxQueueFrames)
            .putBoolean(KEY_OVERRIDE, override)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "latency_tuning"

        const val KEY_TRACE = "trace"
        const val KEY_READ_FRAMES = "read_frames"
        const val KEY_TRACK_BUFFER_FRAMES = "track_frames"
        const val KEY_LOW_LATENCY_TRACK = "low_latency"
        const val KEY_URGENT_PRIORITY = "urgent_priority"
        const val KEY_MAX_QUEUE_FRAMES = "max_queue_frames"
        const val KEY_OVERRIDE = "override"

        val LOW_LATENCY = LatencyTuning(
            readFrames = 960,
            trackBufferFrames = 3840,
            lowLatencyTrack = true,
            maxQueueFrames = 2880,
        )

        fun load(context: Context, lowLatencyMode: Boolean): LatencyTuning {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_OVERRIDE, false)) {
                val preset = if (lowLatencyMode) LOW_LATENCY else LatencyTuning()
                return preset.copy(trace = prefs.getBoolean(KEY_TRACE, false))
            }
            return loadStored(context)
        }

        fun loadStored(context: Context): LatencyTuning {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val defaults = LatencyTuning()
            return LatencyTuning(
                trace = prefs.getBoolean(KEY_TRACE, defaults.trace),
                readFrames = prefs.getInt(KEY_READ_FRAMES, defaults.readFrames).coerceAtLeast(0),
                trackBufferFrames = prefs.getInt(KEY_TRACK_BUFFER_FRAMES, defaults.trackBufferFrames).coerceAtLeast(0),
                lowLatencyTrack = prefs.getBoolean(KEY_LOW_LATENCY_TRACK, defaults.lowLatencyTrack),
                urgentPriority = prefs.getBoolean(KEY_URGENT_PRIORITY, defaults.urgentPriority),
                maxQueueFrames = prefs.getInt(KEY_MAX_QUEUE_FRAMES, defaults.maxQueueFrames).coerceAtLeast(0),
                override = prefs.getBoolean(KEY_OVERRIDE, false),
            )
        }
    }
}
