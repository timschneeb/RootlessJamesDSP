package me.timschneeberger.rootlessjamesdsp.utils

import android.annotation.SuppressLint
import android.os.Build
import com.google.android.material.color.DynamicColors
import timber.log.Timber

object DeviceUtil {
    private val isSamsung by lazy {
        Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }

    @SuppressLint("PrivateApi")
    private fun getSystemProperty(key: String?): String? {
        return try {
            Class.forName("android.os.SystemProperties")
                .getDeclaredMethod("get", String::class.java)
                .invoke(null, key) as String
        } catch (e: Exception) {
            Timber.w("Unable to use SystemProperties.get()")
            Timber.d(e)
            null
        }
    }


    val isDynamicColorAvailable by lazy {
        DynamicColors.isDynamicColorAvailable() ||
                (isSamsung && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    }
}