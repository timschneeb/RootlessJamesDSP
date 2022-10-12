package me.timschneeberger.rootlessjamesdsp.delegates

import android.app.Activity
import android.content.Context
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.model.preference.AppTheme
import me.timschneeberger.rootlessjamesdsp.utils.Constants

interface ThemingDelegate {
    fun applyAppTheme(activity: Activity)

    companion object {
        fun getThemeResIds(appTheme: AppTheme, isAmoled: Boolean): List<Int> {
            val resIds = mutableListOf<Int>()
            when (appTheme) {
                AppTheme.MONET -> {
                    resIds += R.style.Theme_RootlessJamesDSP_Monet
                }
                AppTheme.GREEN_APPLE -> {
                    resIds += R.style.Theme_RootlessJamesDSP_GreenApple
                }
                AppTheme.STRAWBERRY_DAIQUIRI -> {
                    resIds += R.style.Theme_RootlessJamesDSP_StrawberryDaiquiri
                }
                AppTheme.HONEY -> {
                    resIds += R.style.Theme_RootlessJamesDSP_Honey
                }
                AppTheme.TEALTURQUOISE -> {
                    resIds += R.style.Theme_RootlessJamesDSP_TealTurquoise
                }
                AppTheme.YINYANG -> {
                    resIds += R.style.Theme_RootlessJamesDSP_YinYang
                }
                AppTheme.YOTSUBA -> {
                    resIds += R.style.Theme_RootlessJamesDSP_Yotsuba
                }
                AppTheme.TIDAL_WAVE -> {
                    resIds += R.style.Theme_RootlessJamesDSP_TidalWave
                }
                else -> {
                    resIds += R.style.Theme_RootlessJamesDSP
                }
            }

            if (isAmoled) {
                resIds += R.style.ThemeOverlay_RootlessJamesDSP_Amoled
            }

            return resIds
        }
    }
}

// TODO centralize preferences
class ThemingDelegateImpl : ThemingDelegate {
    override fun applyAppTheme(activity: Activity) {
        val preferences = activity.getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)
        val isAmoled = preferences.getBoolean(activity.getString(R.string.key_appearance_pure_black), false)
        val appTheme = AppTheme.valueOf(preferences.getString(activity.getString(R.string.key_appearance_app_theme), AppTheme.DEFAULT.name) ?: AppTheme.DEFAULT.name)
        ThemingDelegate.getThemeResIds(appTheme, isAmoled)
            .forEach { activity.setTheme(it) }
    }
}
