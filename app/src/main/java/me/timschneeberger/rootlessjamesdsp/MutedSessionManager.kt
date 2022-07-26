package me.timschneeberger.rootlessjamesdsp

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaDrm
import android.media.audiofx.DynamicsProcessing
import android.os.Process.myUid
import me.timschneeberger.rootlessjamesdsp.dump.DumpManager
import me.timschneeberger.rootlessjamesdsp.dump.data.AudioPolicyServiceDump
import me.timschneeberger.rootlessjamesdsp.dump.data.ISessionInfoDump
import me.timschneeberger.rootlessjamesdsp.dump.utils.DumpUtils
import me.timschneeberger.rootlessjamesdsp.model.AudioSessionEntry
import me.timschneeberger.rootlessjamesdsp.model.MutedSessionEntry
import me.timschneeberger.rootlessjamesdsp.model.SessionUpdateMode
import me.timschneeberger.rootlessjamesdsp.service.AudioProcessorService
import timber.log.Timber
import java.lang.Exception


class MutedSessionManager(private val context: Context) {

    private var isDisposing = false
    private val sessionList = hashMapOf<Int,MutedSessionEntry>()
    private val changeCallbacks = mutableListOf<OnSessionChangeListener>()
    private var sessionLossListener: OnSessionLossListener? = null

    fun destroy()
    {
        isDisposing = true
        clearSessions()
    }

    fun clearSessions(){
        sessionList.forEach { (_, session) ->
            session.dynamicsProcessing?.enabled = false
            session.dynamicsProcessing?.release()
        }
        sessionList.clear()
    }

    fun update(dump: ISessionInfoDump)
    {
        if(isDisposing) {
            Timber.d("update: MutedSessionManager is disposing; ignoring dump")
            return
        }

        val removedSessions = sessionList.filter {
            !dump.sessions.contains(it.key)
        }
        val addedSessions = dump.sessions.filter {
            !sessionList.contains(it.key)
        }

        addedSessions.forEach next@ {
            val sid = it.key
            val data = it.value
            val name = context.packageManager.getNameForUid(it.value.uid)
            if (data.uid == myUid() || name == context.packageName) {
                Timber.tag(TAG).d("Skipped session $sid of uid $data ($name)")
                return@next
            }
            if (sid == 0) {
                Timber.tag(TAG).w("Session 0 skipped (owner uid $data; $name)");
                return@next
            }

            if (!AudioSessionEntry.isUsageRecordable(it.value.usage)) {
                Timber.tag(TAG).d("Skipped session $sid of uid $data ($name) due to usage '${it.value.usage}'")
                return@next
            }

            addSession(sid, data)
        }

        removedSessions.forEach {
            Timber.tag(TAG)
                .d("Removed session: session ${it.key}; uid: ${it.value.audioSession.uid}; package: ${it.value.audioSession.packageName}")
            it.value.dynamicsProcessing?.release()
            sessionList.remove(it.key)
        }

        if(addedSessions.isNotEmpty() || removedSessions.isNotEmpty())
        {
            changeCallbacks.forEach { it.onSessionChanged(sessionList) }
        }
    }

    private fun addSession(sid: Int, data: AudioSessionEntry){
        Timber.tag(TAG).d("Added session: session $sid; uid: ${data.uid}; package: ${data.packageName}")

        try {
            val muteEffect = DynamicsProcessing(sid)
            muteEffect.setInputGainAllChannelsTo(-200f)
            muteEffect.enabled = true
            muteEffect.setEnableStatusListener { effect, enabled ->
                if (!enabled) {
                    try {
                        (effect as DynamicsProcessing).setInputGainAllChannelsTo(-200f)
                        effect.enabled = true
                        Timber.tag(TAG)
                            .d("Dynamics processor control re-enabled (session $sid)")
                    }
                    catch(ex: Exception)
                    {
                        Timber.tag(TAG).w("Failed to re-enable processor")
                        Timber.tag(TAG).w(ex)
                    }
                }
            }
            muteEffect.setControlStatusListener { effect, controlGranted ->
                if(!controlGranted)
                {
                    sessionLossListener?.onSessionLost(sid)
                }
                Timber.tag(TAG)
                    .d(
                        "Dynamics processor control %s",
                        if (controlGranted) " returned" else "taken (session $sid)"
                    )
            }

            sessionList[sid] = MutedSessionEntry(data, muteEffect)
            Timber.tag(TAG).d("Successfully added session $sid")
        } catch (ex: RuntimeException) {
            Timber.tag(TAG)
                .e("Failed to attach DynamicsProcessing to session $sid (data: $data; message: ${ex.message})")
            if(data.usage.uppercase().contains("MEDIA") || data.usage.uppercase().contains("GAME") || data.usage.uppercase().contains("UNKNOWN"))
            {
                // todo inappropriate callback
                sessionLossListener?.onSessionLost(sid)
            }
        }
    }

    fun setOnSessionLossListener(_sessionLossListener: OnSessionLossListener) {
        sessionLossListener = _sessionLossListener
    }

    fun registerOnSessionChangeListener(changeListener: OnSessionChangeListener) {
        changeCallbacks.add(changeListener)
        changeListener.onSessionChanged(sessionList)
    }

    fun unregisterOnSessionChangeListener(changeListener: OnSessionChangeListener) {
        changeCallbacks.remove(changeListener)
    }


    interface OnSessionChangeListener {
        fun onSessionChanged(sessionList: HashMap<Int,MutedSessionEntry>)
    }

    interface OnSessionLossListener {
        fun onSessionLost(sid: Int)
    }

    companion object {
        const val TAG = "MutedSessionManager"
    }
}