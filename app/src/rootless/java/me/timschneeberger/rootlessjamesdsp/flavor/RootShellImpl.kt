@file:Suppress("UNUSED_PARAMETER")

package me.timschneeberger.rootlessjamesdsp.flavor

// Stubbed
object RootShellImpl {
    interface OnShellAttachedCallback {
        fun onShellAttached(isRoot: Boolean)
    }

    fun getShell(callback: OnShellAttachedCallback) {}
    fun cmd(command: String) = false
}
