package me.timschneeberger.rootlessjamesdsp.backup

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hippo.unifile.UniFile
import me.timschneeberger.rootlessjamesdsp.utils.notifications.Notifications
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class BackupCreatorJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val notificationManager = context.getSystemService<NotificationManager>()

    override suspend fun doWork(): Result {
        val notifier = BackupNotifier(context)
        val uri = inputData.getString(LOCATION_URI_KEY)?.toUri()
            ?: preferences.get<String>(R.string.key_backup_location).toUri()
        val isAutoBackup = inputData.getBoolean(IS_AUTO_BACKUP_KEY, true)

        notificationManager?.notify(Notifications.ID_BACKUP_PROGRESS, notifier.showBackupProgress().build())
        return try {
            val location = BackupManager(context).createBackup(uri, isAutoBackup)
            if (!isAutoBackup) notifier.showBackupComplete(UniFile.fromUri(context, location.toUri()))
            Result.success()
        } catch (e: Exception) {
            Timber.e(e)
            if (!isAutoBackup) notifier.showBackupError(e.message)
            Result.failure()
        } finally {
            notificationManager?.cancel(Notifications.ID_BACKUP_PROGRESS)
        }
    }

    companion object : KoinComponent {
        private const val TAG_AUTO = "BackupCreator"
        private const val TAG_MANUAL = "$TAG_AUTO:manual"

        private const val IS_AUTO_BACKUP_KEY = "is_auto_backup"
        private const val LOCATION_URI_KEY = "location_uri"

        private val preferences: Preferences.App by inject()

        fun isManualJobRunning(context: Context): Boolean {
            val list = WorkManager.getInstance(context).getWorkInfosByTag(TAG_MANUAL).get()
            return list.find { it.state == WorkInfo.State.RUNNING } != null
        }

        fun setupTask(context: Context, prefInterval: Int? = null) {
            val interval = prefInterval ?: preferences.get<String>(R.string.key_backup_frequency).toInt()
            val workManager = WorkManager.getInstance(context)
            if (interval > 0) {

                val request = PeriodicWorkRequestBuilder<BackupCreatorJob>(
                    interval.toLong(),
                    TimeUnit.HOURS,
                    10,
                    TimeUnit.MINUTES,
                )
                    .addTag(TAG_AUTO)
                    .setInputData(workDataOf(IS_AUTO_BACKUP_KEY to true))
                    .build()

                workManager.enqueueUniquePeriodicWork(TAG_AUTO, ExistingPeriodicWorkPolicy.REPLACE, request)
            } else {
                workManager.cancelUniqueWork(TAG_AUTO)
            }
        }

        fun startNow(context: Context, uri: Uri) {
            val inputData = workDataOf(
                IS_AUTO_BACKUP_KEY to false,
                LOCATION_URI_KEY to uri.toString(),
            )
            val request = OneTimeWorkRequestBuilder<BackupCreatorJob>()
                .addTag(TAG_MANUAL)
                .setInputData(inputData)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(TAG_MANUAL, ExistingWorkPolicy.KEEP, request)
        }
    }
}