package me.timschneeberger.rootlessjamesdsp.utils

import android.content.Context
import android.content.res.AssetManager
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

object AssetManagerExtensions {
    fun AssetManager.installPrivateAssets(context: Context) {
        Timber.d("Installing private assets")
        this.copyAssetDir("Convolver", context.getExternalFilesDir(null)!!.absolutePath)
        this.copyAssetDir("DDC", context.getExternalFilesDir(null)!!.absolutePath)
        this.copyAssetDir("Liveprog", context.getExternalFilesDir(null)!!.absolutePath)
    }

    private fun AssetManager.copyAssetDir(assetPath: String, destDirPath: String) {
        this.walkAssetDir(assetPath) {
            this.copyAssetFile(it, "$destDirPath/$it")
        }
    }

    private fun AssetManager.walkAssetDir(assetPath: String, callback: ((String) -> Unit)) {
        val children = this.list(assetPath) ?: return
        if (children.isEmpty()) {
            callback(assetPath)
        } else {
            for (child in children) {
                this.walkAssetDir("$assetPath/$child", callback)
            }
        }
    }

    private fun AssetManager.copyAssetFile(assetPath: String, destPath: String): File {
        val destFile = File(destPath)
        File(destFile.parent!!).mkdirs()
        destFile.createNewFile()

        this.open(assetPath).use { src ->
            FileOutputStream(destFile).use { dest ->
                src.copyTo(dest)
            }
        }

        return destFile
    }
}