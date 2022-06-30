package me.timschneeberger.rootlessjamesdsp

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.util.Log


class VolumeContentObserver(c: Context, handler: Handler? = null) : ContentObserver(handler) {
    var previousVolume: Int
    var context: Context
    var volChangeListener: ((Int) -> Unit)? = null

    fun setOnVolumeChangeListener(l: (Int) -> Unit) {
        volChangeListener = l
    }

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val delta = previousVolume - currentVolume
        if (delta > 0) {
            Log.d("SettingsContentObserver", "Volume decreased $currentVolume")
            previousVolume = currentVolume
            volChangeListener?.invoke(currentVolume)
        } else if (delta < 0) {
            Log.d("SettingsContentObserver", "Volume increased $currentVolume")
            previousVolume = currentVolume
            volChangeListener?.invoke(currentVolume)
        }
    }

    init {
        context = c
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        volChangeListener?.invoke(previousVolume)
    }
}