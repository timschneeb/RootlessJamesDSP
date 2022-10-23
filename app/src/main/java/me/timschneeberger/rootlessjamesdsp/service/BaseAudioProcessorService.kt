package me.timschneeberger.rootlessjamesdsp.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.*
import android.media.audiofx.AudioEffect
import android.os.*
import android.util.SparseArray
import androidx.core.util.isEmpty
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.MainApplication
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.ServiceNotificationHelper
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspLocalEngine
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspRemoteEngine
import me.timschneeberger.rootlessjamesdsp.interop.ProcessorMessageHandler
import me.timschneeberger.rootlessjamesdsp.model.root.EffectSessionEntry
import me.timschneeberger.rootlessjamesdsp.session.root.EffectSessionManager
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SERVICE_HARD_REBOOT_CORE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SERVICE_RELOAD_LIVEPROG
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_SERVICE_SOFT_REBOOT_CORE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.ACTION_PREFERENCES_UPDATED
import me.timschneeberger.rootlessjamesdsp.utils.Constants.CHANNEL_ID_SERVICE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.NOTIFICATION_ID_SERVICE
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.SystemServices
import timber.log.Timber

abstract class BaseAudioProcessorService : Service() {
    private val binder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: BaseAudioProcessorService
            get() = this@BaseAudioProcessorService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }
}
