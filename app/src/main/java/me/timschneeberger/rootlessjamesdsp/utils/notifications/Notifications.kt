package me.timschneeberger.rootlessjamesdsp.utils.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_NONE
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.extensions.buildNotificationChannel
import me.timschneeberger.rootlessjamesdsp.utils.extensions.buildNotificationChannelGroup
import me.timschneeberger.rootlessjamesdsp.utils.isRootless

/**
 * Class to manage the basic information of all the notifications used in the app.
 */
object Notifications {
    /**
     * Notification channel and ids used by the service.
     */
    private const val GROUP_SERVICE = "group_service"
    const val CHANNEL_SERVICE_STATUS = "service_status"
    const val ID_SERVICE_STATUS = 1
    const val CHANNEL_SERVICE_SESSION_LOSS = "service_session_loss"
    const val ID_SERVICE_SESSION_LOSS = 2
    const val CHANNEL_SERVICE_APP_COMPAT = "service_app_compat_alert"
    const val ID_SERVICE_APPCOMPAT = 3
    const val CHANNEL_SERVICE_STARTUP = "service_startup"
    const val ID_SERVICE_STARTUP = 4

    /**
     * Notification channel and ids used by the backup/restore system.
     */
    private const val GROUP_BACKUP_RESTORE = "group_backup_restore"
    const val CHANNEL_BACKUP_RESTORE_PROGRESS = "backup_restore_progress_channel"
    const val ID_BACKUP_PROGRESS = -501
    const val ID_RESTORE_PROGRESS = -503
    const val CHANNEL_BACKUP_RESTORE_COMPLETE = "backup_restore_complete_channel_v2"
    const val ID_BACKUP_COMPLETE = -502
    const val ID_RESTORE_COMPLETE = -504


    private val deprecatedChannels = listOf(
        "JamesDSP",
        "Session loss alert",
        "Permission prompt",
        "App incompatibility alert"
    )

    /**
     * Creates the notification channels introduced in Android Oreo.
     * This won't do anything on Android versions that don't support notification channels.
     *
     * @param context The application context.
     */
    fun createChannels(context: Context) {
        val notificationService = NotificationManagerCompat.from(context)

        // Delete old notification channels
        deprecatedChannels.forEach(notificationService::deleteNotificationChannel)

        notificationService.createNotificationChannelGroupsCompat(
            listOf(
                buildNotificationChannelGroup(GROUP_SERVICE) {
                    setName(context.getString(R.string.notification_group_service))
                },
                buildNotificationChannelGroup(GROUP_BACKUP_RESTORE) {
                    setName(context.getString(R.string.notification_group_backup))
                }
            ),
        )

        notificationService.createNotificationChannelsCompat(
            listOf(
                buildNotificationChannel(CHANNEL_SERVICE_STATUS, IMPORTANCE_NONE) {
                    setName(context.getString(R.string.notification_channel_service))
                    setGroup(GROUP_SERVICE)
                    setShowBadge(false)
                    setVibrationEnabled(false)
                    setSound(null, null)
                },
                buildNotificationChannel(CHANNEL_BACKUP_RESTORE_PROGRESS, IMPORTANCE_LOW) {
                    setName(context.getString(R.string.notification_channel_backup_progress))
                    setGroup(GROUP_BACKUP_RESTORE)
                    setShowBadge(false)
                },
                buildNotificationChannel(CHANNEL_BACKUP_RESTORE_COMPLETE, IMPORTANCE_HIGH) {
                    setName(context.getString(R.string.notification_channel_backup_complete))
                    setGroup(GROUP_BACKUP_RESTORE)
                    setShowBadge(false)
                    setSound(null, null)
                }
            )
        )

        if(isRootless()) {
            notificationService.createNotificationChannelsCompat(
                listOf(
                    buildNotificationChannel(CHANNEL_SERVICE_SESSION_LOSS, IMPORTANCE_HIGH) {
                        setName(context.getString(R.string.notification_channel_session_loss_alert))
                        setGroup(GROUP_SERVICE)
                    },
                    buildNotificationChannel(CHANNEL_SERVICE_APP_COMPAT, IMPORTANCE_HIGH) {
                        setName(context.getString(R.string.notification_channel_app_compat_alert))
                        setGroup(GROUP_SERVICE)
                    },
                    buildNotificationChannel(CHANNEL_SERVICE_STARTUP, IMPORTANCE_DEFAULT) {
                        setName(context.getString(R.string.notification_channel_permission_prompt))
                        setGroup(GROUP_SERVICE)
                        setVibrationEnabled(false)
                    },
                )
            )
        }
    }
}
