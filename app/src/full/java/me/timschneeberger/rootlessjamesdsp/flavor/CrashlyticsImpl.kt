package me.timschneeberger.rootlessjamesdsp.flavor

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

object CrashlyticsImpl {
    fun setCollectionEnabled(on: Boolean) = Firebase.crashlytics.setCrashlyticsCollectionEnabled(on)
    fun setCustomKey(key: String, value: String) = Firebase.crashlytics.setCustomKey(key, value)
    fun setCustomKey(key: String, value: Int) = Firebase.crashlytics.setCustomKey(key, value)
    fun log(msg: String) = Firebase.crashlytics.log(msg)
    fun recordException(t: Throwable) = Firebase.crashlytics.recordException(t)
    fun sendUnsentReports() = Firebase.crashlytics.sendUnsentReports()
}
