package me.timschneeberger.rootlessjamesdsp.delegates

import android.app.Activity
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.model.preference.AppTheme
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ThemingDelegate {
    fun applyAppTheme(activity: Activity)

    companion object {
        fun getThemeResIds(appTheme: AppTheme, isAmoled: Boolean): List<Int> {
            val resIds = mutableListOf<Int>()
            resIds += when (appTheme) {
                AppTheme.MONET -> R.style.Theme_RootlessJamesDSP_Monet
                AppTheme.GREEN_APPLE -> R.style.Theme_RootlessJamesDSP_GreenApple
                AppTheme.STRAWBERRY_DAIQUIRI -> R.style.Theme_RootlessJamesDSP_StrawberryDaiquiri
                AppTheme.HONEY -> R.style.Theme_RootlessJamesDSP_Honey
                AppTheme.TEALTURQUOISE -> R.style.Theme_RootlessJamesDSP_TealTurquoise
                AppTheme.YINYANG -> R.style.Theme_RootlessJamesDSP_YinYang
                AppTheme.YOTSUBA -> R.style.Theme_RootlessJamesDSP_Yotsuba
                AppTheme.TIDAL_WAVE -> R.style.Theme_RootlessJamesDSP_TidalWave
                else -> R.style.Theme_RootlessJamesDSP
            }

            if (isAmoled) {
                resIds += R.style.ThemeOverlay_RootlessJamesDSP_Amoled
            }

            return resIds
        }
    }
}

class ThemingDelegateImpl : ThemingDelegate, KoinComponent {
    private val preferences: Preferences.App by inject()

    override fun applyAppTheme(activity: Activity) {
        val isAmoled = preferences.get<Boolean>(R.string.key_appearance_pure_black)
        val appTheme = AppTheme.valueOf(preferences.get((R.string.key_appearance_app_theme)))
        ThemingDelegate.getThemeResIds(appTheme, isAmoled).forEach { activity.setTheme(it) }
    }
}
