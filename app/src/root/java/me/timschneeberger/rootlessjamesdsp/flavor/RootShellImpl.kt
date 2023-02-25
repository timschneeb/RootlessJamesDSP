package me.timschneeberger.rootlessjamesdsp.flavor

import com.topjohnwu.superuser.Shell
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import timber.log.Timber

object RootShellImpl {
    init {
        Shell.enableVerboseLogging = BuildConfig.DEBUG;
        Shell.setDefaultBuilder(Shell.Builder.create()
            .setFlags(Shell.FLAG_REDIRECT_STDERR)
            .setTimeout(10)
        );
    }

    interface OnShellAttachedCallback {
        fun onShellAttached(isRoot: Boolean)
    }

    fun getShell(callback: OnShellAttachedCallback) {
        Shell.getShell {
            callback.onShellAttached(it.isRoot)
        }
    }

    fun cmd(command: String): Boolean {
        Timber.d("root command: $command")
        return Shell.cmd(command).exec().run {
            if(!isSuccess) {
                Timber.e("Command failed; error $code")
                err.forEach(Timber::e)
            }
            isSuccess
        }
    }
}
