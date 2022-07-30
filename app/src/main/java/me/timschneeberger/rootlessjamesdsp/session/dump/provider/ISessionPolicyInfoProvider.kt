package me.timschneeberger.rootlessjamesdsp.session.dump.provider

import android.content.Context

interface ISessionPolicyInfoProvider : IDumpProvider {
    /**
     * Dump audio session information as ISessionInfoDump
     */
    fun dump(context: Context): ISessionPolicyInfoProvider?

    /**
     * Dump audio session information to string for debug purposes
     */
    override fun dumpString(context: Context): String
}