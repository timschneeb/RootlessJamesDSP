package me.timschneeberger.rootlessjamesdsp.session.shared

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.audiofx.AudioEffect
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import androidx.annotation.CallSuper
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.model.preference.SessionUpdateMode
import me.timschneeberger.rootlessjamesdsp.service.NotificationListenerService
import me.timschneeberger.rootlessjamesdsp.session.dump.DumpManager
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionInfoDump
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber


abstract class BaseSessionManager(protected val context: Context) : DumpManager.OnDumpMethodChangeListener,
    BroadcastReceiver(), MediaSessionManager.OnActiveSessionsChangedListener, KoinComponent
{
    // System services
    private val audioManager = context.getSystemService<AudioManager>()!!
    private val sessionManager = context.getSystemService<MediaSessionManager>()!!

    // Session dump manager
    protected val dumpManager: DumpManager by inject()

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
    private var audioPlaybackCallback: AudioManager.AudioPlaybackCallback? = null

    // Preferences
    private val preferencesListener: SharedPreferences.OnSharedPreferenceChangeListener
    private val preferences: Preferences.App by inject()

    protected abstract fun handleSessionDump(sessionDump: ISessionInfoDump?)

    init {
        Timber.d("Initializing SessionDumpManager")

        // Notify on playback changes
        audioPlaybackCallback = object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
                super.onPlaybackConfigChanged(configs)

                Timber.d("Playback config changed")
                pollOnce(false)
            }
        }
        audioManager.registerAudioPlaybackCallback(audioPlaybackCallback!!, Handler(Looper.getMainLooper()))


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
        preferences.registerOnSharedPreferenceChangeListener(preferencesListener)
    }

    @CallSuper
    open fun destroy()
    {
        Timber.d("Destroying SessionDumpManager")

        dumpManager.unregisterOnDumpMethodChangeListener(this)

        audioPlaybackCallback?.let { audioManager.unregisterAudioPlaybackCallback(it) }

        sessionManager.removeOnActiveSessionsChangedListener(this)
        context.unregisterLocalReceiver(this)
    }

    private fun loadFromPreferences(key: String?){
        when (key) {
            context.getString(R.string.key_session_continuous_polling) -> {
                sessionUpdateMode =
                    if (preferences.get<Boolean>(R.string.key_session_continuous_polling))
                        SessionUpdateMode.ContinuousPolling
                    else
                        SessionUpdateMode.Listener
                Timber.d("Session update mode set to ${sessionUpdateMode.name}")
            }
            context.getString(R.string.key_session_continuous_polling_rate) -> {
                pollingTimeout = R.string.key_session_continuous_polling_rate.let {
                    preferences.get<String>(it).toLongOrNull()
                        ?: preferences.getDefault<String>(it).toLong()
                }

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
            handleSessionDump(dumpManager.dumpSessions())
        }
    }

    fun pollOnce(blocking: Boolean)
    {
        pollingScope.launch {
            pollSessionDump(blocking)
        }
    }

    @CallSuper
    override fun onDumpMethodChange(method: DumpManager.Method) {
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
                    // Refine is not working with nested classes anymore
                    // "uid ${Refine.unsafeCast<MediaSessionHidden.TokenHidden>(it.sessionToken).uid}; " +
                    "usage ${it.playbackInfo?.audioAttributes?.usage}")
        }

        pollOnce(false)
    }
}