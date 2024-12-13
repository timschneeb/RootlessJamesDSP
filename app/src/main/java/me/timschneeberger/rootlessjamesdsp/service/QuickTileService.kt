package me.timschneeberger.rootlessjamesdsp.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import me.timschneeberger.rootlessjamesdsp.MainApplication
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.EngineUtils.toggleEnginePower
import me.timschneeberger.rootlessjamesdsp.utils.SdkCheck
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.PermissionExtensions.hasProjectMediaAppOp
import me.timschneeberger.rootlessjamesdsp.utils.isRootless
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
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
        return (isRootless() && BaseAudioProcessorService.activeServices > 0) ||
                (!isRootless() && preferences.get<Boolean>(R.string.key_powered_on))
    }

    private fun updateState() {
        qsTile?.let { tile ->
            tile.state = if(isEffectEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        super.onClick()

        val toggled = qsTile?.let { it.state != Tile.STATE_ACTIVE } ?: return
        toggleEnginePower(toggled) { intent ->
            val pending = PendingIntent.getActivity(app, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            // If projection permission request needs to be shown, collapse status bar
            if (isRootless() && app.mediaProjectionStartIntent == null && !hasProjectMediaAppOp() && !SdkCheck.isVanillaIceCream) {
                if(SdkCheck.isUpsideDownCake)
                    startActivityAndCollapse(pending)
                else
                    startActivityAndCollapse(intent)
            }
            else
                startActivity(intent)
        }
    }
}