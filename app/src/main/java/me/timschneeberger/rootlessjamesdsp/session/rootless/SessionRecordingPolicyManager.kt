package me.timschneeberger.rootlessjamesdsp.session.rootless

import android.content.Context
import android.content.pm.PackageManager
import me.timschneeberger.rootlessjamesdsp.model.rootless.SessionRecordingPolicyEntry
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionPolicyInfoDump
import me.timschneeberger.rootlessjamesdsp.utils.extensions.CompatExtensions.getApplicationInfoCompat
import timber.log.Timber


class SessionRecordingPolicyManager(private val context: Context) {

    private var isDisposing = false
    private val sessionPolicyList = hashMapOf<String, SessionRecordingPolicyEntry>()
    private val changeCallbacks = mutableListOf<OnSessionRecordingPolicyChangeListener>()

    fun destroy()
    {
        isDisposing = true
        clearSessions()
    }

    fun clearSessions(){
        Timber.d("Cleared session policy list")
        synchronized(sessionPolicyList) {
            sessionPolicyList.clear()
        }
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
        synchronized(sessionPolicyList) {
            removedPolicies.forEach {
                Timber.d("Removed session policy: ${it.value}")
                sessionPolicyList.remove(it.key)
                if (it.value.isRestricted) {
                    isMinorUpdate = false
                }
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
            Timber.d("Skipped session policy for '$packageName'")
            return
        }

        val uid: Int = try {
            context.packageManager.getApplicationInfoCompat(packageName, 0).uid
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e("addSessionPolicy: Package not found: $packageName")
            -1
        }
        val data = SessionRecordingPolicyEntry(uid, packageName, isRestricted)
        if(sessionPolicyList.containsKey(data.packageName))
            Timber.d("Updated session policy: $data")
        else if(data.isRestricted) // Only log new restricted sessions
            Timber.d("Added session policy: $data")
        synchronized(sessionPolicyList) {
            sessionPolicyList[data.packageName] = data
        }
    }

    fun getRestrictedUids(): Array<Int> {
        return synchronized(sessionPolicyList) {
            sessionPolicyList.values // create copy to prevent concurrent access
                .filter { it.isRestricted }
                .filter { it.uid > 0 }
                .map { it.uid }
                .toTypedArray()
        }
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
}