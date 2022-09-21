package me.timschneeberger.rootlessjamesdsp.fragment

import android.os.Bundle
import androidx.preference.*
import me.timschneeberger.rootlessjamesdsp.R


class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_preferences, rootKey)
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}