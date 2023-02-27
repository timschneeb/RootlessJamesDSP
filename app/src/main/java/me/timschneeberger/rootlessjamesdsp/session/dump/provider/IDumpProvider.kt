package me.timschneeberger.rootlessjamesdsp.session.dump.provider

import android.content.Context

interface IDumpProvider {
    /**
     * Dump audio session information to string for debug purposes
     */
    fun dumpString(context: Context): String
}