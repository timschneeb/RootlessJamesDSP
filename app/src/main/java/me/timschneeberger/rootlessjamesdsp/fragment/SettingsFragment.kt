package me.timschneeberger.rootlessjamesdsp.fragment

import android.os.Bundle
import androidx.preference.*
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R


class SettingsFragment : PreferenceFragmentCompat() {

    private val troubleshooting by lazy { findPreference<Preference>(getString(R.string.key_troubleshooting)) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_preferences, rootKey)

        troubleshooting?.isVisible = BuildConfig.ROOTLESS
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}