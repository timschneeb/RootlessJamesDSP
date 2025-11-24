package me.timschneeberger.rootlessjamesdsp.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.EngineLauncherActivity
import me.timschneeberger.rootlessjamesdsp.service.BaseAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.service.RootlessAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object EngineUtils : KoinComponent {
    private val preferences: Preferences.App by inject()

    private fun Context.launchService(activityStarter: (Intent) -> Unit) {
        Intent(this, EngineLauncherActivity::class.java)
            .apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            .also(activityStarter)
    }

    fun Context.toggleEnginePower(isOn: Boolean, activityStarter: (Intent) -> Unit = { startActivity(it) }) {
        // Root/plugin
        if(!isRootless()) {
            if(isRoot() && BaseAudioProcessorService.activeServices <= 0) {
                launchService(activityStarter)
            }
            preferences.set(R.string.key_powered_on, isOn)
            return

        }

        // Rootless
        if (!isOn) {
            RootlessAudioProcessorService.stop(this)
            preferences.set(R.string.key_powered_on, false)
        } else {
            // Intentar iniciar con el token guardado
            val mainApp = applicationContext as? me.timschneeberger.rootlessjamesdsp.MainApplication
            val projectionIntent = mainApp?.mediaProjectionStartIntent

            if (projectionIntent != null) {
                // Tenemos token, iniciar directamente
                RootlessAudioProcessorService.start(this, projectionIntent)
                preferences.set(R.string.key_powered_on, true)
            } else {
                // No tenemos token, abrir la app para pedirlo
                launchService(activityStarter)
            }
        }
    }
    fun isEngineEnabled(context: Context): Boolean {
        return preferences.get(R.string.key_powered_on, false, Boolean::class)
    }
}