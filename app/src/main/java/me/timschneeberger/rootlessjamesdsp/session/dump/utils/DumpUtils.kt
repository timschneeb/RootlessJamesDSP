package me.timschneeberger.rootlessjamesdsp.session.dump.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import rikka.shizuku.SystemServiceHelper
import timber.log.Timber
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

object DumpUtils {
    fun dumpLines(context: Context, service: String): List<String>? {
        return dumpAll(context, service)?.lines()
    }

    fun dumpAll(context: Context, service: String): String? {
        if(context.checkSelfPermission("android.permission.DUMP") == PackageManager.PERMISSION_DENIED)
        {
            return null
        }

        try {
            val pipe = ParcelFileDescriptor.createPipe()
            val readPipe = pipe[0]
            val writePipe = pipe[1]

            val serviceBinder = SystemServiceHelper.getSystemService(service)
            if (serviceBinder == null) {
                Timber.tag(TAG).wtf("Service '$service' does not exist")
                return null
            }
            serviceBinder.dumpAsync(writePipe.fileDescriptor, arrayOf<String>())
            writePipe.close()

            val fd = FileInputStream(readPipe.fileDescriptor)
            val reader = InputStreamReader(fd, "UTF-8")
            val dump = reader.readText()
            reader.close()
            fd.close()
            return dump
        }
        catch (ex: IOException)
        {
            Timber.tag(TAG).e("IOException during dump")
            Timber.tag(TAG).d(ex)
            return null
        }
        catch (ex: Exception)
        {
            Timber.tag(TAG).wtf(ex)
            return null
        }

    }

    const val TAG = "DumpUtils"
}