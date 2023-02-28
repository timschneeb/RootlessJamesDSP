package me.timschneeberger.rootlessjamesdsp.service

import android.content.Intent
import android.service.quicksettings.TileService
import android.speech.tts.TextToSpeech.Engine
import me.timschneeberger.rootlessjamesdsp.activity.EngineLauncherActivity

class QuickTileService : TileService() {

    // Called when the user adds your tile.
    override fun onTileAdded() {
        super.onTileAdded()
    }
    // Called when your app can update your tile.
    override fun onStartListening() {
        super.onStartListening()
    }

    // Called when your app can no longer update your tile.
    override fun onStopListening() {
        super.onStopListening()
    }

    fun updateState() {
        qsTile.subtitle = "Off"
    }

    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        /*startActivity(
            Intent(this, EngineLauncherActivity::class.java)
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        )*/
        startActivityAndCollapse(
            Intent(this, EngineLauncherActivity::class.java)
                .apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                }
        )

        super.onClick()
    }
    // Called when the user removes your tile.
    override fun onTileRemoved() {
        super.onTileRemoved()
    }
}