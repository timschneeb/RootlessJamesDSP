package me.timschneeberger.rootlessjamesdsp.interop.structure

data class DspModule(
    val index: Int,
    val internalName: String,
    val displayName: String,
    val enabled: Boolean,
    val requiresLock: Boolean
)
