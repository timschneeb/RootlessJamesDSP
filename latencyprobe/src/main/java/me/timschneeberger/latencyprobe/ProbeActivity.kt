package me.timschneeberger.latencyprobe

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView

/**
 * Launched by tools/latency/run_latency_test.py:
 *
 *   adb shell am start -n me.timschneeberger.latencyprobe/.ProbeActivity \
 *       --ei count 30 --ei interval_ms 1000 --ez acoustic false --es run_id <id>
 *
 * See [ProbeConfig] for all extras.
 */
class ProbeActivity : Activity() {
    private var runner: ProbeRunner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val status = TextView(this).apply {
            setPadding(48, 48, 48, 48)
            textSize = 16f
        }
        setContentView(status)

        val config = ProbeConfig.fromIntent(intent)
        status.text = "Latency probe running\n\n$config"

        runner = ProbeRunner(this, config) { summary ->
            runOnUiThread {
                status.text = "Latency probe done\n\n$summary"
                if(config.finishWhenDone)
                    finish()
            }
        }.also { it.start() }
    }

    override fun onDestroy() {
        runner?.stop()
        runner = null
        super.onDestroy()
    }
}
