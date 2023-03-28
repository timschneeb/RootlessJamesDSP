package me.timschneeberger.rootlessjamesdsp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.EngineLauncherActivity
import me.timschneeberger.rootlessjamesdsp.service.RootAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.extensions.PermissionExtensions.hasProjectMediaAppOp
import me.timschneeberger.rootlessjamesdsp.utils.notifications.ServiceNotificationHelper
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber


class BootCompletedReceiver : BroadcastReceiver(), KoinComponent {
    private val preferences: Preferences.App by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED)
            return

        if(BuildConfig.ROOTLESS) {
            if (!preferences.get<Boolean>(R.string.key_autostart_prompt_at_boot))
                return

            if(Settings.canDrawOverlays(context) && context.hasProjectMediaAppOp()) {
                Timber.i("Preconditions for a silent auto-start met")
                context.startActivity(Intent(context, EngineLauncherActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                            Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                })
                return
            }

            ServiceNotificationHelper.pushPermissionPromptNotification(context)
        }
        else {
            // Root version: if enhanced processing mode is on, we need to start the service manually
            if(preferences.get<Boolean>(R.string.key_audioformat_enhanced_processing) &&
                !preferences.get<Boolean>(R.string.key_audioformat_processing)) {
                RootAudioProcessorService.startServiceEnhanced(context)
            }
            else if(preferences.get<Boolean>(R.string.key_audioformat_processing))
                RootAudioProcessorService.updateLegacyMode(context, true)
        }
    }
}