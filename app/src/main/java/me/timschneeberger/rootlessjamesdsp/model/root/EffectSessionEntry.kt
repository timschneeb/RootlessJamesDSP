package me.timschneeberger.rootlessjamesdsp.model.root

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspRemoteEngine

data class EffectSessionEntry(
    var session: Int,
    var packageName: String?,
    var effect: JamesDspRemoteEngine?
) : CoroutineScope by CoroutineScope(Dispatchers.Default)