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
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.unregisterLocalReceiver

class QuickTileService : TileService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val app
        get() = application as MainApplication

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

        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_SERVICE_STARTED)
        filter.addAction(Constants.ACTION_SERVICE_STOPPED)
        registerLocalReceiver(broadcastReceiver, filter)
        app.prefs.registerOnSharedPreferenceChangeListener(this)

        super.onStartListening()
    }

    // Called when your app can no longer update your tile.
    override fun onStopListening() {
        app.prefs.unregisterOnSharedPreferenceChangeListener(this)
        unregisterLocalReceiver(broadcastReceiver)
        super.onStopListening()
    }

    private fun isEffectEnabled(): Boolean {
        return (BuildConfig.ROOTLESS && BaseAudioProcessorService.activeServices > 0) ||
                (!BuildConfig.ROOTLESS && getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)
                    .getBoolean(getString(R.string.key_powered_on), true))
    }

    private fun updateState() {
        qsTile.state = if(isEffectEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.updateTile()
    }

    private fun launchService() {
        Intent(this, EngineLauncherActivity::class.java)
            .apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            .let {
                if(BuildConfig.ROOTLESS)
                    startActivityAndCollapse(it)
                else
                    startActivity(it)
            }
    }


    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        super.onClick()

        val toggled = qsTile.state != Tile.STATE_ACTIVE

        // Root
        if(!BuildConfig.ROOTLESS) {
            if(BaseAudioProcessorService.activeServices <= 0) {
                launchService()
            }
            getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(getString(R.string.key_powered_on), toggled)
                .apply()
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            return

        // Rootless
        if(!toggled)
                RootlessAudioProcessorService.stop(this)
        else
            launchService()
    }
}