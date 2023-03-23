package me.timschneeberger.rootlessjamesdsp.flavor.updates

import android.app.Service
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.IBinder
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.MainActivity
import me.timschneeberger.rootlessjamesdsp.utils.extensions.CompatExtensions.getParcelableAs
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import me.timschneeberger.rootlessjamesdsp.utils.storage.Cache
import timber.log.Timber

/** Based on https://github.com/Iamlooker/Droid-ify/ licensed under GPLv3 */
class SessionInstallerService : Service() {
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        Timber.i("SessionInstallerService: $message ($status)")

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            // prompts user to enable unknown source
            val promptIntent: Intent? = intent.extras?.getParcelableAs(Intent.EXTRA_INTENT)

            promptIntent?.let {
                it.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                it.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                startActivity(it)
            }
        } else {
            notifyStatus(intent)
        }

        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Notifies user of installer outcome.
     */
    private fun notifyStatus(intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                toast(getString(R.string.self_update_finished), true)

                Intent(this, MainActivity::class.java)
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    .let(::startActivity)
                    .also { Runtime.getRuntime().exit(0) }
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                // do nothing if user cancels
            }
            else -> {
                // problem occurred when installing/uninstalling package
                toast(getString(R.string.self_update_install_fail, message), true)
                // cleanup possible corrupted packages
                Cache.cleanupNow(this)
            }
        }
    }
}