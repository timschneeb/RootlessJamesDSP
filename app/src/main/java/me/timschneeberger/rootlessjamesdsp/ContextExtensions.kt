package me.timschneeberger.rootlessjamesdsp

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

object ContextExtensions {
    fun Context.isAccessibilityServiceRunning(): Boolean {
        val accessibilityManager =
            getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

        accessibilityManager
            .getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK).forEach {
                Log.d("AccessibilityMgr", it.id)
            }

        return accessibilityManager
            .getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK)
            .map { it.id }
            .contains("me.timschneeberger.rootlessjamesdsp/.PlaybackAccessibilityService")
    }
}