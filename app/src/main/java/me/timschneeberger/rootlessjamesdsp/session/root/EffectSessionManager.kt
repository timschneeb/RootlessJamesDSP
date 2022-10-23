package me.timschneeberger.rootlessjamesdsp.session.root

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Process.myUid
import android.util.Log
import android.util.SparseArray
import androidx.core.util.containsKey
import androidx.core.util.forEach
import androidx.core.util.valueIterator
import kotlinx.coroutines.Job
import me.timschneeberger.rootlessjamesdsp.MainApplication
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspRemoteEngine
import me.timschneeberger.rootlessjamesdsp.interop.ProcessorMessageHandler
import me.timschneeberger.rootlessjamesdsp.model.root.EffectSessionEntry
import me.timschneeberger.rootlessjamesdsp.model.rootless.AudioSessionEntry
import me.timschneeberger.rootlessjamesdsp.model.rootless.MutedSessionEntry
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionInfoDump
import me.timschneeberger.rootlessjamesdsp.utils.AudioEffectFactory
import timber.log.Timber

class EffectSessionManager(private val context: Context) {

    private val app
        get() = context.applicationContext as MainApplication

    var enabled: Boolean = true
        set(value) {
            field = value
            app.rootSessionManager.sessionList.forEach { _, session ->
                session.effect?.enabled = value
            }
        }

    val sessionList = SparseArray<EffectSessionEntry>()
    private val changeCallbacks = mutableListOf<OnSessionChangeListener>()

    fun clearSessions(){
        sessionList.forEach { sessionId, _ ->
            removeSessionById(sessionId)
        }
        sessionList.clear()
    }

    fun addSession(intent: Intent) {
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)
        if (sessionId < 0 || (app.isLegacyMode && sessionId > 0)) {
            return
        }

        // Legacy mode enabled, remove other sessions
        if (sessionId == 0)
            clearSessions()

        addSessionById(sessionId, intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME))
    }

    private fun addSessionById(sessionId: Int, pkg: String?) {
        if (sessionList[sessionId] == null) {
            Timber.d("Adding session $sessionId ($pkg)")

            val effect = try {
                JamesDspRemoteEngine(context, sessionId, 0, ProcessorMessageHandler())
            }
            catch (ex: IllegalStateException) {
                Timber.e("Failed to instantiate JamesDSP effect")
                Timber.e(ex.cause)
                return
            }

            effect.enabled = enabled

            val entry = EffectSessionEntry(
                sessionId, pkg, effect
            )

            // Remove existing entries for package
            if (sessionList.valueIterator().asSequence().count { it.packageName == pkg } > 1) {
                sessionList.valueIterator().asSequence().first { it.packageName == pkg }.let {
                    it.effect?.close()
                    sessionList.remove(sessionList.keyAt(sessionList.indexOfValue(it)))
                }
            }

            sessionList.put(sessionId, entry)
            changeCallbacks.forEach { it.onSessionChanged(sessionList) }
        } else {
            Timber.w("Session $sessionId ($pkg) already existed")
        }
    }

    fun removeSession(intent: Intent) {
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)
        if (app.isLegacyMode) {
            return
        }

        removeSessionById(sessionId)
    }

    private fun removeSessionById(sessionId: Int) {
        if (sessionList[sessionId] != null) {
            Timber.d("Removing session $sessionId")

            sessionList[sessionId].coroutineContext[Job]!!.invokeOnCompletion {
                sessionList[sessionId].effect?.close()
            }
            sessionList.get(sessionId)?.coroutineContext?.get(Job)?.cancel()
            sessionList.remove(sessionId)

            changeCallbacks.forEach { it.onSessionChanged(sessionList) }
        }
        else
            Timber.w("Failed to remove non-existant session $sessionId")
    }

    fun registerOnSessionChangeListener(changeListener: OnSessionChangeListener) {
        changeCallbacks.add(changeListener)
        changeListener.onSessionChanged(sessionList)
    }

    fun unregisterOnSessionChangeListener(changeListener: OnSessionChangeListener) {
        changeCallbacks.remove(changeListener)
    }

    interface OnSessionChangeListener {
        fun onSessionChanged(sessionList: SparseArray<EffectSessionEntry>)
    }
}