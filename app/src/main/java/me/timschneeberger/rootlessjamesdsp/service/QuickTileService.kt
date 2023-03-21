package me.timschneeberger.rootlessjamesdsp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.MainApplication
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.EngineLauncherActivity
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.PermissionExtensions.hasProjectMediaAppOp
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import me.timschneeberger.rootlessjamesdsp.utils.sdkAbove
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent

class QuickTileService : TileService(),
    SharedPreferences.OnSharedPreferenceChangeListener, KoinComponent {

    private val app
        get() = application as MainApplication

    private val preferences: Preferences.App by inject()

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_SERVICE_STARTED -> updateState()
                Constants.ACTION_SERVICE_STOPPED -> updateState()
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            getString(R.string.key_powered_on) -> updateState()
        }
    }

    // Called when your app can update your tile.
    override fun onStartListening() {
        updateState()

        registerLocalReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(Constants.ACTION_SERVICE_STARTED)
            addAction(Constants.ACTION_SERVICE_STOPPED)
        })
        preferences.registerOnSharedPreferenceChangeListener(this)

        super.onStartListening()
    }

    // Called when your app can no longer update your tile.
    override fun onStopListening() {
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        unregisterLocalReceiver(broadcastReceiver)
        super.onStopListening()
    }

    private fun isEffectEnabled(): Boolean {
        return (BuildConfig.ROOTLESS && BaseAudioProcessorService.activeServices > 0) ||
                (!BuildConfig.ROOTLESS && preferences.get<Boolean>(R.string.key_powered_on))
    }

    private fun updateState() {
        qsTile?.let { tile ->
            tile.state = if(isEffectEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    private fun launchService() {
        Intent(this, EngineLauncherActivity::class.java)
            .apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            .also {
                // If projection permission request needs to be shown, collapse status bar
                if(BuildConfig.ROOTLESS && app.mediaProjectionStartIntent == null && !hasProjectMediaAppOp())
                    startActivityAndCollapse(it)
                else
                    startActivity(it)
            }
    }


    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        super.onClick()

        val toggled = qsTile?.let { it.state != Tile.STATE_ACTIVE } ?: return

        // Root
        if(!BuildConfig.ROOTLESS) {
            if(BaseAudioProcessorService.activeServices <= 0) {
                launchService()
            }
            preferences.set(R.string.key_powered_on, toggled)
            return
        }

        sdkAbove(Build.VERSION_CODES.Q) {
            // Rootless
            if (!toggled)
                RootlessAudioProcessorService.stop(this)
            else
                launchService()
        }
    }
}