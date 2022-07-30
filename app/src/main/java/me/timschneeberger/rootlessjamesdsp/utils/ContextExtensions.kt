package me.timschneeberger.rootlessjamesdsp.utils

import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.DialogTextinputBinding
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.getAppName
import timber.log.Timber
import java.io.File


object ContextExtensions {
    fun Context.openPlayStoreApp(pkgName:String?){
        if(!pkgName.isNullOrEmpty()) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkgName")))
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$pkgName")
                    )
                )
            }
        }
    }

    fun Context.isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** Open another app.
     * @param context current Context, like Activity, App, or Service
     * @param packageName the full package name of the app to open
     * @return true if likely successful, false if unsuccessful
     */
    fun Context.launchApp(packageName: String?): Boolean {
        val manager = this.packageManager
        return try {
            val i = manager.getLaunchIntentForPackage(packageName!!)
                ?: return false
            i.addCategory(Intent.CATEGORY_LAUNCHER)
            this.startActivity(i)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }


    fun Context.getVersionName(): String? {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.tag(TAG).e("getVersionName: Package not found")
            Timber.tag(TAG).e(e)
            null
        }
    }

    fun Context.getVersionCode(): Long? {
        return try {
            packageManager.getPackageInfo(packageName, 0).longVersionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.tag(TAG).e("getVersionCode: Package not found")
            Timber.tag(TAG).e(e)
            null
        }
    }

    fun Context.sendLocalBroadcast(intent: Intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    fun Context.registerLocalReceiver(broadcastReceiver: BroadcastReceiver, intentFilter: IntentFilter) {
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)
    }

    fun Context.unregisterLocalReceiver(broadcastReceiver: BroadcastReceiver) {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    fun Context.showAlert(@StringRes title: Int, @StringRes message: Int) {
        val alert = MaterialAlertDialogBuilder(this)
        alert.setMessage(getString(message))
        alert.setTitle(getString(title))
        alert.setNegativeButton(android.R.string.ok, null)
        alert.create().show()
    }

    fun Context.showInputAlert(layoutInflater: LayoutInflater, @StringRes title: Int, @StringRes hint: Int, value: String, callback: ((String?) -> Unit)) {
        val content = DialogTextinputBinding.inflate(layoutInflater)
        content.textInputLayout.hint = getString(hint)
        content.text1.text = Editable.Factory.getInstance().newEditable(value)

        AlertDialog.Builder(this)
            .setTitle(getString(title))
            .setView(content.root)
            .setPositiveButton(android.R.string.ok) { inputDialog, _ ->
                val input = (inputDialog as AlertDialog).requireViewById<TextView>(android.R.id.text1)
                callback.invoke(input.text.toString())
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                callback.invoke(null)
            }
            .create()
            .show()
    }

    fun Context.openUri(uri: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    }

    fun Context.getAppName(packageName: String): CharSequence? {
        return try {
            packageManager.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }?.let {
            packageManager.getApplicationLabel(it)
        }
    }

    fun Context.getAppNameFromUid(uid: Int): String? {
        packageManager.getPackagesForUid(uid)?.forEach { pkg ->
            getAppName(pkg)?.let  {
                return it.toString()
            }
        }
        return null
    }

    const val TAG = "ContextExtensions"
}