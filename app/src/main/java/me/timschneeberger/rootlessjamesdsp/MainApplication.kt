package me.timschneeberger.rootlessjamesdsp

import android.app.Application
import android.os.Build
import android.util.Log
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import me.timschneeberger.rootlessjamesdsp.model.room.AppBlocklistDatabase
import me.timschneeberger.rootlessjamesdsp.model.room.AppBlocklistRepository
import org.lsposed.hiddenapibypass.HiddenApiBypass
import timber.log.Timber

import timber.log.Timber.*

class MainApplication : Application() {
    init {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        {
            HiddenApiBypass.addHiddenApiExemptions("L")
        }
    }

    val applicationScope = CoroutineScope(SupervisorJob())
    val blockedAppDatabase by lazy { AppBlocklistDatabase.getDatabase(this, applicationScope) }
    val blockedAppRepository by lazy { AppBlocklistRepository(blockedAppDatabase.appBlocklistDao()) }

    override fun onCreate() {
        super.onCreate()

        Timber.plant(DebugTree())
        Timber.plant(CrashReportingTree())
    }

    /** A tree which logs important information for crash reporting.  */
    private class CrashReportingTree : Tree() {
        private fun priorityAsString(priority: Int): String {
            return when(priority){
                Log.VERBOSE -> "V"
                Log.DEBUG -> "D"
                Log.INFO -> "I"
                Log.WARN -> "W"
                Log.ERROR -> "E"
                Log.ASSERT -> "A"
                else -> "?"
            }
        }

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val safeTag = tag ?: "Unknown"
            Firebase.crashlytics.log("[${priorityAsString(priority)}] $safeTag: $message")

            if (t != null && (priority == Log.ERROR || priority == Log.WARN || priority == Log.ASSERT)) {
                Firebase.crashlytics.recordException(t)
            }
        }
    }
}