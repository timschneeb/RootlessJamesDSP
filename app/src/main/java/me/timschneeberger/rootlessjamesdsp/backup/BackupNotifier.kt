package me.timschneeberger.rootlessjamesdsp.backup

import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.hippo.unifile.UniFile
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.notifications.Notifications


class BackupNotifier(private val context: Context) {

    private val notificationManager = context.getSystemService<NotificationManager>()

    private val progressNotificationBuilder = NotificationCompat.Builder(context, Notifications.CHANNEL_BACKUP_RESTORE_PROGRESS).apply {
        setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_dsp_launcher))
        setSmallIcon(R.drawable.ic_tune_vertical_variant_24dp)
        setAutoCancel(false)
        setOngoing(true)
        setOnlyAlertOnce(true)
    }

    private val completeNotificationBuilder = NotificationCompat.Builder(context, Notifications.CHANNEL_BACKUP_RESTORE_COMPLETE).apply {
        setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_dsp_launcher))
        setSmallIcon(R.drawable.ic_tune_vertical_variant_24dp)
        setAutoCancel(false)
    }

    private fun NotificationCompat.Builder.show(id: Int) {
        notificationManager?.notify(id, build())
    }

    fun showBackupProgress(): NotificationCompat.Builder {
        val builder = with(progressNotificationBuilder) {
            setContentTitle(context.getString(R.string.backup_create_progress))

            setProgress(0, 0, true)
        }

        builder.show(Notifications.ID_BACKUP_PROGRESS)

        return builder
    }

    fun showBackupError(error: String?) {
        notificationManager?.cancel(Notifications.ID_BACKUP_PROGRESS)

        with(completeNotificationBuilder) {
            setContentTitle(context.getString(R.string.backup_create_error))
            setContentText(error)

            show(Notifications.ID_BACKUP_COMPLETE)
        }
    }

    fun showBackupComplete(unifile: UniFile) {
        notificationManager?.cancel(Notifications.ID_BACKUP_PROGRESS)

        with(completeNotificationBuilder) {
            setContentTitle(context.getString(R.string.backup_create_completed))
            setContentText(unifile.filePath ?: unifile.name)

            // Clear old actions if they exist
            clearActions()

            show(Notifications.ID_BACKUP_COMPLETE)
        }
    }

    fun showRestoreProgress(): NotificationCompat.Builder {
        val builder = with(progressNotificationBuilder) {
            setContentTitle(context.getString(R.string.backup_restore_progress))
            setProgress(0, 0, true)
            setOnlyAlertOnce(true)
            clearActions()
        }

        builder.show(Notifications.ID_RESTORE_PROGRESS)

        return builder
    }

    fun showRestoreError(error: String?) {
        notificationManager?.cancel(Notifications.ID_RESTORE_PROGRESS)

        with(completeNotificationBuilder) {
            setContentTitle(context.getString(R.string.backup_restore_error))
            setContentText(error)

            show(Notifications.ID_RESTORE_COMPLETE)
        }
    }

    fun showRestoreComplete() {
        notificationManager?.cancel(Notifications.ID_RESTORE_PROGRESS)

        with(completeNotificationBuilder) {
            setContentTitle(context.getString(R.string.backup_restore_completed))

            // Clear old actions if they exist
            clearActions()

            show(Notifications.ID_RESTORE_COMPLETE)
        }
    }
}
