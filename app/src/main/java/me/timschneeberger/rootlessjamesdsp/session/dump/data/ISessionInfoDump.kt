package me.timschneeberger.rootlessjamesdsp.session.dump.data

import me.timschneeberger.rootlessjamesdsp.model.AudioSessionEntry

interface ISessionInfoDump : IDump {
    val sessions: HashMap<Int /* sid */, AudioSessionEntry>
}