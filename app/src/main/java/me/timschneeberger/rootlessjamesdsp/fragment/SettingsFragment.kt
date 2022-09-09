package me.timschneeberger.rootlessjamesdsp.fragment

import android.os.Bundle
import androidx.preference.*
import com.google.firebase.crashlytics.FirebaseCrashlytics
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.Constants


class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = Constants.PREF_APP
        setPreferencesFromResource(R.xml.app_preferences, rootKey)

        findPreference<Preference>(getString(R.string.key_share_crash_reports))?.setOnPreferenceChangeListener { _, newValue ->
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(newValue as Boolean)
            true
        }
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}