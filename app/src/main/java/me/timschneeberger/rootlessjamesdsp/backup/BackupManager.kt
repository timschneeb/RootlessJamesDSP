package me.timschneeberger.rootlessjamesdsp.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hippo.unifile.UniFile
import kotlinx.coroutines.Job
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.preference.FileLibraryPreference
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.broadcastPresetLoadEvent
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import me.timschneeberger.rootlessjamesdsp.utils.storage.Tar
import okio.buffer
import okio.gzip
import okio.sink
import okio.source
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(private val context: Context): KoinComponent {
    private val preferences: Preferences.App by inject()
    var job: Job? = null

    /**
     * Create backup file from database
     *
     * @param uri path of Uri
     * @param isAutoBackup backup called from scheduled backup job
     */
    fun createBackup(uri: Uri, isAutoBackup: Boolean): String {
        Timber.d("Creating backup (auto=$isAutoBackup) to $uri")

        var file: UniFile? = null
        try {
            file = (if (isAutoBackup) {
                        // Get dir of file and create
                        var dir = UniFile.fromUri(context, uri)
                        dir = dir.createDirectory("automatic")

                        // Delete older backups
                        val numberOfBackups = preferences.get<String>(R.string.key_backup_maximum).toIntOrNull() ?: 2
                        val backupRegex = Regex("""jamesdsp_backup_\d+-\d+-\d+_\d+-\d+.tar.gz""")
                        dir.listFiles { _, filename -> backupRegex.matches(filename) }
                            .orEmpty()
                            .sortedByDescending { it.name }
                            .drop(numberOfBackups - 1)
                            .forEach { it.delete() }

                        // Create new file to place backup
                        dir.createFile(getBackupFilename())
                    } else {
                        UniFile.fromUri(context, uri)
                    })
                ?: throw Exception("Couldn't create backup file")

            if (!file.isFile) {
                throw IllegalStateException("Failed to get handle on file")
            }

            Tar.Composer(file.openOutputStream().sink().gzip().buffer().outputStream()).use { c ->
                c.metadata = mutableMapOf(
                    META_MIN_VERSION_CODE to BuildConfig.VERSION_CODE.toString(),
                    META_FLAVOR to BuildConfig.FLAVOR,
                    META_IS_BACKUP to true.toString(),
                    META_HAS_DEVICE_PROFILES to false.toString()
                )

                File(context.applicationInfo.dataDir + "/shared_prefs")
                    .listFiles()
                    ?.filter { it.name.startsWith("dsp_") }
                    ?.filter { it.extension == "xml" }
                    ?.forEach { c.add(it, "shared_prefs/${it.name}") }

                if(preferences.get<Boolean>(R.string.key_device_profiles_enable)) {
                    c.metadata[META_HAS_DEVICE_PROFILES] = true.toString()
                    File(context.applicationInfo.dataDir + "/files/profiles").let { root ->
                        root
                            .walkTopDown()
                            .forEach { c.add(it, "profiles/${it.toRelativeString(root)}") }
                    }
                }

                FileLibraryPreference.types.entries.forEach { entry ->
                    File(context.getExternalFilesDir(null), "/${entry.key}")
                        .listFiles()
                        ?.filter { entry.value.any { ext -> it.absolutePath.endsWith(ext) } }
                        ?.forEach { c.add(it, "${entry.key}/${it.name}") }
                }
            }


            return file.uri.toString()
        } catch (e: Exception) {
            Timber.e(e)
            file?.delete()
            throw e
        }
    }

    fun restoreBackup(uri: Uri, dirty: Boolean) {
        Timber.d("Restoring backup from $uri")

        context.contentResolver.openInputStream(uri)!!.source().gzip().buffer().inputStream().use { stream ->
            val targetFolder = File(context.cacheDir, "restore")
            val metadata = Tar.Reader(stream, ::isKnownFile).extract(targetFolder)
            if(metadata == null) {
                targetFolder.deleteRecursively()
                throw UnsupportedOperationException(context.getString(R.string.backup_restore_error_format))
            }

            if(BuildConfig.VERSION_CODE < (metadata[META_MIN_VERSION_CODE]?.toIntOrNull() ?: 0)) {
                targetFolder.deleteRecursively()
                throw UnsupportedOperationException(context.getString(R.string.backup_restore_error_version_too_new))
            }

            // Clean restore
            if(!dirty) {
                // Remove profiles
                File(context.applicationInfo.dataDir + "/files/profiles").deleteRecursively()

                // Remove shared dsp prefs
                File(context.applicationInfo.dataDir + "/shared_prefs").listFiles { file: File ->
                    file.name.startsWith("dsp_") && file.extension == "xml"
                }?.forEach { it.delete() }
                // Remove external files
                context.getExternalFilesDir(null)
                    ?.absoluteFile
                    ?.listFiles()
                    ?.forEach { it.deleteRecursively() }
            }

            var enableDeviceProfiles = false
            targetFolder.listFiles()?.forEach { file ->
                if(file.isDirectory && file.name == "shared_prefs")
                    file.copyRecursively(File(context.applicationInfo.dataDir + "/shared_prefs"), true)
                else if(file.isDirectory && file.name == "profiles") {
                    enableDeviceProfiles = true
                    file.copyRecursively(
                        File(context.applicationInfo.dataDir + "/files/profiles"),
                        true
                    )
                }
                else if(file.isDirectory && FileLibraryPreference.types.any { file.name.startsWith(it.key) }) {
                    file.copyRecursively(File(context.getExternalFilesDir(null)!!.absolutePath + "/" + file.name), true)
                }
            }

            targetFolder.deleteRecursively()

            context.broadcastPresetLoadEvent()
            context.sendLocalBroadcast(Intent(Constants.ACTION_BACKUP_RESTORED))

            if(enableDeviceProfiles)
                preferences.set(R.string.key_device_profiles_enable, true)
        }
    }

    companion object {
        private const val META_MIN_VERSION_CODE = "min_version_code"
        private const val META_FLAVOR = "flavor"
        private const val META_HAS_DEVICE_PROFILES = "has_device_profiles"
        const val META_IS_BACKUP = "is_backup"

        private fun isKnownFile(name: String): Boolean {
            return (name.contains("dsp_") && name.endsWith(".xml")) ||
                    name.startsWith("profiles/") ||
                    FileLibraryPreference.types.any { name.contains(it.key) && it.value.any { ext -> name.endsWith(ext) } }
        }

        fun getBackupFilename(): String {
            val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
            return "jamesdsp_backup_$date.tar.gz"
        }
    }
}
