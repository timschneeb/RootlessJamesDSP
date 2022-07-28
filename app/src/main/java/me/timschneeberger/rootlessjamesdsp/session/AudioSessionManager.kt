package me.timschneeberger.rootlessjamesdsp.session

import android.annotation.SuppressLint
import android.content.*
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.audiofx.AudioEffect
import android.media.session.MediaController
import android.media.session.MediaSessionHidden
import android.media.session.MediaSessionManager
import android.os.*
import androidx.core.app.NotificationManagerCompat
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.session.dump.DumpManager
import me.timschneeberger.rootlessjamesdsp.model.SessionUpdateMode
import me.timschneeberger.rootlessjamesdsp.service.NotificationListenerService
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.SystemServices
import timber.log.Timber


class AudioSessionManager(val context: Context)
{
    private val audioManager = SystemServices.get(context, AudioManager::class.java)
    private val sessionManager = SystemServices.get(context, MediaSessionManager::class.java)

    var sessionUpdateMode: SessionUpdateMode = SessionUpdateMode.Listener
        set(value) {
            field = value
            updatePollingMode()
        }
    var pollingTimeout = 3000L

    private val pollingMutex = Mutex()
    private val pollingScope = CoroutineScope(Dispatchers.Main)
    private var continuousPollingJob: Job? = null
    var sessionDatabase: MutedSessionManager = MutedSessionManager(context)

    private var activeSessionsChangedListener: MediaSessionManager.OnActiveSessionsChangedListener
    private var audioPlaybackCallback: AudioManager.AudioPlaybackCallback
    private var sessionChangedReceiver: BroadcastReceiver

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)
    private val preferencesListener: SharedPreferences.OnSharedPreferenceChangeListener

    private val dumpMethodChangeListener = object : DumpManager.OnDumpMethodChangeListener {
        override fun onDumpMethodChange(method: DumpManager.Method) {
            sessionDatabase.clearSessions()
            pollOnce(true)
        }
    }

    init {

        sessionChangedReceiver = object : BroadcastReceiver() {
            // Too unreliable, most players don't implement this
            override fun onReceive(context: Context, intent: Intent) {
                val sessionId =
                    intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)
                if (sessionId < 0) {
                    return
                }

                Timber.tag(TAG).d(
                    "Audio effect control session broadcast: $sessionId (${intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)})"
                )
                pollOnce(false)
            }
        }

        audioPlaybackCallback = object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
                super.onPlaybackConfigChanged(configs)

                Timber.tag(TAG).d("Playback config changed")
                pollOnce(false)
            }
        }

        activeSessionsChangedListener = object : MediaSessionManager.OnActiveSessionsChangedListener {
            @SuppressLint("BinaryOperationInTimber")
            override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
                controllers ?: return
                controllers.forEach {
                    Timber.tag(TAG).d("active session changed: package ${it.packageName}; " +
                            "uid ${Refine.unsafeCast<MediaSessionHidden.TokenHidden>(it.sessionToken).uid}; " +
                            "usage ${it.playbackInfo?.audioAttributes?.usage}")
                }

                pollOnce(false)
            }
        }

        preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener {
                    _, key ->
            loadFromPreferences(key)
        }

        DumpManager.get(context).registerOnDumpMethodChangeListener(dumpMethodChangeListener)

        audioManager.registerAudioPlaybackCallback(audioPlaybackCallback, Handler(Looper.getMainLooper()))
        context.registerLocalReceiver(sessionChangedReceiver, IntentFilter(Constants.ACTION_SESSION_CHANGED))
        updateSessionListenerState()

        loadFromPreferences(context.getString(R.string.key_session_continuous_polling))
        loadFromPreferences(context.getString(R.string.key_session_continuous_polling_rate))
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesListener)
    }

    fun destroy()
    {
        DumpManager.get(context).unregisterOnDumpMethodChangeListener(dumpMethodChangeListener)

        audioManager.unregisterAudioPlaybackCallback(audioPlaybackCallback)
        sessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
        context.unregisterLocalReceiver(sessionChangedReceiver)

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferencesListener)

        sessionDatabase.destroy()
    }

    private fun loadFromPreferences(key: String){
        when (key) {
            context.getString(R.string.key_session_continuous_polling) -> {
                sessionUpdateMode =
                    if (sharedPreferences.getBoolean(key, false))
                        SessionUpdateMode.ContinuousPolling
                    else
                        SessionUpdateMode.Listener
                Timber.tag(TAG).d("Session update mode set to ${sessionUpdateMode.name}")
            }
            context.getString(R.string.key_session_continuous_polling_rate) -> {
                pollingTimeout = sharedPreferences.getString(key, "3000")?.toLongOrNull() ?: 3000L
                continuousPollingJob?.cancel()
                updatePollingMode()
                Timber.tag(TAG).d("Session polling interval set to ${pollingTimeout}ms")
            }
        }
    }

    private fun updateSessionListenerState() {
        val hasPermission = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
        Timber.tag(TAG).d("Notification listener permission granted? $hasPermission")
        if (hasPermission) {
            sessionManager.addOnActiveSessionsChangedListener(
                activeSessionsChangedListener,
                ComponentName(context, NotificationListenerService::class.java)
            )
        } else {
            sessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
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
            DumpManager.get(context).dumpSessions()?.let { sessionDatabase.update(it) }
        }
    }

    private fun pollOnce(blocking: Boolean)
    {
        pollingScope.launch {
            pollSessionDump(blocking)
        }
    }

    companion object
    {
        const val TAG = "AudioSessionManager"
    }
}