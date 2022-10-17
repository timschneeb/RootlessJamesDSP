package me.timschneeberger.rootlessjamesdsp.utils

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.AppCompatibilityActivity
import me.timschneeberger.rootlessjamesdsp.activity.MainActivity
import me.timschneeberger.rootlessjamesdsp.model.AudioSessionEntry
import me.timschneeberger.rootlessjamesdsp.service.AudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.getAppName
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.getAppNameFromUid


object ServiceNotificationHelper {
    private fun createNotificationBuilder(context: Context, channel: String): Notification.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(context, channel)
        else
            Notification.Builder(context)
    }

    fun pushPermissionPromptNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_FORCE_SHOW_CAPTURE_PROMPT, true)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val contentIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = createNotificationBuilder(context, Constants.CHANNEL_ID_PERMISSION_PROMPT)
            .setContentTitle(context.getString(R.string.notification_request_permission_title))
            .setContentText(context.getString(R.string.notification_request_permission))
            .setSmallIcon(R.drawable.ic_tune_vertical_variant_24dp)
            .setContentIntent(contentIntent)
            .build()

        SystemServices.get(context, NotificationManager::class.java)
            .notify(Constants.NOTIFICATION_ID_PERMISSION_PROMPT, notification)
    }

    fun pushServiceNotification(context: Context, sessions: Array<AudioSessionEntry>?) {
        val notification = createServiceNotification(context, sessions)
        SystemServices.get(context, NotificationManager::class.java)
            .notify(Constants.NOTIFICATION_ID_SERVICE, notification)
    }

    fun createServiceNotification(context: Context, sessions: Array<AudioSessionEntry>?): Notification {
        val apps = sessions?.distinct()?.joinToString(", ") {
            context.getAppNameFromUid(it.uid) ?: it.packageName
        }

        val text: String =
            when {
                sessions?.isNotEmpty() == true -> context.getString(R.string.notification_processing, apps)
                sessions?.isEmpty() == true -> context.getString(R.string.notification_idle)
                else -> context.getString(R.string.notification_waiting)
            }

        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)

        val contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return createNotificationBuilder(context, Constants.CHANNEL_ID_SERVICE)
            .setContentTitle(context.getAppName())
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_tune_vertical_variant_24dp)
            .addAction(createStopAction(context))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    fun pushSessionLossNotification(context: Context, mediaProjectionStartIntent: Intent?) {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)

        val contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = createNotificationBuilder(context, Constants.CHANNEL_ID_SESSION_LOSS)
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

    @RequiresApi(Build.VERSION_CODES.Q)
    fun pushAppIssueNotification(context: Context, mediaProjectionStartIntent: Intent?, data: AudioSessionEntry) {
        val intent = createAppTroubleshootIntent(context, mediaProjectionStartIntent, data, directLaunch = false)
        val contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = createNotificationBuilder(context, Constants.CHANNEL_ID_APP_INCOMPATIBILITY)
            .setContentTitle(context.getString(R.string.session_app_compat_notification_title))
            .setContentText(context.getString(R.string.session_app_compat_notification))
            .setSmallIcon(R.drawable.ic_baseline_warning_24dp)
            .addAction(createAppTroubleshootAction(context, mediaProjectionStartIntent, data))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        SystemServices.get(context, NotificationManager::class.java)
            .notify(Constants.NOTIFICATION_ID_APP_INCOMPATIBILITY, notification)
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createAppTroubleshootAction(context: Context, mediaProjectionStartIntent: Intent?, data: AudioSessionEntry): Notification.Action? {
        mediaProjectionStartIntent ?: return null
        val fixIntent = PendingIntent.getService(
            context,
            0,
            createAppTroubleshootIntent(context, mediaProjectionStartIntent, data, directLaunch = false),
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val fixIcon = Icon.createWithResource(context, R.drawable.ic_twotone_chevron_right_24dp)
        val fixString = context.getString(R.string.action_fix)
        val actionBuilder = Notification.Action.Builder(fixIcon, fixString, fixIntent)
        return actionBuilder.build()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun createAppTroubleshootIntent(ctx: Context, mediaProjectionData: Intent?, data: AudioSessionEntry, directLaunch: Boolean): Intent {
        val intent = Intent(ctx, AppCompatibilityActivity::class.java)
        intent.action = AudioProcessorService.ACTION_START
        intent.putExtra(AudioProcessorService.EXTRA_MEDIA_PROJECTION_DATA, mediaProjectionData)
        intent.putExtra(AudioProcessorService.EXTRA_APP_UID, data.uid)
        intent.putExtra(AudioProcessorService.EXTRA_APP_COMPAT_INTERNAL_CALL, directLaunch)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    fun createStopIntent(ctx: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && BuildConfig.ROOTLESS) {
            with(Intent(ctx, AudioProcessorService::class.java)) {
                action = AudioProcessorService.ACTION_STOP
                this
            }
        }
        else {
            throw NotImplementedError("TODO") // TODO implement root service
        }
    }

    fun createStartIntent(ctx: Context, mediaProjectionData: Intent?): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && BuildConfig.ROOTLESS) {
            with(Intent(ctx, AudioProcessorService::class.java)) {
                action = AudioProcessorService.ACTION_START
                putExtra(AudioProcessorService.EXTRA_MEDIA_PROJECTION_DATA, mediaProjectionData)
                this
            }
        }
        else {
            throw NotImplementedError("TODO") // TODO implement root service
        }
    }
}