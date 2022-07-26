package me.timschneeberger.rootlessjamesdsp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.doubledot.doki.ui.DokiActivity
import me.timschneeberger.rootlessjamesdsp.databinding.ActivitySettingsBinding
import me.timschneeberger.rootlessjamesdsp.dump.DumpManager
import me.timschneeberger.rootlessjamesdsp.preference.MaterialSwitchPreference
import me.timschneeberger.rootlessjamesdsp.service.NotificationListenerService
import me.timschneeberger.rootlessjamesdsp.utils.ApplicationUtils
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.showAlert
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.settingsToolbar)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.settingsToolbar.setNavigationOnClickListener { onBackPressed() }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = Constants.PREF_APP
            setPreferencesFromResource(R.xml.app_preferences, rootKey)

            findPreference<EditTextPreference>(getString(R.string.key_session_continuous_polling_rate))?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            findPreference<Preference>(getString(R.string.key_share_crash_reports))?.setOnPreferenceChangeListener { _, newValue ->
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(newValue as Boolean)
                true
            }
            findPreference<MaterialSwitchPreference>(getString(R.string.key_session_loss_ignore))?.setOnPreferenceChangeListener { _, newValue ->
                if((newValue as? Boolean) == true)
                {
                    requireContext().showAlert(R.string.warning, R.string.session_loss_ignore_warning)
                }
                true
            }
            findPreference<Preference>(getString(R.string.key_troubleshooting_dump))?.setOnPreferenceClickListener {
                val debug = DumpManager.get(requireContext()).collectDebugDumps()
                val path = requireContext().filesDir.absolutePath + "/dump.txt"
                val output = FileOutputStream(path)
                val writer = OutputStreamWriter(output)
                writer.write(debug)
                writer.close()
                output.close()

                val uri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".dump_provider", File(path))
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                shareIntent.type = "text/plain"
                startActivity(Intent.createChooser(shareIntent, getString(R.string.troubleshooting_dump_share_title)))
                true
            }
            findPreference<Preference>(getString(R.string.key_troubleshooting_dontkillmyapp))?.setOnPreferenceClickListener {
                DokiActivity.start(requireContext())
                true
            }
            findPreference<Preference>(getString(R.string.key_troubleshooting_notification_access))?.setOnPreferenceClickListener {
                val intent = ApplicationUtils.getIntentForNotificationAccess(requireContext().packageName, NotificationListenerService::class.java)
                requireActivity().startActivity(intent)
                true
            }
        }
    }
}