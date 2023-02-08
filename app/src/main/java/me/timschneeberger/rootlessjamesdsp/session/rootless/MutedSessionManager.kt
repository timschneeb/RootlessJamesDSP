package me.timschneeberger.rootlessjamesdsp.session.rootless

import android.content.Context
import android.content.Intent
import android.os.Process.myUid
import me.timschneeberger.rootlessjamesdsp.model.rootless.AudioSessionEntry
import me.timschneeberger.rootlessjamesdsp.model.rootless.MutedSessionEntry
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionInfoDump
import me.timschneeberger.rootlessjamesdsp.utils.AudioEffectFactory
import timber.log.Timber

class MutedSessionManager(private val context: Context) {

    private var isDisposing = false
    private val factory by lazy { AudioEffectFactory() }
    private val sessionList = hashMapOf<Int, MutedSessionEntry>()
    private val changeCallbacks = mutableListOf<OnSessionChangeListener>()
    private var sessionLossListener: OnSessionLossListener? = null
    private var appProblemListener: OnAppProblemListener? = null

    private var excludedUids = arrayOf<Int>()
    private val excludedPackages = arrayOf(
        context.packageName,

        // Non-music apps known to cause issues
        "com.google.android.googlequicksearchbox",
        "com.google.android.as",
        "com.kieronquinn.app.pixelambientmusic",
        "com.draftkings.sportsbook",
        "com.samsung.gpuwatchapp"
    )

    init {
        factory.sessionLossListener = { sid, data ->
            sendFxCloseBroadcast(data.packageName, sid)
            sessionLossListener?.onSessionLost(sid)
        }
    }

    fun destroy()
    {
        isDisposing = true
        clearSessions()
    }

    fun clearSessions(){
        sessionList.forEach { (_, session) ->
            session.audioMuteEffect?.enabled = false
            session.audioMuteEffect?.release()
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
            if (data.uid == myUid() || excludedPackages.contains(name)) {
                Timber.d("Skipped session $sid due to package name $name ($data)")
                return@next
            }
            if (sid == 0) {
                Timber.w("Session 0 skipped ($data)")
                return@next
            }

            if (!AudioSessionEntry.isUsageRecordable(it.value.usage)) {
                Timber.d("Skipped session $sid due to usage ($data)")
                return@next
            }

            addSession(sid, data)
        }

        removedSessions.forEach {
            Timber.d("Removed session: session ${it.key}; data: ${it.value.audioSession}")
            it.value.audioMuteEffect?.release()
            sessionList.remove(it.key)
        }

        if(addedSessions.isNotEmpty() || removedSessions.isNotEmpty())
        {
            changeCallbacks.forEach { it.onSessionChanged(sessionList) }
        }
    }

    private fun addSession(sid: Int, data: AudioSessionEntry){
        if(excludedUids.contains(data.uid)) {
            Timber.d("Rejected session $sid from excluded uid ${data.uid} ($data)")
            return
        }

        Timber.d("Added session: sid=$sid; $data")

        val muteEffect = factory.make(sid, data)
        if(muteEffect == null && data.isUsageRecordable())
        {
            // App did not allow to attach effect; possibly caused by HW-acceleration
            appProblemListener?.onAppProblemDetected(data)
            return
        }

        muteEffect ?: return

        sessionList[sid] = MutedSessionEntry(data, muteEffect)
        Timber.d("Successfully added session $sid")
    }

    private fun sendFxCloseBroadcast(pkgName: String, sid: Int) {
        val intent = Intent("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION")
        intent.putExtra("android.media.extra.PACKAGE_NAME", pkgName)
        intent.putExtra("android.media.extra.AUDIO_SESSION", sid)
        intent.putExtra(EXTRA_IGNORE, 1)
        context.sendBroadcast(intent)

        Timber.d("Sent control session close request for $pkgName")
    }

    fun setExcludedUids(uids: Array<Int>) {
        excludedUids = uids

        val excludedSessions = sessionList.filter { excludedUids.contains(it.value.audioSession.uid) }
        excludedSessions.forEach { (_, session) ->
            session.audioMuteEffect?.enabled = false
            session.audioMuteEffect?.release()
        }
        excludedSessions.map { it.key }.forEach { sid ->
            sessionList.remove(sid)
        }
    }

    fun setOnSessionLossListener(_sessionLossListener: OnSessionLossListener) {
        sessionLossListener = _sessionLossListener
    }

    fun setOnAppProblemListener(_appProblemListener: OnAppProblemListener) {
        appProblemListener = _appProblemListener
    }

    fun registerOnSessionChangeListener(changeListener: OnSessionChangeListener) {
        changeCallbacks.add(changeListener)
        changeListener.onSessionChanged(sessionList)
    }

    fun unregisterOnSessionChangeListener(changeListener: OnSessionChangeListener) {
        changeCallbacks.remove(changeListener)
    }

    interface OnSessionChangeListener {
        fun onSessionChanged(sessionList: HashMap<Int, MutedSessionEntry>)
    }

    interface OnSessionLossListener {
        fun onSessionLost(sid: Int)
    }

    interface OnAppProblemListener {
        fun onAppProblemDetected(data: AudioSessionEntry)
    }

    companion object {
        const val EXTRA_IGNORE = "rootlessjamesdsp.ignore"
    }
}