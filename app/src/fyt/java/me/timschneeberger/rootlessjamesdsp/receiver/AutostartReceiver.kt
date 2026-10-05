package me.timschneeberger.rootlessjamesdsp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.timschneeberger.rootlessjamesdsp.utils.BootStarter
import me.timschneeberger.rootlessjamesdsp.utils.isRoot
import timber.log.Timber

/**
 * Starts the audio service without showing any UI when [ACTION_AUTOSTART] is received.
 * Meant for devices where BOOT_COMPLETED is not delivered (e.g. FYT head units).
 */
class AutostartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_AUTOSTART)
            return

        if (!isRoot()) {
            Timber.w("AutostartReceiver: ignored, only supported by the root build")
            return
        }

        Timber.i("AutostartReceiver: starting service")
        BootStarter.tryStartNow(context)
    }

    companion object {
        const val ACTION_AUTOSTART = "james.dsp.action.AUTOSTART"
    }
}
