package me.timschneeberger.rootlessjamesdsp.utils

import android.content.Context
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.service.RootAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import timber.log.Timber

object BootStarter {
    /** Starts the root audio service the same way BootCompletedReceiver does, without any UI. */
    fun tryStartNow(ctx: Context): Boolean {
        return try {
            val prefs = Preferences(ctx).App()
            if (prefs.get<Boolean>(R.string.key_audioformat_enhanced_processing) &&
                !prefs.get<Boolean>(R.string.key_audioformat_processing)
            ) {
                RootAudioProcessorService.startServiceEnhanced(ctx)
            } else if (prefs.get<Boolean>(R.string.key_audioformat_processing)) {
                RootAudioProcessorService.updateLegacyMode(ctx, true)
            }
            true
        } catch (t: Throwable) {
            Timber.e(t, "BootStarter.tryStartNow failed")
            false
        }
    }
}
