package me.timschneeberger.rootlessjamesdsp.utils.extensions

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.util.Base64
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.DialogTextinputBinding
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.CompatExtensions.getApplicationInfoCompat
import me.timschneeberger.rootlessjamesdsp.utils.extensions.CompatExtensions.getPackageInfoCompat
import me.timschneeberger.rootlessjamesdsp.utils.isPlugin
import me.timschneeberger.rootlessjamesdsp.utils.isRootless
import timber.log.Timber
import java.io.File
import kotlin.math.roundToInt


object ContextExtensions {
    /**
     * Converts to dp.
     */
    val Int.pxToDp: Int
        get() = (this / Resources.getSystem().displayMetrics.density).toInt()

    /**
     * Converts to px.
     */
    val Int.dpToPx: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    /**
     * Returns the color for the given attribute.
     *
     * @param resource the attribute.
     * @param alphaFactor the alpha number [0,1].
     */
    @ColorInt
    fun Context.getResourceColor(@AttrRes resource: Int, alphaFactor: Float = 1f): Int {
        val typedArray = obtainStyledAttributes(intArrayOf(resource))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()

        if (alphaFactor < 1f) {
            val alpha = (color.alpha * alphaFactor).roundToInt()
            return Color.argb(alpha, color.red, color.green, color.blue)
        }

        return color
    }

    @ColorInt
    fun Context.resolveColorAttribute(@AttrRes resId: Int): Int {
        val value = TypedValue()
        this.theme.resolveAttribute(resId, value, true)
        return ContextCompat.getColor(this, value.resourceId)
    }

