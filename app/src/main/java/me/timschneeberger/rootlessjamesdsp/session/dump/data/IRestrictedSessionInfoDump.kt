package me.timschneeberger.rootlessjamesdsp.session.dump.data

interface IRestrictedSessionInfoDump {
    val capturePermissionLog: HashMap<String /* package */, Boolean /* captureAllowed */>

    override fun toString(): String
}