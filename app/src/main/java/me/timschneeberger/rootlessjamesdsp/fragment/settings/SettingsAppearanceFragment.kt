package me.timschneeberger.rootlessjamesdsp.fragment.settings

import android.os.Bundle
import androidx.preference.*
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.model.preference.AppTheme
import me.timschneeberger.rootlessjamesdsp.model.preference.ThemeMode
import me.timschneeberger.rootlessjamesdsp.preference.ThemesPreference
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.isDynamicColorAvailable

class SettingsAppearanceFragment : SettingsBaseFragment() {

    private val themeMode by lazy { findPreference<ListPreference>(getString(R.string.key_appearance_theme_mode)) }
    private val amoledMode by lazy { findPreference<Preference>(getString(R.string.key_appearance_pure_black)) }
    private val appTheme by lazy { findPreference<ThemesPreference>(getString(R.string.key_appearance_app_theme)) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = Constants.PREF_APP
        setPreferencesFromResource(R.xml.app_appearance_preferences, rootKey)

        val appThemes = AppTheme.values().filter {
            val monetFilter = if (it == AppTheme.MONET) {
                isDynamicColorAvailable
            } else {
                true
            }
            it.titleResId != null && monetFilter
        }
        appTheme?.entries = appThemes

        themeMode?.setOnPreferenceChangeListener { _, _ ->
            updateViewStates()
            true
        }
        updateViewStates()

        savedInstanceState?.let {
            appTheme?.lastScrollPosition = it.getInt(STATE_THEMES_SCROLL_POSITION, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        appTheme?.let {
            outState.putInt(STATE_THEMES_SCROLL_POSITION, it.lastScrollPosition ?: 0)
        }
        super.onSaveInstanceState(outState)
    }

    private fun updateViewStates(){
        amoledMode?.isVisible = themeMode?.value?.toIntOrNull()?.let { ThemeMode.fromInt(it) } != ThemeMode.Light
    }

    companion object {
        private const val STATE_THEMES_SCROLL_POSITION = "stateThemesScrollPosition"

        fun newInstance(): SettingsAppearanceFragment {
            return SettingsAppearanceFragment()
        }
    }
}