package me.timschneeberger.rootlessjamesdsp.utils.extensions

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import me.timschneeberger.rootlessjamesdsp.utils.sdkAbove
import java.io.Serializable

@Suppress("DEPRECATION")
object CompatExtensions {
    inline fun <reified T : Serializable> Bundle.getSerializableAs(key: String): T? {
        return sdkAbove(Build.VERSION_CODES.TIRAMISU) {
            this.getSerializable(key, T::class.java)
        }.below {
            this.getSerializable(key) as? T
        }
    }

    inline fun <reified T : Parcelable> Bundle.getParcelableAs(key: String): T? {
        return sdkAbove(Build.VERSION_CODES.TIRAMISU) {
            this.getParcelable(key, T::class.java)
        }.below {
            this.getParcelable(key)
        }
    }

    fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo {
        return sdkAbove(Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        }.below {
            getPackageInfo(packageName, flags)
        }
    }

    fun PackageManager.getApplicationInfoCompat(packageName: String, flags: Int = 0): ApplicationInfo {
        return sdkAbove(Build.VERSION_CODES.TIRAMISU) {
            getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        }.below {
            getApplicationInfo(packageName, flags)
        }
    }

    fun PackageManager.getInstalledApplicationsCompat(flags: Int = 0): List<ApplicationInfo> {
        return sdkAbove(Build.VERSION_CODES.TIRAMISU) {
            getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        }.below {
            getInstalledApplications(flags)
        }
    }
}