package me.timschneeberger.rootlessjamesdsp.session.dump.provider

import android.content.Context
import me.timschneeberger.rootlessjamesdsp.session.dump.data.AudioPolicyServiceDump
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionInfoDump
import me.timschneeberger.rootlessjamesdsp.session.dump.utils.DumpUtils
import me.timschneeberger.rootlessjamesdsp.model.AudioSessionEntry
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.getPackageNameFromUid
import timber.log.Timber
import java.util.*

class AudioFlingerServiceDumpProvider : IDumpProvider {

     override fun dumpString(context: Context): String {
        val dump = DumpUtils.dumpAll(context, TARGET_SERVICE)
        val sb = StringBuilder("=====> $TARGET_SERVICE raw dump\n")
        sb.append(dump)
        sb.append("\n\n")
        sb.append("=====> $TARGET_SERVICE processed dump\n")
        sb.append("Not implemented")

        return sb.toString()
    }

    companion object
    {
        const val TARGET_SERVICE = "media.audio_flinger"
    }
}
