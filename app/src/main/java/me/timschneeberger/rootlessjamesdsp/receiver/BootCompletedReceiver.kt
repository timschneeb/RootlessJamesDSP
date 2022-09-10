package me.timschneeberger.rootlessjamesdsp.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.MainActivity
import me.timschneeberger.rootlessjamesdsp.utils.Constants
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

            val notify = context.getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)
                .getBoolean(context.getString(R.string.key_autostart_prompt_at_boot), true)
            if(notify) {
                notificationManager.notify(Constants.NOTIFICATION_ID_PERMISSION_PROMPT,
                    createPermissionRequestNotification(context))
            }
        }
    }

    private fun createPermissionRequestNotification(context: Context): Notification {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_FORCE_SHOW_CAPTURE_PROMPT, true)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val contentIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = Notification.Builder(context, Constants.CHANNEL_ID_PERMISSION_PROMPT)
        notificationBuilder.setContentTitle(context.getString(R.string.notification_request_permission_title))
        notificationBuilder.setContentText(context.getString(R.string.notification_request_permission))
        notificationBuilder.setSmallIcon(R.drawable.ic_tune_vertical_variant_24dp)
        notificationBuilder.setContentIntent(contentIntent)
        return notificationBuilder.build()
    }
}