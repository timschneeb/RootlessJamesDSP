package me.timschneeberger.rootlessjamesdsp.fragment.settings

import android.os.Bundle
import android.util.Patterns
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.api.AutoEqClient
import me.timschneeberger.rootlessjamesdsp.flavor.CrashlyticsImpl
import me.timschneeberger.rootlessjamesdsp.utils.extensions.AssetManagerExtensions.installPrivateAssets
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showYesNoAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import org.koin.android.ext.android.inject
import java.util.Locale

class SettingsMiscFragment : SettingsBaseFragment() {

    private val autoStartNotify by lazy { findPreference<Preference>(getString(R.string.key_autostart_prompt_at_boot)) }
    private val repairAssets by lazy { findPreference<Preference>(getString(R.string.key_troubleshooting_repair_assets)) }
    private val crashReports by lazy { findPreference<Preference>(getString(R.string.key_share_crash_reports)) }
    private val aeqApiUrl by lazy { findPreference<EditTextPreference>(getString(R.string.key_network_autoeq_api_url)) }
    private val debugDatabase by lazy { findPreference<Preference>(getString(R.string.key_debug_database)) }

    private val preferences: Preferences.App by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = Constants.PREF_APP
        setPreferencesFromResource(R.xml.app_misc_preferences, rootKey)

        crashReports?.setOnPreferenceChangeListener { _, newValue ->
            CrashlyticsImpl.setCollectionEnabled(newValue as Boolean)
            true
        }

        repairAssets?.setOnPreferenceClickListener {
            requireContext().assets.installPrivateAssets(requireContext(), force = true)
            requireContext().showAlert(R.string.success, R.string.troubleshooting_repair_assets_success)
            true
        }

        aeqApiUrl?.setOnPreferenceChangeListener { _, newValue ->
            if (!Patterns.WEB_URL.matcher(newValue.toString().lowercase(Locale.ROOT)).matches()) {
                requireContext().toast(R.string.network_invalid_url)
                return@setOnPreferenceChangeListener false
            }

            // Verify URL by performing a connection test
            try {
                val client = AutoEqClient(requireContext(), 5, newValue.toString())
                requireContext().toast(R.string.network_autoeq_conntest_running)

                client.queryProfiles(
                    "conntest",
                    onResponse = { _, _ ->
                        context?.let {
                            it.toast(R.string.network_autoeq_conntest_done, false)
                        }
                    },
                    onFailure = { error ->
                        context?.showYesNoAlert(
                            getString(R.string.network_autoeq_conntest_fail),
                            getString(R.string.network_autoeq_conntest_fail_summary, error)
                        ) {
                            if (it) {
                                // Restore default URL if requested
                                preferences.reset<String>(R.string.key_network_autoeq_api_url)
                                aeqApiUrl?.text = preferences.getDefault(R.string.key_network_autoeq_api_url)
                            }
                        }
                    }
                )
            }
            catch(ex: IllegalArgumentException) {
                // Handle invalid base url argument in retrofit
                requireContext().toast(R.string.network_invalid_url)
                return@setOnPreferenceChangeListener false
            }

            true
        }

        crashReports?.parent?.isVisible = !BuildConfig.FOSS_ONLY
        debugDatabase?.parent?.isVisible = BuildConfig.DEBUG
        autoStartNotify?.isVisible = BuildConfig.ROOTLESS
    }

    companion object {
        fun newInstance(): SettingsMiscFragment {
            return SettingsMiscFragment()
        }
    }
}