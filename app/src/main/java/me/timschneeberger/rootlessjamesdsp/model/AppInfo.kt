package me.timschneeberger.rootlessjamesdsp.model

import android.graphics.drawable.Drawable

data class AppInfo (
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    val isSystem: Boolean,
    val uid: Int,
)