    fun Context.openPlayStoreApp(pkgName:String?){
        if(!pkgName.isNullOrEmpty()) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkgName")))
            } catch (e: ActivityNotFoundException) {
                try {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$pkgName")
                        )
                    )
                }
                catch (e: ActivityNotFoundException) {
                    toast(getString(R.string.no_activity_found))
                }
            }
        }
    }

    fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
        @Suppress("DEPRECATION")
        return getSystemService<ActivityManager>()
            ?.getRunningServices(Integer.MAX_VALUE)
            ?.any { serviceClass.name == it.service.className }
            ?: false
    }

    fun Context.isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfoCompat(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** Open another app.
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

    /**
     * Convenience method to acquire a partial wake lock.
     */
    fun Context.acquireWakeLock(tag: String): PowerManager.WakeLock {
        return getSystemService<PowerManager>()!!
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$tag:WakeLock")
            .apply { acquire(10*60*1000L /*10 minutes*/) }
    }


    @SuppressLint("BatteryLife")
    fun Context.requestIgnoreBatteryOptimizations() {
        if (!isRootless() && !getSystemService<PowerManager>()!!.isIgnoringBatteryOptimizations(packageName)) {
            try {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")))
            }
            catch(ex: ActivityNotFoundException) {
                toast(getString(R.string.no_activity_found))
            }
        }
    }

    // Very simple & naive app cloner checks; please don't use multiple instances at once
    private val PKGNAME_REFS = setOf("bWUudGltc2NobmVlYmVyZ2VyLnJvb3RsZXNzamFtZXNkc3A=",
        "bWUudGltc2NobmVlYmVyZ2VyLnJvb3RsZXNzamFtZXNkc3AuZGVidWc=",
        "amFtZXMuZHNw", "amFtZXMuZHNwLmRlYnVn")
    private val APPNAME_REFS = setOf("Um9vdGxlc3NKYW1lc0RTUA==", "SmFtZXNEU1A=")
    fun Context.check(): Int {
        val appName = getAppName()
        if(isPlugin()) return 0
        if(PKGNAME_REFS.none { decode(it) == packageName }) return 1
        if(APPNAME_REFS.none { decode(it) == appName }) return 2
        if(!BuildConfig.DEBUG && packageName.contains("debug")) return 3
        return 0
    }

    private fun decode(input: String): String {
        return String(Base64.decode(input, 0), Charsets.UTF_8)
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
        showAlert(getString(title), getString(message))
    }

    fun Context.showAlert(title: CharSequence, message: CharSequence) {
        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setTitle(title)
            .setNegativeButton(android.R.string.ok, null)
            .create()
            .show()
    }

    fun Context.showYesNoAlert(title: String, message: String, callback: ((Boolean) -> Unit)) {
        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setTitle(title)
            .setNegativeButton(getString(R.string.no)) { _, _ ->
                callback.invoke(false)
            }
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                callback.invoke(true)
            }
            .create()
            .show()
    }

    fun Context.showYesNoAlert(@StringRes title: Int, @StringRes message: Int, callback: ((Boolean) -> Unit)) {
        showYesNoAlert(getString(title), getString(message), callback)
    }

    fun Context.showSingleChoiceAlert(
        @StringRes title: Int,
        choices: Array<CharSequence>,
        checkedIndex: Int,
        callback: ((Int?) -> Unit)
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(title))
            .setSingleChoiceItems(choices, checkedIndex) { dialog, i ->
                dialog.dismiss()
                callback.invoke(i)
            }
            .setNegativeButton(getString(android.R.string.cancel)) { _, _ ->
                callback.invoke(null)
            }
            .create()
            .show()
    }

    fun Context.showInputAlert(
        layoutInflater: LayoutInflater,
        @StringRes title: Int,
        @StringRes hint: Int,
        value: String,
        isNumberInput: Boolean,
        suffix: String?,
        callback: ((String?) -> Unit)
    ) {
        showInputAlert(layoutInflater, getString(title), getString(hint), value, isNumberInput, suffix, callback)
    }

    fun Context.showInputAlert(
        layoutInflater: LayoutInflater,
        title: String?,
        hint: String?,
        value: String,
        isNumberInput: Boolean,
        suffix: String?,
        callback: ((String?) -> Unit)
    ) {
        val content = DialogTextinputBinding.inflate(layoutInflater)
        content.textInputLayout.hint = hint
        content.text1.text = Editable.Factory.getInstance().newEditable(value)
        if(isNumberInput)
            content.text1.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        content.textInputLayout.suffixText = suffix

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(content.root)
            .setPositiveButton(android.R.string.ok) { inputDialog, _ ->
                val input = (inputDialog as AlertDialog).findViewById<TextView>(android.R.id.text1)
                callback.invoke(input?.text?.toString())
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                callback.invoke(null)
            }
            .create()
            .show()
    }

    fun Context.showChoiceAlert(
        entries: Array<CharSequence>,
        @StringRes titleRes: Int,
        @StringRes positiveRes: Int,
        @StringRes negativeRes: Int = android.R.string.cancel,
        onConfirm: (index: Int) -> Unit
    ) {
        var selected = -1
        MaterialAlertDialogBuilder(this)
            .setSingleChoiceItems(
                entries,
                -1
            ) { _, which: Int ->
                selected = which
            }
            .setTitle(getString(titleRes))
            .setNegativeButton(getString(negativeRes)){ _, _ -> }
            .setPositiveButton(getString(positiveRes)){ _, _ ->
                selected.let {
                    if(it >= 0)
                        onConfirm(it)
                }
            }
            .create()
            .show()
    }

    fun <T> Context.showMultipleChoiceAlert(
        entries: Array<CharSequence>,
        entryValues: Array<T>,
        @StringRes titleRes: Int,
        @StringRes positiveRes: Int,
        @StringRes negativeRes: Int = android.R.string.cancel,
        onConfirm: (selected: List<T>) -> Unit
    ) {
        val selected = arrayListOf<T>()
        MaterialAlertDialogBuilder(this)
            .setMultiChoiceItems(
                entries,
                null
            ) { _: DialogInterface, which: Int, isChecked: Boolean ->
                entryValues.getOrNull(which)?.let {
                    if(isChecked)
                        selected.add(it)
                    else if(selected.contains(it))
                        selected.remove(it)
                }
            }
            .setTitle(getString(titleRes))
            .setNegativeButton(getString(negativeRes)){ _, _ -> }
            .setPositiveButton(getString(positiveRes)){ _, _ ->
                selected.let {
                    if(it.isNotEmpty())
                        onConfirm(it)
                }
            }
            .create()
            .show()
    }

    fun Context.toast(message: String, long: Boolean = true) = Toast.makeText(this, message,
        if(long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    fun Context.toast(@StringRes message: Int, long: Boolean = true) = toast(getString(message), long)

    fun resolveReservedUid(uid: Int): String? {
        return when (uid) {
            Process.ROOT_UID -> "root"
            Process.SHELL_UID -> "com.android.shell"
            /*Process.MEDIA_UID*/ 1013 ->  "media"
            /*Process.AUDIOSERVER_UID*/ 1041 -> "audioserver"
            /*Process.CAMERASERVER_UID*/ 1047 -> "cameraserver"
            Process.SYSTEM_UID -> "android"
            else -> null
        }
    }

    fun Context.getAppName(): String = applicationInfo.loadLabel(packageManager).toString()

    fun Context.getAppName(packageName: String): CharSequence? {
        return try {
            packageManager.getApplicationInfoCompat(packageName, 0)
        } catch (e: Exception) {
            null
        }?.let {
            packageManager.getApplicationLabel(it)
        }
    }

    fun Context.getAppNameFromUid(uid: Int): String? {
        val reserved = resolveReservedUid(uid)
        if(reserved != null)
            return reserved

        val pkg = getPackageNameFromUid(uid)
        val name = pkg?.let { getAppName(it) }
        if(name != null)
            return name.toString()

        return pkg
    }

    fun Context.getAppNameFromUidSafe(uid: Int): String {
        val name = getAppNameFromUid(uid)
        if(name != null)
            return name

        return "UID $uid"
    }

    fun Context.getUidFromPackage(packageName: String): Int {
        return try {
            packageManager.getApplicationInfoCompat(packageName, 0).uid
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e("Cannot get UID. Package not found: $packageName")
            -1
        }
    }

    fun Context.getPackageNameFromUid(uid: Int): String? {

        var pkgName: String? = null
        try {
            pkgName = packageManager.getPackagesForUid(uid)?.firstOrNull()
        }
        catch (_: SecurityException) {}
        catch (ex: Exception) { Timber.w(ex) }

        if(pkgName != null)
            return pkgName

        try {
            pkgName = packageManager.getNameForUid(uid)
        }
        catch (_: SecurityException) {}
        catch (ex: Exception) { Timber.w(ex) }

        return pkgName
    }


    fun Context.getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun Context.hideKeyboardFrom(view: View) {
        getSystemService<InputMethodManager>()
            ?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun Context.ensureCacheDir(name: String): File {
        return File(cacheDir, name).apply { isDirectory || mkdirs() }
    }

    fun Context.restoreDspSettings(silent: Boolean = false) {
        // Delete DSP settings
        Timber.d("Reverting dsp preferences")
        File(applicationInfo.dataDir + "/shared_prefs")
            .listFiles()?.forEach next@ { f ->
                if(!f.name.startsWith("dsp_") || f.extension != "xml" || f.isDirectory)
                    return@next
                f.delete()
            }

        if(!silent) {
            broadcastPresetLoadEvent()
        }
    }

    fun Context.broadcastPresetLoadEvent() {
        sendLocalBroadcast(Intent(Constants.ACTION_PREFERENCES_UPDATED))
        sendLocalBroadcast(Intent(Constants.ACTION_PRESET_LOADED))
    }
}