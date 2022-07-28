package me.timschneeberger.rootlessjamesdsp.utils

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import timber.log.Timber


class VolumeContentObserver(val context: Context, handler: Handler? = null) : ContentObserver(handler) {
    private var previousVolume: Int
    private var volChangeListener: ((Int) -> Unit)? = null
    private var audio: AudioManager

    fun setOnVolumeChangeListener(l: (Int) -> Unit) {
        volChangeListener = l
    }

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        val currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val delta = previousVolume - currentVolume
        if (delta > 0) {
            Timber.tag("SettingsContentObserver").d("Volume decreased $currentVolume")
            previousVolume = currentVolume
            volChangeListener?.invoke(currentVolume)
        } else if (delta < 0) {
            Timber.tag("SettingsContentObserver").d("Volume increased $currentVolume")
            previousVolume = currentVolume
            volChangeListener?.invoke(currentVolume)
        }
    }

    init {
        audio = SystemServices.get(context, AudioManager::class.java)
        previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        volChangeListener?.invoke(previousVolume)
    }
}