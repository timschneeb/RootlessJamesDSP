package me.timschneeberger.rootlessjamesdsp.session.dump.data

import me.timschneeberger.rootlessjamesdsp.model.AudioSessionDumpEntry

interface ISessionInfoDump : IDump {
    val sessions: HashMap<Int /* sid */, AudioSessionDumpEntry>
}