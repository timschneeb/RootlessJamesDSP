package me.timschneeberger.rootlessjamesdsp.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.service.RootAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.Preferences
import me.timschneeberger.rootlessjamesdsp.utils.ServiceNotificationHelper
import me.timschneeberger.rootlessjamesdsp.utils.SystemServices
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class BootCompletedReceiver : BroadcastReceiver(), KoinComponent {
    private val preferences: Preferences.App by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED)
            return

        if(BuildConfig.ROOTLESS) {
            val notificationManager = SystemServices.get(context, NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    Constants.CHANNEL_ID_PERMISSION_PROMPT,
                    context.getString(R.string.notification_channel_permission_prompt),
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.enableVibration(false)
                channel.enableLights(false)
                notificationManager.createNotificationChannel(channel)
            }

            if (preferences.get<Boolean>(R.string.key_autostart_prompt_at_boot))
                ServiceNotificationHelper.pushPermissionPromptNotification(context)
        }
        else {
            // Root version: if enhanced processing mode is on, we need to start the service manually
            if(preferences.get<Boolean>(R.string.key_audioformat_enhanced_processing) &&
                !preferences.get<Boolean>(R.string.key_audioformat_processing)) {
                RootAudioProcessorService.startServiceEnhanced(context)
            }
        }
    }
}