package me.timschneeberger.rootlessjamesdsp.utils.extensions

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.getSystemService
import me.timschneeberger.rootlessjamesdsp.utils.extensions.CompatExtensions.getApplicationInfoCompat
import me.timschneeberger.rootlessjamesdsp.utils.sdkAbove
import timber.log.Timber

object PermissionExtensions {
    private const val APP_OP_PROJECT_MEDIA = "android:project_media"
    private const val APP_OP_PACKAGE_USAGE = "android:get_usage_stats"

    fun Context.hasProjectMediaAppOp() = hasAppOp(APP_OP_PROJECT_MEDIA)
    fun Context.hasPackageUsageAppOp() = hasAppOp(APP_OP_PACKAGE_USAGE)

    private fun Context.hasAppOp(appOp: String): Boolean {
        return try {
            val applicationInfo = packageManager.getApplicationInfoCompat(packageName, 0)
            val appOpsManager = getSystemService<AppOpsManager>()
            val mode = appOpsManager!!.unsafeCheckOpNoThrow(
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
    fun Context.hasPackageUsagePermission() = hasPermission(Manifest.permission.PACKAGE_USAGE_STATS) && hasPackageUsageAppOp()

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