package me.timschneeberger.rootlessjamesdsp.utils

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.MainActivity
import me.timschneeberger.rootlessjamesdsp.service.AudioProcessorService

object ServiceNotificationHelper {
    fun pushServiceNotification(context: Context, established: Boolean) {
        val notification = createServiceNotification(context, established)
        SystemServices.get(context, NotificationManager::class.java)
            .notify(Constants.NOTIFICATION_ID_SERVICE, notification)
    }

    fun createServiceNotification(context: Context, established: Boolean): Notification {
        val textRes: Int =
            if (established) {
                R.string.notification_processing
            }
            else
                R.string.notification_waiting

        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)

        val contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(context, Constants.CHANNEL_ID_SERVICE)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(textRes))
            .setSmallIcon(R.drawable.ic_tune_vertical_variant_24dp)
            .addAction(createStopAction(context))
            .setContentIntent(contentIntent)
            .build()
    }

    fun pushSessionLossNotification(context: Context, mediaProjectionStartIntent: Intent?) {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)

        val contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, Constants.CHANNEL_ID_SESSION_LOSS)
            .setContentTitle(context.getString(R.string.session_control_loss_notification_title))
            .setContentText(context.getString(R.string.session_control_loss_notification))
            .setSmallIcon(R.drawable.ic_baseline_warning_24dp)
            .addAction(createRetryAction(context, mediaProjectionStartIntent))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        SystemServices.get(context, NotificationManager::class.java)
            .notify(Constants.NOTIFICATION_ID_SESSION_LOSS, notification)
    }

    private fun createStopAction(context: Context): Notification.Action {
        val stopIntent = createStopIntent(context)
        val stopPendingIntent = PendingIntent.getService(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIcon = Icon.createWithResource(context, R.drawable.ic_close_24dp)
        val stopString = context.getString(R.string.action_stop)
        val actionBuilder = Notification.Action.Builder(stopIcon, stopString, stopPendingIntent)
        return actionBuilder.build()
    }

    private fun createRetryAction(context: Context, mediaProjectionStartIntent: Intent?): Notification.Action? {
        mediaProjectionStartIntent ?: return null
        val retryIntent = createStartIntent(context, mediaProjectionStartIntent)
        val retryPendingIntent = PendingIntent.getService(
            context,
            0,
            retryIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val retryIcon = Icon.createWithResource(context, R.drawable.ic_baseline_refresh_24dp)
        val retryString = context.getString(R.string.action_retry)
        val actionBuilder = Notification.Action.Builder(retryIcon, retryString, retryPendingIntent)
        return actionBuilder.build()
    }


    fun createStopIntent(ctx: Context): Intent {
        val intent = Intent(ctx, AudioProcessorService::class.java)
        intent.action = AudioProcessorService.ACTION_STOP
        return intent
    }

    fun createStartIntent(ctx: Context, mediaProjectionData: Intent?): Intent {
        val intent = Intent(ctx, AudioProcessorService::class.java)
        intent.action = AudioProcessorService.ACTION_START
        intent.putExtra(AudioProcessorService.EXTRA_MEDIA_PROJECTION_DATA, mediaProjectionData)
        return intent
    }
}