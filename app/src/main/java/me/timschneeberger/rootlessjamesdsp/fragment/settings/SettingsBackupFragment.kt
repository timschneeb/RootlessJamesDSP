package me.timschneeberger.rootlessjamesdsp.fragment.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hippo.unifile.UniFile
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.SettingsActivity
import me.timschneeberger.rootlessjamesdsp.backup.BackupCreatorJob
import me.timschneeberger.rootlessjamesdsp.backup.BackupManager
import me.timschneeberger.rootlessjamesdsp.backup.BackupRestoreService
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import org.koin.android.ext.android.inject

class SettingsBackupFragment : SettingsBaseFragment() {

    private val create by lazy { findPreference<Preference>(getString(R.string.key_backup_create)) }
    private val restore by lazy { findPreference<Preference>(getString(R.string.key_backup_restore)) }
    private val frequency by lazy { findPreference<ListPreference>(getString(R.string.key_backup_frequency)) }
    private val location by lazy { findPreference<Preference>(getString(R.string.key_backup_location)) }

    private val preferences: Preferences.App by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = Constants.PREF_APP
        setPreferencesFromResource(R.xml.app_backup_preferences, rootKey)

        if(!hasStoragePermissions()) {
            preferences.reset<String>(R.string.key_backup_location, false)
            resetFrequencyIfLocationUnset()
        }

        updateSummaries()

        create?.setOnPreferenceClickListener {
            if (!BackupCreatorJob.isManualJobRunning(requireContext())) {
                openSaveFileSelection()
            } else {
                requireContext().toast(R.string.backup_in_progress)
            }
            true
        }

        restore?.setOnPreferenceClickListener {
            openLoadFileSelection()
            true
        }

        frequency?.setOnPreferenceChangeListener { _, newValue ->
            val interval = (newValue as String).toInt()
            BackupCreatorJob.setupTask(requireContext(), interval)

            if(preferences.get<String>(R.string.key_backup_location).isBlank()
                && (newValue.toString().toIntOrNull() ?: 0) > 0) {
                openLocationSelection()
            }
            true
        }

        location?.setOnPreferenceClickListener {
            openLocationSelection()
            true
        }
    }

    private fun openLocationSelection() {
        try {
            requireContext().toast(getString(R.string.backup_select_location), true)
            (requireActivity() as SettingsActivity).backupLocationSelectLauncher.launch(null)
        }
        catch(ex: ActivityNotFoundException) {
            requireContext().toast(getString(R.string.no_activity_found))
        }
    }

    private fun openSaveFileSelection() {
        try {
            (requireActivity() as SettingsActivity).backupSaveFileSelectLauncher.launch(BackupManager.getBackupFilename())
        }
        catch(ex: ActivityNotFoundException) {
            requireContext().toast(getString(R.string.no_activity_found))
        }
    }

    private fun openLoadFileSelection() {
        try {
            (requireActivity() as SettingsActivity).backupLoadFileSelectLauncher.launch(
                arrayOf("application/tar+gzip", "application/gzip")
            )
        }
        catch(ex: ActivityNotFoundException) {
            requireContext().toast(getString(R.string.no_activity_found))
        }
    }

    fun resetFrequencyIfLocationUnset() {
        if(preferences.get<String>(R.string.key_backup_location).isBlank()) {
            preferences.reset<String>(R.string.key_backup_frequency)
            frequency?.value = preferences.getDefault<String>(R.string.key_backup_frequency)
        }
    }

    private fun hasStoragePermissions(): Boolean {
        requireContext().contentResolver.persistedUriPermissions.forEach {
            if(it.uri == preferences.get<String>(R.string.key_backup_location).toUri() && it.isWritePermission)
                return true
        }

        return false
    }

    fun startManualBackup(uri: Uri) {
        requireContext().contentResolver.takePersistableUriPermission(uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        BackupCreatorJob.startNow(requireActivity(), uri)
    }

    fun startManualRestore(uri: Uri) {
        MaterialAlertDialogBuilder(requireContext())
            .setItems(
                arrayOf(
                    getString(R.string.backup_restore_mode_clean),
                    getString(R.string.backup_restore_mode_dirty)
                )
            ) { dialogInterface, i ->
                BackupRestoreService.start(requireActivity(), uri, i == 1)
                dialogInterface.dismiss()
            }
            .setTitle(R.string.backup_restore_mode_title)
            .setNegativeButton(getString(android.R.string.cancel)){ _, _ -> }
            .create()
            .show()
    }

    fun updateSummaries() {
        val current = preferences.get<String>(R.string.key_backup_location)
        location?.summary = if(current.isBlank()) getString(R.string.value_not_set)
            else UniFile.fromUri(context, current.toUri()).filePath + "/automatic"
    }

    companion object {
        fun newInstance(): SettingsBackupFragment {
            return SettingsBackupFragment()
        }
    }
}