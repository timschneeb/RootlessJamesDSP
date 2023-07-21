package me.timschneeberger.rootlessjamesdsp.session.dump.provider

import android.content.Context
import me.timschneeberger.rootlessjamesdsp.model.AppInfo
import me.timschneeberger.rootlessjamesdsp.session.dump.data.PackageServiceDump
import me.timschneeberger.rootlessjamesdsp.session.dump.utils.DumpUtils
import me.timschneeberger.rootlessjamesdsp.utils.extensions.PermissionExtensions.hasPackageUsagePermission
import timber.log.Timber

class PackageServiceDumpProvider : IDumpProvider {

    fun dump(context: Context): PackageServiceDump? {
        if(!context.hasPackageUsagePermission()) {
            Timber.e("Package usage permission not granted")
            return null
        }

        val dump = DumpUtils.dumpAll(context, TARGET_SERVICE, arrayOf("packages"))
        dump ?: return null

        return process("$dump\nPackages End")
    }

    private fun process(dump: String): PackageServiceDump {
        val packageHeaderRegex = """Package \[(\S+)]""".toRegex()
        var currentPackage: String? = null
        var currentUid = -1
        var currentIsSystem = false

        val apps = mutableListOf<AppInfo>()
        dump.lines().forEach {
            if(it.contains("Package")) {
                currentPackage?.let { pkg ->
                    apps.add(AppInfo(pkg, pkg, null, currentIsSystem, currentUid))
                }
                currentPackage = packageHeaderRegex.find(dump)?.groups?.get(1)?.value ?: currentPackage
            }

            if(currentPackage == null)
                return@forEach

            if(it.contains("userId"))
                currentUid = it.split("=").getOrNull(1)?.trim()?.toIntOrNull() ?: -1
            if(it.contains("flags"))
                currentIsSystem = it.contains("SYSTEM")
        }
        return PackageServiceDump(apps)
    }
    override fun dumpString(context: Context): String {
        val dump = DumpUtils.dumpAll(context, TARGET_SERVICE, arrayOf("packages"))
        val sb = StringBuilder("=====> $TARGET_SERVICE raw dump\n")
        sb.append(dump)
        sb.append("\n\n")
        sb.append("=====> $TARGET_SERVICE processed dump\n")
        sb.append(process(dump ?: ""))

        return sb.toString()
    }

    companion object
    {
        const val TARGET_SERVICE = "package"
    }
}
