package me.timschneeberger.rootlessjamesdsp.fragment

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.preference.*
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.preference.MaterialSwitchPreference
import me.timschneeberger.rootlessjamesdsp.service.NotificationListenerService
import me.timschneeberger.rootlessjamesdsp.session.dump.DumpManager
import me.timschneeberger.rootlessjamesdsp.utils.ApplicationUtils
import me.timschneeberger.rootlessjamesdsp.utils.AssetManagerExtensions.installPrivateAssets
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.showAlert
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

class SettingsTroubleshootingFragment : PreferenceFragmentCompat() {

    private val dumpManager: DumpManager by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = Constants.PREF_APP
        setPreferencesFromResource(R.xml.app_troubleshooting_preferences, rootKey)

        findPreference<Preference>(getString(R.string.key_troubleshooting_dump))?.setOnPreferenceClickListener {
            val debug = dumpManager.collectDebugDumps()
            val path = File(requireContext().filesDir, "dump.txt")
            val output = FileOutputStream(path)
            val writer = OutputStreamWriter(output)
            val log = File(requireContext().cacheDir, "application.log")

            writer.write(debug)
            writer.write("==================> Application log\n")
            writer.flush()

            if (log.exists()) {
                try {
                    log.inputStream().use { input ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer, 0, 8192)
                                .also { read = it } >= 0
                        ) {
                            output.write(buffer, 0, read)
                            output.flush()
                        }
                    }
                }
                catch (ex: Exception) {
                    writer.write("NOTE: Failed to append log file.\n$ex\n")
                }
            }
            else {
                writer.write("NOTE: Log file does not exist\n")
            }
            writer.close()
            output.close()

            val uri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".dump_provider", path)
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            shareIntent.type = "text/plain"
            startActivity(Intent.createChooser(shareIntent, getString(R.string.troubleshooting_dump_share_title)))
            true
        }
        findPreference<Preference>(getString(R.string.key_troubleshooting_repair_assets))?.setOnPreferenceClickListener {
            requireContext().assets.installPrivateAssets(requireContext(), force = true)
            requireContext().showAlert(R.string.success, R.string.troubleshooting_repair_assets_success)
            true
        }
        findPreference<Preference>(getString(R.string.key_troubleshooting_notification_access))?.setOnPreferenceClickListener {
            val intent = ApplicationUtils.getIntentForNotificationAccess(requireContext().packageName, NotificationListenerService::class.java)
            requireActivity().startActivity(intent)
            true
        }
        findPreference<MaterialSwitchPreference>(getString(R.string.key_session_loss_ignore))?.setOnPreferenceChangeListener { _, newValue ->
            if((newValue as? Boolean) == true)
            {
                requireContext().showAlert(
                    R.string.warning,
                    R.string.session_loss_ignore_warning
                )
            }
            true
        }
        findPreference<EditTextPreference>(getString(R.string.key_session_continuous_polling_rate))?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val a = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.windowBackground, a, true)
        if (a.isColorType) {
            view.setBackgroundColor(a.data)
        } else {
            view.background = requireContext().resources.getDrawable(a.resourceId, requireContext().theme)
        }
        return view
    }

    companion object {
        fun newInstance(): SettingsTroubleshootingFragment {
            return SettingsTroubleshootingFragment()
        }
    }
}