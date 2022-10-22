package me.timschneeberger.rootlessjamesdsp.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.*
import android.media.audiofx.AudioEffect
import android.os.*
import android.util.SparseArray
import androidx.core.util.forEach
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

class RootAudioProcessorService : BaseAudioProcessorService(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    EffectSessionManager.OnSessionChangeListener {

    // System services
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager

    // Termination flags
    private var isServiceDisposing = false

    // App object
    private val app
        get() = application as MainApplication

    override fun onCreate() {
        super.onCreate()

        // Register shared preferences listener
        app.prefs.registerOnSharedPreferenceChangeListener(this)
        app.rootSessionManager.registerOnSessionChangeListener(this)

        // Get reference to system services
        audioManager = SystemServices.get(this, AudioManager::class.java)
        notificationManager = SystemServices.get(this, NotificationManager::class.java)

        // Register notification channels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                getString(R.string.notification_channel_service),
                NotificationManager.IMPORTANCE_NONE
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Launch foreground service
        val notification = if (app.isLegacyMode)
            ServiceNotificationHelper.createServiceNotificationLegacy(this)
        else
            ServiceNotificationHelper.createServiceNotification(this, arrayOf())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID_SERVICE,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(
                NOTIFICATION_ID_SERVICE,
                notification
            )
        }

        // Initialize shared preferences manually
        onSharedPreferenceChanged(app.prefs, getString(R.string.key_powered_on))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand")
        if (intent == null)
            return START_STICKY

        // Handle intent action
        when (intent.action) {
            null -> {
                Timber.wtf("onStartCommand: intent.action is null")
            }
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                if(!JamesDspRemoteEngine.isPluginInstalled()) {
                    Timber.e("onStartCommand: Ignoring open request because plugin is not installed")
                    stopSelf()
                    return START_NOT_STICKY
                }

                app.rootSessionManager.addSession(intent)
            }
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
                MainScope().launch {
                    if (sessionId != 0)
                        delay(800)
                    app.rootSessionManager.removeSession(intent)
                    if (app.rootSessionManager.sessionList.isEmpty()) {
                        stopSelf()
                    }
                }
            }
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceDisposing = true

        // Stop foreground service
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            stopForeground(STOP_FOREGROUND_REMOVE)
        else
            stopForeground(true)

        // Notify app about service termination and unregister
        sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_STOPPED))

        app.prefs.unregisterOnSharedPreferenceChangeListener(this)
        app.rootSessionManager.unregisterOnSessionChangeListener(this)

        notificationManager.cancel(NOTIFICATION_ID_SERVICE)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            getString(R.string.key_audioformat_legacymode) -> { updateServiceNotification() }
            getString(R.string.key_powered_on) -> {
                app.rootSessionManager.enabled = sharedPreferences?.getBoolean(key, true) ?: true
                updateServiceNotification()
            }
        }
    }

    override fun onSessionChanged(sessionList: SparseArray<EffectSessionEntry>) {
        updateServiceNotification()
    }

    private fun updateServiceNotification() {
        if(app.prefs.getBoolean(getString(R.string.key_audioformat_legacymode), true))
            ServiceNotificationHelper.pushServiceNotificationLegacy(this)
        else
            ServiceNotificationHelper.pushServiceNotificationRoot(this, app.rootSessionManager.sessionList)
    }

    companion object {
        fun startService(context: Context, intent: Intent) {
            Timber.d("startService: intent=$intent")

            // Prevent launch if plugin missing
            if(!JamesDspRemoteEngine.isPluginInstalled()) {
                Timber.e("Service launch cancelled. Plugin not installed.")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }

        fun updateLegacyMode(context: Context, enable: Boolean) {
            Timber.d("updateLegacyMode: enable=$enable")

            val action =
                if (enable) AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION
                else AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION
            Intent(context, RootAudioProcessorService::class.java)
                .apply { this.action = action }
                .apply { putExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0) }
                .run { startService(context, this) }
        }
    }
}
