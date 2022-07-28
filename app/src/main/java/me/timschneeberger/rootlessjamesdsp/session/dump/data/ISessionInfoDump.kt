package me.timschneeberger.rootlessjamesdsp.session.dump.data

import me.timschneeberger.rootlessjamesdsp.model.AudioSessionEntry

interface ISessionInfoDump {
    val sessions: HashMap<Int /* sid */, AudioSessionEntry>

    override fun toString(): String
}