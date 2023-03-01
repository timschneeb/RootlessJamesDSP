package me.timschneeberger.rootlessjamesdsp.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.*
import android.media.audiofx.AudioEffect
import android.os.*
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.MainApplication
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.ServiceNotificationHelper
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspRemoteEngine
import me.timschneeberger.rootlessjamesdsp.model.IEffectSession
import me.timschneeberger.rootlessjamesdsp.model.room.AppBlocklistDatabase
import me.timschneeberger.rootlessjamesdsp.model.room.AppBlocklistRepository
import me.timschneeberger.rootlessjamesdsp.model.room.BlockedApp
import me.timschneeberger.rootlessjamesdsp.model.root.RemoteEffectSession
import me.timschneeberger.rootlessjamesdsp.session.root.OnRootSessionChangeListener
import me.timschneeberger.rootlessjamesdsp.session.root.RootSessionDumpManager
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.Constants.CHANNEL_ID_SERVICE
import me.timschneeberger.rootlessjamesdsp.utils.Constants.NOTIFICATION_ID_SERVICE
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.SystemServices
import timber.log.Timber

class RootAudioProcessorService : BaseAudioProcessorService(),
    SharedPreferences.OnSharedPreferenceChangeListener, OnRootSessionChangeListener {

    // System services
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager

    // Termination flags
    private var isServiceDisposing = false

    // Enhanced processing
    private var sessionDumpManager: RootSessionDumpManager? = null

    // Room databases
    private val applicationScope = CoroutineScope(SupervisorJob())
    private val blockedAppDatabase by lazy { AppBlocklistDatabase.getDatabase(this, applicationScope) }
    private val blockedAppRepository by lazy { AppBlocklistRepository(blockedAppDatabase.appBlocklistDao()) }
    private val blockedApps by lazy { blockedAppRepository.blocklist.asLiveData() }
    private val blockedAppObserver = Observer<List<BlockedApp>?> { apps ->
        if(!app.isEnhancedProcessing)
            return@Observer

        apps?.map { it.uid }?.let { uids ->
            Timber.d("blockedAppObserver: Excluded UIDs: ${uids.joinToString("; ")}")

            app.rootSessionDatabase.setExcludedUids(uids.toTypedArray())
            sessionDumpManager?.pollOnce(false)
        }
    }


    // App object
    private val app
        get() = application as MainApplication

    override fun onCreate() {
        super.onCreate()

        // Register shared preferences listener
        app.prefs.registerOnSharedPreferenceChangeListener(this)
        app.rootSessionDatabase.registerOnSessionChangeListener(this)

        // Get reference to system services
        audioManager = SystemServices.get(this, AudioManager::class.java)
        notificationManager = SystemServices.get(this, NotificationManager::class.java)

        // Setup database observer
        blockedApps.observeForever(blockedAppObserver)

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
        arrayOf(R.string.key_powered_on, R.string.key_audioformat_enhancedprocessing).forEach {
            onSharedPreferenceChanged(app.prefs, getString(it))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand")
        if (intent == null)
            return START_STICKY

        if(!JamesDspRemoteEngine.isPluginInstalled()) {
            Timber.e("onStartCommand: Ignoring command because plugin is not installed")
            stopSelf()
            return START_NOT_STICKY
        }

        // Handle intent action
        when (intent.action) {
            null -> {
                Timber.wtf("onStartCommand: intent.action is null")
            }
            ACTION_START_ENHANCED_PROCESSING -> {
                if(!app.isEnhancedProcessing) {
                    Timber.e("onStartCommand: ACTION_START_ENHANCED_PROCESSING received, but isEnhancedProcessing is false")
                }
                sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_STARTED))
            }
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                if(app.rootSessionDatabase.sessionList.isEmpty())
                    sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_STARTED))

                if(!isServiceDisposing && !app.isEnhancedProcessing)
                    app.rootSessionDatabase.addSessionByIntent(intent)
            }
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                if(!app.isEnhancedProcessing) {
                    val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
                    MainScope().launch {
                        if (sessionId != 0)
                            delay(800)
                        app.rootSessionDatabase.removeSessionByIntent(intent)
                        if (app.rootSessionDatabase.sessionList.isEmpty()) {
                            stopSelf()
                        }
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
        stopForeground(STOP_FOREGROUND_REMOVE)

        // Unregister database observer
        blockedApps.removeObserver(blockedAppObserver)

        // Notify app about service termination and unregister
        sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_STOPPED))

        app.rootSessionDatabase.clearSessions()

        app.prefs.unregisterOnSharedPreferenceChangeListener(this)
        app.rootSessionDatabase.unregisterOnSessionChangeListener(this)

        notificationManager.cancel(NOTIFICATION_ID_SERVICE)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            getString(R.string.key_audioformat_legacymode) -> { updateServiceNotification() }
            getString(R.string.key_powered_on) -> {
                app.rootSessionDatabase.enabled = sharedPreferences?.getBoolean(key, true) ?: true
                updateServiceNotification()
            }
            getString(R.string.key_audioformat_enhancedprocessing) -> {
                // If switched to enhanced processing while the service was already active...
                if(app.isEnhancedProcessing) {
                    setupEnhancedProcessing()
                }
                else {
                    sessionDumpManager?.destroy()
                    sessionDumpManager = null
                    app.rootSessionDatabase.setExcludedUids(arrayOf())
                    app.rootSessionDatabase.clearSessions()
                }
            }
        }
    }

    private fun setupEnhancedProcessing() {
        app.rootSessionDatabase.clearSessions()

        sessionDumpManager = RootSessionDumpManager(this)
        sessionDumpManager?.setOnSessionDump(app.rootSessionDatabase::update)
        sessionDumpManager?.setOnDumpMethodChanged(app.rootSessionDatabase::clearSessions)
        sessionDumpManager?.pollOnce(false)
    }

    override fun onSessionChanged(sessionList: HashMap<Int, IEffectSession>) {
        updateServiceNotification()
    }

    private fun updateServiceNotification() {
        if(app.prefs.getBoolean(getString(R.string.key_audioformat_legacymode), true))
            ServiceNotificationHelper.pushServiceNotificationLegacy(this)
        else
            ServiceNotificationHelper.pushServiceNotification(this, app.rootSessionDatabase.sessionList.values.toTypedArray())
    }

    companion object {
        const val ACTION_START_ENHANCED_PROCESSING = BuildConfig.APPLICATION_ID + ".root.service.START_ENHANCED"
        const val ACTION_STOP = BuildConfig.APPLICATION_ID + ".root.service.STOP"

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

        fun startServiceEnhanced(context: Context) {
            Intent(context, RootAudioProcessorService::class.java)
                .apply { this.action = ACTION_START_ENHANCED_PROCESSING }
                .run { startService(context, this) }
        }

        fun stopService(context: Context) {
            Intent(context, RootAudioProcessorService::class.java)
                .apply { this.action = ACTION_STOP }
                .run { startService(context, this) }
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
