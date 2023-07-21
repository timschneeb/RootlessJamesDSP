package me.timschneeberger.rootlessjamesdsp.session.dump.data

import me.timschneeberger.rootlessjamesdsp.model.AppInfo

data class PackageServiceDump(val apps: List<AppInfo>) : IDump {
    override fun toString(): String {
        val sb = StringBuilder("\n--> Apps\n")
        apps.forEach(sb::append)
        return sb.toString()
    }
}