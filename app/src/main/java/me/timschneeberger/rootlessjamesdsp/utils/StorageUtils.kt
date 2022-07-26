package me.timschneeberger.rootlessjamesdsp.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


object StorageUtils {
    fun importFile(context: Context, targetDir: String, uri: Uri): File? {
        val destinationFilename = File(
                    targetDir +
                    File.separatorChar +
                    queryName(context, uri)
        )
        try {
            context.contentResolver.openInputStream(uri)?.use { ins ->
                if(!createFileFromStream(ins, destinationFilename))
                    return null
            }
        } catch (ex: Exception) {
            Timber.e(ex.message)
            ex.printStackTrace()
            return null
        }
        return destinationFilename
    }

    private fun createFileFromStream(ins: InputStream, destination: File?): Boolean {
        try {
            FileOutputStream(destination).use { os ->
                val buffer = ByteArray(4096)
                var length: Int
                while (ins.read(buffer).also { length = it } > 0) {
                    os.write(buffer, 0, length)
                }
                os.flush()
            }
        } catch (ex: Exception) {
            Timber.e(ex.message)
            ex.printStackTrace()
            return false
        }
        return true
    }

    fun queryName(context: Context, uri: Uri): String {
        val returnCursor = context.contentResolver.query(uri, null, null, null, null)!!
        val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name: String = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }
}