package me.timschneeberger.rootlessjamesdsp.session

import android.content.Context
import android.media.audiofx.DynamicsProcessing
import android.os.Process.myUid
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionInfoDump
import me.timschneeberger.rootlessjamesdsp.model.AudioSessionEntry
import me.timschneeberger.rootlessjamesdsp.model.MutedSessionEntry
import timber.log.Timber
import java.lang.Exception


class MutedSessionManager(private val context: Context) {

    private var isDisposing = false
    private val sessionList = hashMapOf<Int,MutedSessionEntry>()
    private val changeCallbacks = mutableListOf<OnSessionChangeListener>()
    private var sessionLossListener: OnSessionLossListener? = null
    private var excludedUids = arrayOf<Int>()

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
            !sessionList.contains(it.key) && !excludedUids.contains(it.value.uid)
        }

        addedSessions.forEach next@ {
            val sid = it.key
            val data = it.value
            val name = context.packageManager.getNameForUid(it.value.uid)
            if (data.uid == myUid() || name == context.packageName) {
                Timber.tag(TAG).d("Skipped session $sid ($data)")
                return@next
            }
            if (sid == 0) {
                Timber.tag(TAG).w("Session 0 skipped ($data)")
                return@next
            }

            if (!AudioSessionEntry.isUsageRecordable(it.value.usage)) {
                Timber.tag(TAG).d("Skipped session $sid due to usage ($data)")
                return@next
            }

            addSession(sid, data)
        }

        removedSessions.forEach {
            Timber.tag(TAG)
                .d("Removed session: session ${it.key}; data: ${it.value.audioSession}")
            it.value.dynamicsProcessing?.release()
            sessionList.remove(it.key)
        }

        if(addedSessions.isNotEmpty() || removedSessions.isNotEmpty())
        {
            changeCallbacks.forEach { it.onSessionChanged(sessionList) }
        }
    }

    private fun addSession(sid: Int, data: AudioSessionEntry){
        if(excludedUids.contains(data.uid)) {
            Timber.tag(TAG).d("Rejected session $sid from excluded uid ${data.uid} ($data)")
            return
        }

        Timber.tag(TAG).d("Added session: sid=$sid; $data")

        try {
            val muteEffect = DynamicsProcessing(Int.MAX_VALUE, sid, null)
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
                        sessionLossListener?.onSessionLost(sid)
                    }
                }
            }
            muteEffect.setControlStatusListener { effect, controlGranted ->
                if(!controlGranted)
                {
                    sessionLossListener?.onSessionLost(sid)
                }
                else {
                    try {
                        (effect as DynamicsProcessing).setInputGainAllChannelsTo(-200f)
                        effect.enabled = true
                        Timber.tag(TAG)
                            .d("Dynamics processor re-muted (session $sid)")
                    }
                    catch(ex: Exception)
                    {
                        Timber.tag(TAG).w("Failed to re-mute session")
                        Timber.tag(TAG).w(ex)
                        sessionLossListener?.onSessionLost(sid)
                    }
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
                // TODO callback not appropriate -> attach fail != session loss
                sessionLossListener?.onSessionLost(sid)
            }
        }
    }

    fun setExcludedUids(uids: Array<Int>) {
        excludedUids = uids

        val excludedSessions = sessionList.filter { excludedUids.contains(it.value.audioSession.uid) }
        excludedSessions.forEach { (_, session) ->
            session.dynamicsProcessing?.enabled = false
            session.dynamicsProcessing?.release()
        }
        excludedSessions.map { it.key }.forEach { sid ->
            sessionList.remove(sid)
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