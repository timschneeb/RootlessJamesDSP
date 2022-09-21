package me.timschneeberger.rootlessjamesdsp.fragment

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.*
import com.google.firebase.crashlytics.FirebaseCrashlytics
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.Constants

class SettingsMiscFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = Constants.PREF_APP
        setPreferencesFromResource(R.xml.app_misc_preferences, rootKey)

        findPreference<Preference>(getString(R.string.key_share_crash_reports))?.setOnPreferenceChangeListener { _, newValue ->
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(newValue as Boolean)
            true
        }
        findPreference<Preference>(getString(R.string.key_debug_database))?.isVisible = BuildConfig.DEBUG
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
        fun newInstance(): SettingsMiscFragment {
            return SettingsMiscFragment()
        }
    }
}