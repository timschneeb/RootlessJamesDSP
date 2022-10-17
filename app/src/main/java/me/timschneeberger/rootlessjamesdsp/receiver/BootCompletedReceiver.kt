package me.timschneeberger.rootlessjamesdsp.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ServiceNotificationHelper
import me.timschneeberger.rootlessjamesdsp.utils.SystemServices


class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
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

            if(context.getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)
                    .getBoolean(context.getString(R.string.key_autostart_prompt_at_boot), true))
                ServiceNotificationHelper.pushPermissionPromptNotification(context)
        }
    }
}