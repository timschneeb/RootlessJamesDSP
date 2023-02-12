package me.timschneeberger.rootlessjamesdsp.fragment

import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.preference.*
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R

class SettingsAboutFragment : PreferenceFragmentCompat() {

    private val version by lazy { findPreference<Preference>(getString(R.string.key_credits_version)) }
    private val buildInfo by lazy { findPreference<Preference>(getString(R.string.key_credits_build_info)) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_about_preferences, rootKey)

        version?.summary = BuildConfig.VERSION_NAME
        buildInfo?.isVisible = BuildConfig.DEBUG || BuildConfig.PREVIEW
        val type = if(BuildConfig.PREVIEW)
            "Preview"
        else if(BuildConfig.DEBUG)
            "Debug"
        else
            "Release"

        buildInfo?.summary = "$type (${BuildConfig.FLAVOR_dependencies}) build @${BuildConfig.COMMIT_SHA} (compiled at ${BuildConfig.BUILD_TIME})"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
        fun newInstance(): SettingsAboutFragment {
            return SettingsAboutFragment()
        }
    }
}