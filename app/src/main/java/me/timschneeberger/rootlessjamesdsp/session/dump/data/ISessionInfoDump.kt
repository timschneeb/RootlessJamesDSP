package me.timschneeberger.rootlessjamesdsp.session.dump.data

import me.timschneeberger.rootlessjamesdsp.model.rootless.AudioSessionEntry

interface ISessionInfoDump : IDump {
    val sessions: HashMap<Int /* sid */, AudioSessionEntry>
}