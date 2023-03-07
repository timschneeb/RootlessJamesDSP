package me.timschneeberger.rootlessjamesdsp.utils

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import timber.log.Timber

object PermissionExtensions {
    private const val APP_OP_PROJECT_MEDIA = "android:project_media"

    fun Context.hasProjectMediaAppOp() = hasAppOp(APP_OP_PROJECT_MEDIA)
    private fun Context.hasAppOp(appOp: String): Boolean {
        return try {
            val applicationInfo = packageManager.getApplicationInfoCompat(packageName, 0)
            val appOpsManager = SystemServices.get<AppOpsManager>(this)
            val mode = appOpsManager.unsafeCheckOpNoThrow(
                appOp,
                applicationInfo.uid,
                applicationInfo.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    fun Context.hasRecordPermission() = hasPermission(Manifest.permission.RECORD_AUDIO)
    fun Context.hasDumpPermission() = hasPermission(Manifest.permission.DUMP)
    fun Context.hasNotificationPermission(): Boolean {
        sdkAbove(Build.VERSION_CODES.TIRAMISU) {
            return hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        }
        return false
    }
    private fun Context.hasPermission(permission: String): Boolean {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}