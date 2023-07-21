package me.timschneeberger.rootlessjamesdsp.session.dump.utils

import android.content.Context
import android.os.ParcelFileDescriptor
import me.timschneeberger.rootlessjamesdsp.utils.extensions.PermissionExtensions.hasDumpPermission
import rikka.shizuku.SystemServiceHelper
import timber.log.Timber
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

object DumpUtils {
    fun dumpLines(context: Context, service: String, args: Array<String> = arrayOf<String>()): List<String>? {
        return dumpAll(context, service, args)?.lines()
    }

    fun dumpAll(context: Context, service: String, args: Array<String> = arrayOf<String>()): String? {
        if(!context.hasDumpPermission())
            return null

        try {
            val pipe = ParcelFileDescriptor.createPipe()
            val readPipe = pipe[0]
            val writePipe = pipe[1]

            val serviceBinder = SystemServiceHelper.getSystemService(service)
            if (serviceBinder == null) {
                Timber.wtf("Service '$service' does not exist")
                return null
            }
            serviceBinder.dumpAsync(writePipe.fileDescriptor, args)
            writePipe.close()

            val fd = FileInputStream(readPipe.fileDescriptor)
            val reader = InputStreamReader(fd, "UTF-8")
            val dump = reader.readText()
            reader.close()
            readPipe.close()
            fd.close()
            return dump
        }
        catch (ex: IOException)
        {
            Timber.e("IOException during dump")
            Timber.d(ex)
            return null
        }
        catch (ex: Exception)
        {
            Timber.wtf(ex)
            return null
        }
    }
}