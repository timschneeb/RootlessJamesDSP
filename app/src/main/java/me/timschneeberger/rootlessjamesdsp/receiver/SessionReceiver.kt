package me.timschneeberger.rootlessjamesdsp.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.service.RootAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.session.rootless.MutedSessionManager
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.sendLocalBroadcast
import timber.log.Timber

class SessionReceiver : BroadcastReceiver() {

    @SuppressLint("BinaryOperationInTimber")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) {
            return
        }

        if(intent.getIntExtra(MutedSessionManager.EXTRA_IGNORE, 0) == 1) {
            Timber.d("Control close intent ignored")
            return
        }

        Timber.i(
            "Action: ${intent.action}; " +
                    "session: ${intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)}; " +
                    "package ${intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)}")

        context.sendLocalBroadcast(
            Intent(Constants.ACTION_SESSION_CHANGED)
                .apply {
                    putExtras(intent)
                }
        )

        if(!BuildConfig.ROOTLESS) {
            Intent(context, RootAudioProcessorService::class.java)
                .apply { this.action = intent.action }
                .apply { putExtras(intent) }
                .run { RootAudioProcessorService.startService(context, this) }
        }
    }
}