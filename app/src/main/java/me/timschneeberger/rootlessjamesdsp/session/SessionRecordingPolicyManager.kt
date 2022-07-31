package me.timschneeberger.rootlessjamesdsp.session

import android.content.Context
import android.content.pm.PackageManager
import me.timschneeberger.rootlessjamesdsp.model.SessionRecordingPolicyEntry
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionPolicyInfoDump
import timber.log.Timber


class SessionRecordingPolicyManager(private val context: Context) {

    private var isDisposing = false
    private val sessionPolicyList = hashMapOf<String,SessionRecordingPolicyEntry>()
    private val changeCallbacks = mutableListOf<OnSessionRecordingPolicyChangeListener>()

    fun destroy()
    {
        isDisposing = true
        clearSessions()
    }

    fun clearSessions(){
        Timber.d("Cleared session policy list")
        sessionPolicyList.clear()
    }

    fun update(dump: ISessionPolicyInfoDump)
    {
        if(isDisposing) {
            Timber.d("update: SessionRecordingPolicyManager is disposing; ignoring dump")
            return
        }

        val removedPolicies = sessionPolicyList.filter {
            !dump.capturePermissionLog.contains(it.key)
        }
        val addedPolicies = dump.capturePermissionLog.filter {
            !sessionPolicyList.contains(it.key) && it.key != context.packageName
        }
        val updatedPolicies = dump.capturePermissionLog.filter {
            sessionPolicyList.contains(it.key) && sessionPolicyList[it.key]?.isRestricted != /* !isAllowed */ !it.value
        }

        var isMinorUpdate = true
        removedPolicies.forEach {
            Timber.tag(TAG).d("Removed session policy: ${it.value}")
            sessionPolicyList.remove(it.key)
            if(it.value.isRestricted) {
                isMinorUpdate = false
            }
        }

        addedPolicies.forEach next@ {
            val isRestricted = /* !isAllowed */ !it.value
            addSessionPolicy(it.key, isRestricted)
            if(isRestricted) {
                isMinorUpdate = false
            }
        }

        updatedPolicies.forEach next@ {
            addSessionPolicy(it.key, /* !isAllowed */ !it.value)
            isMinorUpdate = false
        }

        if(addedPolicies.isNotEmpty() || removedPolicies.isNotEmpty() || updatedPolicies.isNotEmpty())
        {
            changeCallbacks.forEach { it.onSessionRecordingPolicyChanged(sessionPolicyList, isMinorUpdate) }
        }
    }

    private fun addSessionPolicy(packageName: String, isRestricted: Boolean){
        if (packageName == context.packageName) {
            Timber.tag(TAG).d("Skipped session policy for '$packageName'")
            return
        }

        val uid: Int = try {
            context.packageManager.getApplicationInfo(packageName, 0).uid
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e("addSessionPolicy: Package not found: $packageName")
            -1
        }
        val data = SessionRecordingPolicyEntry(uid, packageName, isRestricted)
        if(sessionPolicyList.containsKey(data.packageName))
            Timber.tag(TAG).d("Updated session policy: $data")
        else if(data.isRestricted) // Only log new restricted sessions
            Timber.tag(TAG).d("Added session policy: $data")
        sessionPolicyList[data.packageName] = data
    }

    fun getRestrictedUids(): Array<Int> {
        return sessionPolicyList
            .filter { it.value.isRestricted }
            .filter { it.value.uid > 0 }
            .map { it.value.uid }
            .toTypedArray()
    }

    fun registerOnRestrictedSessionChangeListener(changeListener: OnSessionRecordingPolicyChangeListener) {
        changeCallbacks.add(changeListener)
    }

    fun unregisterOnRestrictedSessionChangeListener(changeListener: OnSessionRecordingPolicyChangeListener) {
        changeCallbacks.remove(changeListener)
    }

    interface OnSessionRecordingPolicyChangeListener {
        fun onSessionRecordingPolicyChanged(sessionList: HashMap<String, SessionRecordingPolicyEntry>, isMinorUpdate: Boolean)
    }

    companion object {
        const val TAG = "SessionRecordingPolicyManager"
    }
}