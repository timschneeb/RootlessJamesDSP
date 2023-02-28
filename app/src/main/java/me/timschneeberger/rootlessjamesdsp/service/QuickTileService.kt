package me.timschneeberger.rootlessjamesdsp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.EngineLauncherActivity
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.unregisterLocalReceiver

class QuickTileService : TileService() {

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_SERVICE_STARTED -> updateState()
                Constants.ACTION_SERVICE_STOPPED -> updateState()
            }
        }
    }

    // Called when your app can update your tile.
    override fun onStartListening() {
        updateState()

        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_SERVICE_STARTED)
        filter.addAction(Constants.ACTION_SERVICE_STOPPED)
        registerLocalReceiver(broadcastReceiver, filter)

        super.onStartListening()
    }

    // Called when your app can no longer update your tile.
    override fun onStopListening() {
        unregisterLocalReceiver(broadcastReceiver)
        super.onStopListening()
    }

    private fun isEffectEnabled(): Boolean {
        return (BuildConfig.ROOTLESS && BaseAudioProcessorService.activeServices > 0) ||
                (!BuildConfig.ROOTLESS && getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)
                    .getBoolean(getString(R.string.key_powered_on), true))
    }

    private fun updateState() {
        val online = isEffectEnabled()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            qsTile.stateDescription = if(online) "On" else "Off"
        }
        qsTile.subtitle = if(online) "On" else "Off"
        qsTile.state = if(online) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

        qsTile.updateTile()
    }

    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        if(!BuildConfig.ROOTLESS) {
            // TODO root: handle dead service
            getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(getString(R.string.key_powered_on), !isEffectEnabled())
                .apply()
            return
        }

        // Rootless
        if(isEffectEnabled()) {
            RootlessAudioProcessorService.stop(this)
        }
        else {
            startActivityAndCollapse(
                Intent(this, EngineLauncherActivity::class.java)
                    .apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    }
            )
        }
        super.onClick()
    }
}