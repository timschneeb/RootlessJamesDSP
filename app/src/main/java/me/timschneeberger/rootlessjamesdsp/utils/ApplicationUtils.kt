package me.timschneeberger.rootlessjamesdsp.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.service.NotificationListenerService
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.getAppName


object ApplicationUtils {
    private const val PKGNAME_REF = "bWUudGltc2NobmVlYmVyZ2VyLnJvb3RsZXNzamFtZXNkc3A="
    private const val APPNAME_REF = "Um9vdGxlc3NKYW1lc0RTUA=="

    // Very simple & naive app cloner checks; please don't use multiple instances at once
    fun check(ctx: Context): Int {
        if(decode(PKGNAME_REF) != ctx.packageName) return 1
        if(decode(APPNAME_REF) != ctx.getText(R.string.app_name)) return 2
        if(decode(APPNAME_REF) != ctx.getAppName()) return 3
        return 0
    }

    private fun decode(input: String): String {
        return String(Base64.decode(input, 0), Charsets.UTF_8)
    }

    fun describe(ctx: Context): String {
        return "package=${ctx.packageName}; app_name=${ctx.getString(R.string.app_name)}; label=${ctx.getAppName()}"
    }

    fun getIntentForNotificationAccess(packageName: String, notificationAccessServiceClass: Class<out NotificationListenerService>): Intent =
        getIntentForNotificationAccess(packageName, notificationAccessServiceClass.name)

    private fun getIntentForNotificationAccess(packageName: String, notificationAccessServiceClassName: String): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, ComponentName(packageName, notificationAccessServiceClassName).flattenToString())
        }
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        val value = "$packageName/$notificationAccessServiceClassName"
        val key = ":settings:fragment_args_key"
        intent.putExtra(key, value)
        intent.putExtra(":settings:show_fragment_args", Bundle().also { it.putString(key, value) })
        return intent
    }
}