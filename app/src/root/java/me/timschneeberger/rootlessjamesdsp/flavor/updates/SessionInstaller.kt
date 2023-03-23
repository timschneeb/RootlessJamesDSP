package me.timschneeberger.rootlessjamesdsp.flavor.updates

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import me.timschneeberger.rootlessjamesdsp.utils.SdkCheck
import me.timschneeberger.rootlessjamesdsp.utils.sdkAbove
import me.timschneeberger.rootlessjamesdsp.utils.storage.Cache
import kotlin.coroutines.resume

/** Based on https://github.com/Iamlooker/Droid-ify/ licensed under GPLv3 */
class SessionInstaller(private val context: Context) {

    private val sessionInstaller = context.packageManager.packageInstaller
    private val intent = Intent(context, SessionInstallerService::class.java)

    companion object {
        private var installerCallbacks = mutableListOf<PackageInstaller.SessionCallback>()
        private val flags = if (SdkCheck.isSnowCake) PendingIntent.FLAG_MUTABLE else 0
        private val sessionParams =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                sdkAbove(sdk = Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }
    }

    suspend fun performInstall(
        installItem: String
    ): Boolean = suspendCancellableCoroutine { cont ->

        val cacheFile = Cache.getReleaseFile(context, installItem)
        val id = sessionInstaller.createSession(sessionParams)
        val installerCallback = object : PackageInstaller.SessionCallback() {
            override fun onCreated(sessionId: Int) {}
            override fun onBadgingChanged(sessionId: Int) {}
            override fun onActiveChanged(sessionId: Int, active: Boolean) {}
            override fun onProgressChanged(sessionId: Int, progress: Float) {}
            override fun onFinished(sessionId: Int, success: Boolean) {
                if (sessionId == id) cont.resume(true)
            }
        }
        installerCallbacks.add(installerCallback)

        sessionInstaller.registerSessionCallback(
            installerCallbacks.last(),
            Handler(Looper.getMainLooper())
        )

        sessionInstaller.openSession(id).use { activeSession ->
            val sizeBytes = cacheFile.length()
            cacheFile.inputStream().use { fileStream ->
                activeSession.openWrite(cacheFile.name, 0, sizeBytes).use { outputStream ->
                    if (cont.isActive) {
                        fileStream.copyTo(outputStream)
                        activeSession.fsync(outputStream)
                    }
                }
            }

            val pendingIntent = PendingIntent.getService(context, id, intent, flags)

            if (cont.isActive) activeSession.commit(pendingIntent.intentSender)
        }
        cont.invokeOnCancellation {
            sessionInstaller.abandonSession(id)
        }
    }

    fun cleanup() {
        installerCallbacks.forEach { sessionInstaller.unregisterSessionCallback(it) }
        sessionInstaller.mySessions.forEach { sessionInstaller.abandonSession(it.sessionId) }
    }
}