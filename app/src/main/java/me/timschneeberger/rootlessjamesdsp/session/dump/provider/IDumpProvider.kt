package me.timschneeberger.rootlessjamesdsp.session.dump.provider

import android.content.Context
import me.timschneeberger.rootlessjamesdsp.session.dump.data.IDump
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionInfoDump

interface IDumpProvider {
    /**
     * Dump audio session information to string for debug purposes
     */
    fun dumpString(context: Context): String
}