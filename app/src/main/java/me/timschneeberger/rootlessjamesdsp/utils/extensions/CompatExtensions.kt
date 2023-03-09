package me.timschneeberger.rootlessjamesdsp.utils.extensions

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

object CompatExtensions {
    @Suppress("UNCHECKED_CAST")
    fun <T : Serializable> Bundle.getSerializableAs(key: String, clazz: Class<T>): T? {
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getSerializable(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            this.getSerializable(key)
        }) as? T
    }

    inline fun <reified T : Parcelable> Bundle.getParcelableAs(key: String): T? {
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getParcelable(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            this.getParcelable(key)
        })
    }

    fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION") getPackageInfo(packageName, flags)
        }
    }

    fun PackageManager.getApplicationInfoCompat(packageName: String, flags: Int = 0): ApplicationInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION") getApplicationInfo(packageName, flags)
        }
    }

    fun PackageManager.getInstalledApplicationsCompat(flags: Int = 0): List<ApplicationInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION") getInstalledApplications(flags)
        }
    }
}