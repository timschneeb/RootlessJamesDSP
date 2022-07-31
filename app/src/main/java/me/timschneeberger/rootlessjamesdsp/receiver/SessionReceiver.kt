package me.timschneeberger.rootlessjamesdsp.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import timber.log.Timber

class SessionReceiver : BroadcastReceiver() {

    @SuppressLint("BinaryOperationInTimber")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) {
            return
        }

        Timber.tag(TAG).i(
            "Action: ${intent.action}; " +
                    "session: ${intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)}; " +
                    "package ${intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)}")
        context.sendBroadcast(
            Intent(Constants.ACTION_SESSION_CHANGED)
                .apply {
                    putExtras(intent)
                }
        )
    }

    companion object {
        const val TAG = "SessionReceiver"
    }
}