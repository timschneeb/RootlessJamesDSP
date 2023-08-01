package me.timschneeberger.rootlessjamesdsp.utils.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.IconCompat
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.AppCompatibilityActivity
import me.timschneeberger.rootlessjamesdsp.activity.EngineLauncherActivity
import me.timschneeberger.rootlessjamesdsp.activity.MainActivity
import me.timschneeberger.rootlessjamesdsp.model.IEffectSession
import me.timschneeberger.rootlessjamesdsp.service.RootlessAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.SdkCheck
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.getAppName
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.getAppNameFromUid
import me.timschneeberger.rootlessjamesdsp.utils.isRootless
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


object ServiceNotificationHelper: KoinComponent {
    private val preferences: Preferences.App by inject()

    fun pushPermissionPromptNotification(context: Context) {
        NotificationCompat.Builder(context, Notifications.CHANNEL_SERVICE_STARTUP)
            .setContentTitle(context.getString(R.string.notification_request_permission_title))
            .setContentText(context.getString(R.string.notification_request_permission))
            .setSmallIcon(R.drawable.ic_tune_vertical_variant_24dp)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, EngineLauncherActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
            .let {
                context.getSystemService<NotificationManager>()
                    ?.notify(Notifications.ID_SERVICE_STARTUP, it)
            }
    }

    fun pushServiceNotification(context: Context, sessions: Array<IEffectSession>) {
        context.getSystemService<NotificationManager>()
            ?.notify(Notifications.ID_SERVICE_STATUS, createServiceNotification(context, sessions))
    }

    fun pushServiceNotificationLegacy(context: Context) {
        context.getSystemService<NotificationManager>()
            ?.notify(Notifications.ID_SERVICE_STATUS, createServiceNotificationLegacy(context))
    }

    fun createServiceNotificationLegacy(context: Context): Notification {
        return createServiceNotification(
            context,
            context.getString(
                if(preferences.get<Boolean>(R.string.key_powered_on)) R.string.notification_processing_title
                else R.string.notification_processing_disabled_title
            ),
            context.getString(R.string.notification_processing_legacy)
        )
    }

    fun createServiceNotification(context: Context, sessions: Array<IEffectSession>): Notification {
        val apps = sessions.distinct().joinToString(", ") {
            // Rootless uses UIDs primarily internally, while Root has a guaranteed package name
            if(isRootless()) {
                context.getAppNameFromUid(it.uid)
                    ?: it.packageName
            }
            else {
                context.getAppName(it.packageName)
                    ?: context.getAppNameFromUid(it.uid)
                    ?: it.packageName as CharSequence
            }
        }

        // Rootless: if service active it is always processing audio
        val enabled = isRootless() || preferences.get<Boolean>(R.string.key_powered_on)

        return createServiceNotification(context,
            context.getString(
                if(enabled) R.string.notification_processing_title
                else R.string.notification_processing_disabled_title
            ), when {
                sessions.isNotEmpty() -> context.getString(R.string.notification_processing, apps)
                else -> context.getString(R.string.notification_idle)
            })
    }

