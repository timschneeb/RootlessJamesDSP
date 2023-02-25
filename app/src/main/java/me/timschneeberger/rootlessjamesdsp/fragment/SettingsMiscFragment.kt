package me.timschneeberger.rootlessjamesdsp.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.preference.*
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.api.AutoEqClient
import me.timschneeberger.rootlessjamesdsp.flavor.CrashlyticsImpl
import me.timschneeberger.rootlessjamesdsp.utils.AssetManagerExtensions.installPrivateAssets
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.showAlert
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.showYesNoAlert
import java.util.Locale

class SettingsMiscFragment : PreferenceFragmentCompat() {

    private val autoStartNotify by lazy { findPreference<Preference>(getString(R.string.key_autostart_prompt_at_boot)) }
    private val repairAssets by lazy { findPreference<Preference>(getString(R.string.key_troubleshooting_repair_assets)) }
    private val crashReports by lazy { findPreference<Preference>(getString(R.string.key_share_crash_reports)) }
    private val aeqApiUrl by lazy { findPreference<EditTextPreference>(getString(R.string.key_network_autoeq_api_url)) }
    private val debugDatabase by lazy { findPreference<Preference>(getString(R.string.key_debug_database)) }

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
                Toast.makeText(
                    requireContext(),
                    getString(R.string.network_invalid_url),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnPreferenceChangeListener false
            }

            // Verify URL by performing a connection test
            try {
                val client = AutoEqClient(requireContext(), 5, newValue.toString())
                Toast.makeText(
                    requireContext(),
                    R.string.network_autoeq_conntest_running,
                    Toast.LENGTH_SHORT
                ).show()

                client.queryProfiles(
                    "conntest",
                    onResponse = { _, _ ->
                        context?.let {
                            Toast.makeText(it,
                                R.string.network_autoeq_conntest_done,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onFailure = { error ->
                        context?.showYesNoAlert(
                            getString(R.string.network_autoeq_conntest_fail),
                            getString(R.string.network_autoeq_conntest_fail_summary, error)
                        ) {
                            if (it) {
                                // Restore default URL if requested
                                context
                                    ?.getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)
                                    ?.edit()
                                    ?.putString(
                                        getString(R.string.key_network_autoeq_api_url),
                                        AutoEqClient.DEFAULT_API_URL
                                    )
                                    ?.apply()

                                aeqApiUrl?.text = AutoEqClient.DEFAULT_API_URL
                            }
                        }
                    }
                )
            }
            catch(ex: IllegalArgumentException) {
                // Handle invalid base url argument in retrofit
                Toast.makeText(
                    requireContext(),
                    getString(R.string.network_invalid_url),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnPreferenceChangeListener false
            }

            true
        }

        crashReports?.parent?.isVisible = !BuildConfig.FOSS_ONLY
        debugDatabase?.parent?.isVisible = BuildConfig.DEBUG
        autoStartNotify?.parent?.isVisible = BuildConfig.ROOTLESS
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val a = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.windowBackground, a, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && a.isColorType) {
            view.setBackgroundColor(a.data)
        } else {
            view.background = ResourcesCompat.getDrawable(requireContext().resources, a.resourceId, requireContext().theme)
        }
        return view
    }

    companion object {
        fun newInstance(): SettingsMiscFragment {
            return SettingsMiscFragment()
        }
    }
}