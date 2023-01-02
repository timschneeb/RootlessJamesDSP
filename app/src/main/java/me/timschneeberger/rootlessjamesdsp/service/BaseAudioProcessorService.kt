package me.timschneeberger.rootlessjamesdsp.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

abstract class BaseAudioProcessorService : Service() {
    private val binder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: BaseAudioProcessorService
            get() = this@BaseAudioProcessorService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        activeServices++
        super.onCreate()
    }

    override fun onDestroy() {
        activeServices--
        super.onDestroy()
    }

    companion object {
        var activeServices: Int = 0
            private set
    }
}
