package me.timschneeberger.rootlessjamesdsp.dump.data

import me.timschneeberger.rootlessjamesdsp.model.AudioSessionEntry

interface ISessionInfoDump {
    val sessions: HashMap<Int /* sid */, AudioSessionEntry>

    override fun toString(): String
}