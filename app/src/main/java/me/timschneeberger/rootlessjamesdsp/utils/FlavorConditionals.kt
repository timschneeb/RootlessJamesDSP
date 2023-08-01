@file:Suppress("NOTHING_TO_INLINE")
package me.timschneeberger.rootlessjamesdsp.utils

import me.timschneeberger.rootlessjamesdsp.BuildConfig

inline fun isRootless() = BuildConfig.ROOTLESS && !BuildConfig.PLUGIN
inline fun isRoot() = !BuildConfig.ROOTLESS && !BuildConfig.PLUGIN
inline fun isPlugin() = BuildConfig.PLUGIN
