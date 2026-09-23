package me.timschneeberger.rootlessjamesdsp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import me.timschneeberger.rootlessjamesdsp.latency.LatencyTracer
import me.timschneeberger.rootlessjamesdsp.latency.LatencyTuning
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import org.json.JSONObject

/**
 * Debug builds only: lets tools/latency/run_latency_test.py change [LatencyTuning] over adb, then
 * hard-reboots the capture loop so the new values apply.
 *
 *   adb shell am broadcast -n <pkg>/me.timschneeberger.rootlessjamesdsp.receiver.LatencyTuningReceiver \
 *       --ez trace true --ei read_frames 480 --ei track_frames 960 --ez low_latency true
 *
 * Absent extras keep their stored value; `--ez reset true` restores the legacy defaults first.
 * Any tuning sent this way overrides the app's Low-latency mode setting, unless
 * `--ez app_settings true` is given: then the app setting decides and only `trace` is taken.
 */
class LatencyTuningReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val base = if(intent.getBooleanExtra(EXTRA_RESET, false)) LatencyTuning() else LatencyTuning.loadStored(context)
        val tuning = LatencyTuning(
            trace = intent.getBooleanExtra(LatencyTuning.KEY_TRACE, base.trace),
            readFrames = intent.getIntExtra(LatencyTuning.KEY_READ_FRAMES, base.readFrames).coerceAtLeast(0),
            trackBufferFrames = intent.getIntExtra(LatencyTuning.KEY_TRACK_BUFFER_FRAMES, base.trackBufferFrames).coerceAtLeast(0),
            lowLatencyTrack = intent.getBooleanExtra(LatencyTuning.KEY_LOW_LATENCY_TRACK, base.lowLatencyTrack),
            urgentPriority = intent.getBooleanExtra(LatencyTuning.KEY_URGENT_PRIORITY, base.urgentPriority),
            maxQueueFrames = intent.getIntExtra(LatencyTuning.KEY_MAX_QUEUE_FRAMES, base.maxQueueFrames).coerceAtLeast(0),
            override = !intent.getBooleanExtra(EXTRA_APP_SETTINGS, false),
        )
        tuning.save(context)

        Log.i(LatencyTracer.TAG, JSONObject()
            .put("src", "jdsp").put("ev", "tuning").put("t_ns", System.nanoTime())
            .put("tuning", tuning.toJson())
            .toString())

        // No-op if the service is not running; the stored values apply on its next start
        context.sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_HARD_REBOOT_CORE))
    }

    companion object {
        const val EXTRA_RESET = "reset"
        const val EXTRA_APP_SETTINGS = "app_settings"
    }
}