    private fun createServiceNotification(
        context: Context,
        title: String,
        message: String
    ) = NotificationCompat.Builder(context, Notifications.CHANNEL_SERVICE_STATUS)
        .setShowWhen(false)
        .setOnlyAlertOnce(true)
        .setCategory(Notification.CATEGORY_SERVICE)
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_tune_vertical_variant_24dp)
        .setContentIntent(
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                                or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                or Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setOngoing(true)
        .apply {
            if(isRootless())
                addAction(createStopAction(context))
        }
        .build()

    fun pushSessionLossNotification(context: Context, mediaProjectionStartIntent: Intent?) =
        NotificationCompat.Builder(context, Notifications.CHANNEL_SERVICE_SESSION_LOSS)
            .setContentTitle(context.getString(R.string.session_control_loss_notification_title))
            .setContentText(context.getString(R.string.session_control_loss_notification))
            .setSmallIcon(R.drawable.ic_baseline_warning_24dp)
            .addAction(createRetryAction(context, mediaProjectionStartIntent))
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                                or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                or Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
            .let {
                context.getSystemService<NotificationManager>()
                    ?.notify(Notifications.ID_SERVICE_SESSION_LOSS, it)
            }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun pushAppIssueNotification(context: Context, projectionIntent: Intent?, appUid: Int) {
        NotificationCompat.Builder(context, Notifications.CHANNEL_SERVICE_APP_COMPAT)
            .setContentTitle(context.getString(R.string.session_app_compat_notification_title))
            .setContentText(context.getString(R.string.session_app_compat_notification))
            .setSmallIcon(R.drawable.ic_baseline_warning_24dp)
            .addAction(createAppTroubleshootAction(context, projectionIntent, appUid))
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    createAppTroubleshootIntent(context, projectionIntent, appUid, false),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
            .let {
                context.getSystemService<NotificationManager>()
                    ?.notify(Notifications.ID_SERVICE_APPCOMPAT, it)
            }
    }

    private fun createAction(
        context: Context,
        @StringRes textRes: Int,
        @DrawableRes iconRes: Int,
        intent: Intent
    ): NotificationCompat.Action {
        return NotificationCompat.Action(
            IconCompat.createWithResource(context, iconRes),
            context.getString(textRes),
            PendingIntent.getService(
                context,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    private fun createStopAction(context: Context) =
        createAction(
            context,
            R.string.action_stop,
            R.drawable.ic_close_24dp,
            if (SdkCheck.isQ && isRootless()) {
                with(Intent(context, RootlessAudioProcessorService::class.java)) {
                    action = RootlessAudioProcessorService.ACTION_STOP
                    this
                }
            }
            else throw IllegalStateException()
        )

    private fun createRetryAction(context: Context, mediaProjectionStartIntent: Intent?) =
        mediaProjectionStartIntent?.let {
            createAction(
                context,
                R.string.action_retry,
                R.drawable.ic_baseline_refresh_24dp,
                it
            )
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createAppTroubleshootAction(context: Context, projectionIntent: Intent?, uid: Int) =
        projectionIntent?.let {
            createAction(
                context,
                R.string.action_fix,
                R.drawable.ic_twotone_chevron_right_24dp,
                createAppTroubleshootIntent(context, it, uid, directLaunch = false)
            )
        }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun createAppTroubleshootIntent(
        ctx: Context,
        mediaProjectionData: Intent?,
        appUid: Int,
        directLaunch: Boolean
    ): Intent {
        return Intent(ctx, AppCompatibilityActivity::class.java).apply {
            action = RootlessAudioProcessorService.ACTION_START
            putExtra(RootlessAudioProcessorService.EXTRA_MEDIA_PROJECTION_DATA, mediaProjectionData)
            putExtra(RootlessAudioProcessorService.EXTRA_APP_UID, appUid)
            putExtra(RootlessAudioProcessorService.EXTRA_APP_COMPAT_INTERNAL_CALL, directLaunch)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }


    fun createStopIntent(ctx: Context) =
        if (SdkCheck.isQ && isRootless()) {
            with(Intent(ctx, RootlessAudioProcessorService::class.java)) {
                action = RootlessAudioProcessorService.ACTION_STOP
                this
            }
        }
        else throw IllegalStateException()

    fun createStartIntent(ctx: Context, mediaProjectionData: Intent? = null) =
        if (SdkCheck.isQ && isRootless()) {
            with(Intent(ctx, RootlessAudioProcessorService::class.java)) {
                action = RootlessAudioProcessorService.ACTION_START
                putExtra(RootlessAudioProcessorService.EXTRA_MEDIA_PROJECTION_DATA, mediaProjectionData)
                this
            }
        }
        else throw IllegalStateException()
}