package me.timschneeberger.rootlessjamesdsp.session.dump

import android.content.Context
import me.timschneeberger.rootlessjamesdsp.session.dump.data.AudioPolicyServiceDump
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionInfoDump
import me.timschneeberger.rootlessjamesdsp.session.dump.utils.DumpUtils
import me.timschneeberger.rootlessjamesdsp.model.AudioSessionEntry
import timber.log.Timber
import java.util.*

class AudioPolicyServiceDumper : ISessionInfoProvider {

    override fun dump(context: Context): ISessionInfoDump? {
        val dump = DumpUtils.dumpAll(context, TARGET_SERVICE)
        dump ?: return null

        return process(context, dump)
    }

    private fun process(context: Context, dump: String): ISessionInfoDump {
        // API 29+
        val sessionRegex = """Session Id:\s*(\d+)\s+UID:\s*(\d+)[\S\s]*?Attributes:[\S\s]*?Content type:\s*(\w+)\s*Usage:\s*(\w+)""".toRegex()
        // API 33+
        val sessionRegex33 = """Session ID:\s*(\d+);\s*uid \s*(\d+);[\S\s]*?Attributes:[\S\s]*?Content type:\s*(\w+)\s*Usage:\s*(\w+)""".toRegex()
        // General
        val captureAllowedRegex = """allowPlaybackCapture=(\S+)(?:\s*,).+packageName=(\S+)""".toRegex()

        val sessions = hashMapOf<Int, AudioSessionEntry>()

        var matches = sessionRegex.findAll(dump)
        if(matches.count() <= 0)
        {
            matches = sessionRegex33.findAll(dump)
        }

        matches.forEach next@ {
            try {
                val sid = it.groups[1]?.value?.toInt()
                val uid = it.groups[2]?.value?.toInt()
                val content = it.groups[3]?.value ?: "CONTENT_TYPE_UNKNOWN"
                val usage = it.groups[4]?.value
                if(sid == null || uid == null || usage == null)
                {
                    Timber.tag(TAG).e("Incomplete match at '${it.value}': uid=$uid; sid=$sid; content=$content; usage=$usage")
                    return@next
                }

                val pkg = context.packageManager.getNameForUid(uid)
                    ?: context.packageManager.getPackagesForUid(uid)?.firstOrNull()
                    ?: uid.toString()
                sessions[sid] = AudioSessionEntry(uid, pkg, usage, content)
                Timber.tag(TAG).v("Found session id $sid (uid $uid; usage $usage; content $content; pkg $pkg)")

            } catch (ex: NumberFormatException) {
                Timber.tag(TAG).e("Failed to parse match")
                Timber.tag(TAG).e(ex)
            }
        }

        // Parse capture allow log
        val captureAllowLog = hashMapOf<String, Boolean>()
        captureAllowedRegex.findAll(dump).forEach regexLoop@{ it2 ->
            val pkgName = it2.groups[2]?.value ?: return@regexLoop
            val allowed = it2.groups[1]?.value?.trim()?.lowercase(Locale.ROOT) == "true"
            captureAllowLog[pkgName] = allowed;
            if (!allowed) {
                Timber.tag(TAG).v("Playback capture restricted by $pkgName")
            }
        }

        Timber.tag(TAG).d("Dump processed")
        return AudioPolicyServiceDump(sessions, captureAllowLog)
    }

    override fun dumpString(context: Context): String {
        val dump = DumpUtils.dumpAll(context, TARGET_SERVICE)
        val sb = StringBuilder("=====> $TARGET_SERVICE raw dump\n")
        sb.append(dump)
        sb.append("\n\n")
        sb.append("=====> $TARGET_SERVICE processed dump\n")
        sb.append(process(context, dump ?: ""))

        return sb.toString()
    }

    companion object
    {
        const val TAG = "AudioPolicyServiceDumper"
        const val TARGET_SERVICE = "media.audio_policy"
    }
}
