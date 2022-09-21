package me.timschneeberger.rootlessjamesdsp.fragment

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.*
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R

class SettingsAboutFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_about_preferences, rootKey)

        findPreference<Preference>(getString(R.string.key_credits_version))?.summary = BuildConfig.VERSION_NAME
        val build = findPreference<Preference>(getString(R.string.key_credits_build_info))
        build?.isVisible = BuildConfig.DEBUG || BuildConfig.PREVIEW
        val type = if(BuildConfig.PREVIEW)
            "Preview"
        else if(BuildConfig.DEBUG)
            "Debug"
        else
            "Release"

        build?.summary = "${type} build @${BuildConfig.COMMIT_SHA} (compiled at ${BuildConfig.BUILD_TIME})"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
        fun newInstance(): SettingsAboutFragment {
            return SettingsAboutFragment()
        }
    }
}