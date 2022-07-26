package me.timschneeberger.rootlessjamesdsp

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.os.BuildCompat
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.lsposed.hiddenapibypass.HiddenApiBypass
import timber.log.Timber

import timber.log.Timber.*

class MainApplication : Application() {
    init {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        {
            // TODO is this still necessary?
            HiddenApiBypass.setHiddenApiExemptions("")
        }
    }

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