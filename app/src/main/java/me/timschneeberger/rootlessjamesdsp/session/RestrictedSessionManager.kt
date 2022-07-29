package me.timschneeberger.rootlessjamesdsp.session

import android.content.Context
import android.content.pm.PackageManager
import me.timschneeberger.rootlessjamesdsp.model.RestrictedSessionEntry
import me.timschneeberger.rootlessjamesdsp.session.dump.data.IRestrictedSessionInfoDump
import timber.log.Timber


class RestrictedSessionManager(private val context: Context) {

    private var isDisposing = false
    private val sessionPolicyList = hashMapOf<String,RestrictedSessionEntry>()
    private val changeCallbacks = mutableListOf<OnRestrictedSessionChangeListener>()

    fun destroy()
    {
        isDisposing = true
        clearSessions()
    }

    fun clearSessions(){
        Timber.d("cleared session policy list")
        sessionPolicyList.clear()
    }

    fun update(dump: IRestrictedSessionInfoDump)
    {
        if(isDisposing) {
            Timber.d("update: RestrictedSessionManager is disposing; ignoring dump")
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
            changeCallbacks.forEach { it.onRestrictedSessionChanged(sessionPolicyList, isMinorUpdate) }
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
        val data = RestrictedSessionEntry(uid, packageName, isRestricted)
        if(sessionPolicyList.containsKey(data.packageName))
            Timber.tag(TAG).d("Updated session policy: $data")
        else if(data.isRestricted) // Only log new restricted sessions
            Timber.tag(TAG).d("Added session policy: $data")
        sessionPolicyList[data.packageName] = data
    }

    fun getRestrictedUids(): Array<Int> {
        return sessionPolicyList
            .filter { it.value.isRestricted }
            .map { it.value.uid }
            .toTypedArray()
    }

    fun registerOnRestrictedSessionChangeListener(changeListener: OnRestrictedSessionChangeListener) {
        changeCallbacks.add(changeListener)
        // TODO clients should get the initial state by themselves
        //changeListener.onRestrictedSessionChanged(sessionList, )
    }

    fun unregisterOnRestrictedSessionChangeListener(changeListener: OnRestrictedSessionChangeListener) {
        changeCallbacks.remove(changeListener)
    }

    interface OnRestrictedSessionChangeListener {
        fun onRestrictedSessionChanged(sessionList: HashMap<String, RestrictedSessionEntry>, isMinorUpdate: Boolean)
    }

    companion object {
        const val TAG = "RestrictedSessionManager"
    }
}