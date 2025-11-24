package me.timschneeberger.rootlessjamesdsp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.EngineLauncherActivity
import me.timschneeberger.rootlessjamesdsp.service.RootAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.extensions.PermissionExtensions.hasProjectMediaAppOp
import me.timschneeberger.rootlessjamesdsp.utils.isRoot
import me.timschneeberger.rootlessjamesdsp.utils.isRootless
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

        if(isRootless()) {
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
        else if(isRoot()) {
            // Root version: if enhanced processing mode is on, we need to start the service manually
            if(preferences.get<Boolean>(R.string.key_audioformat_enhanced_processing) &&
                !preferences.get<Boolean>(R.string.key_audioformat_processing)) {

                /*
                    FIXME: When targetting Android 15+, we are not allowed to start a
                           media_playback/media_projection foreground service from a BOOT_COMPLETED receiver.

                           Possible solutions:
                            - Also use EngineLauncherActivity for this.
                              Downside: requires SYSTEM_ALERT_WINDOW permission for the root build
                            - Better: Use the special use FGS type instead of media_playback for the root service.

                           Ref: https://developer.android.com/about/versions/15/behavior-changes-15#fgs-sysalert
                 */
                RootAudioProcessorService.startServiceEnhanced(context)
            }
            else if(preferences.get<Boolean>(R.string.key_audioformat_processing))
                RootAudioProcessorService.updateLegacyMode(context, true)
        }
    }
}