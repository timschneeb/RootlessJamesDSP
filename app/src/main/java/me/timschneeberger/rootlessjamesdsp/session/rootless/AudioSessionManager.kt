package me.timschneeberger.rootlessjamesdsp.session.rootless

import android.annotation.SuppressLint
import android.content.*
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.audiofx.AudioEffect
import android.media.session.MediaController
import android.media.session.MediaSessionHidden
import android.media.session.MediaSessionManager
import android.os.*
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.session.dump.DumpManager
import me.timschneeberger.rootlessjamesdsp.model.preference.SessionUpdateMode
import me.timschneeberger.rootlessjamesdsp.service.NotificationListenerService
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionPolicyInfoDump
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.SystemServices
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber


class AudioSessionManager(val context: Context) : DumpManager.OnDumpMethodChangeListener,
    BroadcastReceiver(), MediaSessionManager.OnActiveSessionsChangedListener, KoinComponent
{
    // System services
    private val audioManager = SystemServices.get(context, AudioManager::class.java)
    private val sessionManager = SystemServices.get(context, MediaSessionManager::class.java)

    // Session dump manager
    private val dumpManager: DumpManager by inject()

    // Session polling settings
    private var sessionUpdateMode: SessionUpdateMode = SessionUpdateMode.Listener
        set(value) {
            field = value
            updatePollingMode()
        }
    private var pollingTimeout = 3000L

    // Polling job
    private val pollingMutex = Mutex()
    private val pollingScope = CoroutineScope(Dispatchers.Main)
    private var continuousPollingJob: Job? = null

    // Callbacks
    @RequiresApi(VERSION_CODES.O)
    private var audioPlaybackCallback: AudioManager.AudioPlaybackCallback? = null

    // Preferences
    private val preferencesListener: SharedPreferences.OnSharedPreferenceChangeListener
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)

    // Session database
    val sessionDatabase: MutedSessionManager = MutedSessionManager(context)
    // Session policy database
    val sessionPolicyDatabase: SessionRecordingPolicyManager = SessionRecordingPolicyManager(context)

    init {
        // Notify on playback changes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioPlaybackCallback = object : AudioManager.AudioPlaybackCallback() {
                override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
                    super.onPlaybackConfigChanged(configs)

                    Timber.d("Playback config changed")
                    pollOnce(false)
                }
            }
            audioManager.registerAudioPlaybackCallback(audioPlaybackCallback!!, Handler(Looper.getMainLooper()))
        }

        // Register callbacks
        dumpManager.registerOnDumpMethodChangeListener(this)
        context.registerLocalReceiver(this, IntentFilter(Constants.ACTION_SESSION_CHANGED))
        installNotificationListenerService()

        // Setup preferences
        preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener {
                _, key ->
            loadFromPreferences(key)
        }
        loadFromPreferences(context.getString(R.string.key_session_continuous_polling))
        loadFromPreferences(context.getString(R.string.key_session_continuous_polling_rate))
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesListener)
    }

    fun destroy()
    {
        dumpManager.unregisterOnDumpMethodChangeListener(this)

        if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
            audioPlaybackCallback?.let { audioManager.unregisterAudioPlaybackCallback(it) }
        }

        sessionManager.removeOnActiveSessionsChangedListener(this)
        context.unregisterLocalReceiver(this)

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferencesListener)

        sessionDatabase.destroy()
        sessionPolicyDatabase.destroy()
    }

    private fun loadFromPreferences(key: String){
        when (key) {
            context.getString(R.string.key_session_continuous_polling) -> {
                sessionUpdateMode =
                    if (sharedPreferences.getBoolean(key, false))
                        SessionUpdateMode.ContinuousPolling
                    else
                        SessionUpdateMode.Listener
                Timber.d("Session update mode set to ${sessionUpdateMode.name}")
            }
            context.getString(R.string.key_session_continuous_polling_rate) -> {
                pollingTimeout = sharedPreferences.getString(key, "3000")?.toLongOrNull() ?: 3000L
                continuousPollingJob?.cancel()
                updatePollingMode()
                Timber.d("Session polling interval set to ${pollingTimeout}ms")
            }
        }
    }

    private fun installNotificationListenerService() {
        val hasPermission = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
        Timber.d("Notification listener permission granted? $hasPermission")
        try {
            if (hasPermission) {
                sessionManager.addOnActiveSessionsChangedListener(
                    this,
                    ComponentName(context, NotificationListenerService::class.java)
                )
            } else {
                sessionManager.removeOnActiveSessionsChangedListener(this)
            }
        }
        catch (ex: SecurityException) {
            Timber.e("installNotificationListenerService: SecurityException; missing permission")
        }
    }

    private fun updatePollingMode()
    {
        when (sessionUpdateMode) {
            SessionUpdateMode.ContinuousPolling -> {
                continuousPollingJob = pollingScope.launch {
                    while(continuousPollingJob != null && continuousPollingJob?.isCancelled == false)
                    {
                        pollSessionDump()
                        delay(pollingTimeout)
                    }
                }
            }
            SessionUpdateMode.Listener -> {
                continuousPollingJob?.cancel()
            }
        }
    }

    private suspend fun pollSessionDump(blocking: Boolean = true)
    {
        if(pollingMutex.isLocked && !blocking)
        {
            return
        }

        pollingMutex.withLock {
            val sessions = dumpManager.dumpSessions()
            if(sessions is ISessionPolicyInfoDump) {
                sessionPolicyDatabase.update(sessions)
            }
            else {
                dumpManager.dumpCaptureAllowlistLog()?.let { sessionPolicyDatabase.update(it) }
            }

            sessions?.let { sessionDatabase.update(it) }
        }
    }

    fun pollOnce(blocking: Boolean)
    {
        pollingScope.launch {
            pollSessionDump(blocking)
        }
    }

    override fun onDumpMethodChange(method: DumpManager.Method) {
        sessionDatabase.clearSessions()
        sessionPolicyDatabase.clearSessions()
        pollOnce(true)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action == Constants.ACTION_SESSION_CHANGED) {
            // Too unreliable, most players don't implement this
            val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)
            if (sessionId < 0)
                return

            Timber.d("Audio effect control session broadcast: $sessionId (${intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)})")
            pollOnce(false)
        }
    }

    @SuppressLint("BinaryOperationInTimber")
    override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
        controllers ?: return
        controllers.forEach {
            Timber.d("active session changed: package ${it.packageName}; " +
                    "uid ${Refine.unsafeCast<MediaSessionHidden.TokenHidden>(it.sessionToken).uid}; " +
                    "usage ${it.playbackInfo?.audioAttributes?.usage}")
        }

        pollOnce(false)
    }
}