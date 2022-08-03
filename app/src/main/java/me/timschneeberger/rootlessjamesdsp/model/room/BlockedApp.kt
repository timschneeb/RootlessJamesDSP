package me.timschneeberger.rootlessjamesdsp.model.room

import android.graphics.drawable.Drawable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class BlockedApp(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "package_name") val packageName: String?,
    @ColumnInfo(name = "app_name") val appName: String?
) {
    @Ignore var appIcon: Drawable? = null
}