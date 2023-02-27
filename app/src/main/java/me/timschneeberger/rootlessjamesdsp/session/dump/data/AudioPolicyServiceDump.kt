package me.timschneeberger.rootlessjamesdsp.session.dump.data

import me.timschneeberger.rootlessjamesdsp.model.AudioSessionDumpEntry

data class AudioPolicyServiceDump(override val sessions: HashMap<Int /* sid */, AudioSessionDumpEntry>,
                                  override val capturePermissionLog: HashMap<String /* package */, Boolean /* captureAllowed */>) :
    ISessionInfoDump, ISessionPolicyInfoDump
{
    override fun toString(): String {
        val sb = StringBuilder("\n---> Session stack\n")
        sessions.forEach { (key, value) ->
            sb.append("sid=$key\t-> $value\n")
        }
        sb.append("\n--> Capture permission log\n")
        capturePermissionLog.forEach { (key, value) ->
            sb.append("package=$key\t-> isAllowed=$value\n")
        }
        return sb.toString()
    }
}
