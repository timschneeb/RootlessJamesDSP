package me.timschneeberger.rootlessjamesdsp.utils.extensions

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

object CompatExtensions {
    inline fun <reified T : Serializable> Bundle.getSerializableAs(key: String): T? {
        return this.getSerializable(key, T::class.java)
    }

    inline fun <reified T : Parcelable> Bundle.getParcelableAs(key: String): T? {
        return this.getParcelable(key, T::class.java)
    }

    fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo {
        return getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    }

    fun PackageManager.getApplicationInfoCompat(packageName: String, flags: Int = 0): ApplicationInfo {
        return getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
    }

    fun PackageManager.getInstalledApplicationsCompat(flags: Int = 0): List<ApplicationInfo> {
        return getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
    }
}
