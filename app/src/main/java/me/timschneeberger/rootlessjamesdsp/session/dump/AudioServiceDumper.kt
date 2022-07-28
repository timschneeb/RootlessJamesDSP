package me.timschneeberger.rootlessjamesdsp.session.dump

import android.content.Context
import me.timschneeberger.rootlessjamesdsp.session.dump.data.AudioServiceDump
import me.timschneeberger.rootlessjamesdsp.session.dump.data.ISessionInfoDump
import me.timschneeberger.rootlessjamesdsp.session.dump.utils.AudioFlingerServiceDumpUtils
import me.timschneeberger.rootlessjamesdsp.session.dump.utils.DumpUtils
import me.timschneeberger.rootlessjamesdsp.model.AudioSessionEntry
import timber.log.Timber
import java.lang.Exception

class AudioServiceDumper : ISessionInfoProvider {

    override fun dump(context: Context): ISessionInfoDump? {
        val dump = DumpUtils.dumpAll(context, TARGET_SERVICE)
        dump ?: return null

        return process(context, dump)
    }

    private fun process(context: Context, dump: String): ISessionInfoDump {

        // API 29 (No session id)
        val playbackConfRegex29 = """ID:\d+.*u\/pid:(\d+)\/(\d+).*usage=(\w+).*content=(\w+)""".toRegex()
        // API 30 (No session id)
        val playbackConfRegex30 = """AudioPlaybackConfiguration.*u\/pid:(\d+)\/(\d+).*usage=(\w+).*content=(\w+)""".toRegex()
        // API 31+
        val playbackConfRegex31 = """AudioPlaybackConfiguration.*u\/pid:(\d+)\/(\d+).*usage=(\w+).*content=(\w+).*sessionId:(\d+)""".toRegex()

        val sidPidLookupMap = mutableMapOf<Int /* pid */, Int /* sid */>()
        val globalSessionRefs = AudioFlingerServiceDumpUtils.dump(context)
        globalSessionRefs?.forEach {
            if(sidPidLookupMap.contains(it.pid))
            {
                Timber.tag(TAG).w("SID/PID map: Duplicated PID (pid=${it.pid}; sid=${it.sid})")
            }
            else
            {
                Timber.tag(TAG).d("SID/PID map: AudioFlinger: pid=${it.pid}; sid=${it.sid}")
            }

            sidPidLookupMap[it.pid] = it.sid
        }

        val sessions = hashMapOf<Int, AudioSessionEntry>()

        var matches = playbackConfRegex31.findAll(dump)
        // Fallbacks
        if(matches.count() <= 0)
            matches = playbackConfRegex30.findAll(dump)
        if(matches.count() <= 0)
            matches = playbackConfRegex29.findAll(dump)

        // Note: API 29 & 30 lack a session id
        matches.forEach next@ {
            try {
                var uid: Int? = null
                var pid: Int? = null
                var usage: String? = null
                var content = "CONTENT_TYPE_UNKNOWN"
                try{
                    uid = it.groups[1]?.value?.toInt()
                    pid = it.groups[2]?.value?.toInt()
                    usage = it.groups[3]?.value
                    content = it.groups[4]?.value ?: "CONTENT_TYPE_UNKNOWN"
                }
                catch(ex: IndexOutOfBoundsException)
                {
                    Timber.e(ex)
                }

                if(pid == null || uid == null || usage == null)
                {
                    Timber.tag(TAG).e("Failed to parse match for p/uid: $pid/$uid (usage=$usage)")
                    return@next
                }

                var sid = try {
                    it.groups[5]?.value?.toInt()
                }
                catch(ex: Exception) {
                    null
                }
                if(sid == null && sidPidLookupMap.contains(pid))
                {
                    // Fallback to SID/PID table from AudioFlinger
                    Timber.tag(TAG).d("Falling back to SID lookup via AudioFlinger (p/uid=$pid/$uid; usage=$usage; content=$content)")
                    sid = sidPidLookupMap[pid]
                }

                if(sid == null)
                {
                    Timber.tag(TAG).e("Failed to determine session id for p/uid: $pid/$uid (usage=$usage; content=$content)")
                    return@next
                }
                val pkg = context.packageManager.getNameForUid(uid)
                    ?: context.packageManager.getPackagesForUid(uid)?.firstOrNull()
                    ?: uid.toString()
                sessions[sid] = AudioSessionEntry(uid, pkg, usage, content)
            } catch (ex: NumberFormatException) {
                Timber.tag(TAG).e("Failed to parse match")
                Timber.tag(TAG).e(ex)
            }
        }

        Timber.tag(TAG).d("Dump processed")
        return AudioServiceDump(sessions)
    }

    override fun dumpString(context: Context): String {
        val dump = DumpUtils.dumpAll(context, TARGET_SERVICE)
        val sb = StringBuilder("=====> $TARGET_SERVICE raw dump\n")
        sb.append(dump)
        sb.append("\n\n")
        sb.append("=====> $TARGET_SERVICE processed dump\n")
        sb.append(process(context, dump ?: ""))
        sb.append("\n\n")
        sb.append(AudioFlingerServiceDumpUtils.dumpString(context))

        return sb.toString()
    }

    companion object {
        const val TAG = "AudioServiceDumper"
        const val TARGET_SERVICE = "audio"
    }
}
