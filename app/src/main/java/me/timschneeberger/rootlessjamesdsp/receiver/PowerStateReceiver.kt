package me.timschneeberger.rootlessjamesdsp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.timschneeberger.rootlessjamesdsp.utils.EngineUtils.toggleEnginePower
import timber.log.Timber

class PowerStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return
        context ?: return

        Timber.d("Received power state broadcast")

        context.toggleEnginePower(intent.getBooleanExtra(EXTRA_ENABLED, false))
    }

    companion object {
        const val EXTRA_ENABLED = "rootlessjamesdsp.enabled"
    }
}