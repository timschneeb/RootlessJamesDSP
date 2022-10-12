package me.timschneeberger.rootlessjamesdsp.model.preference

enum class SessionUpdateMode(val value: Int) {
    Listener(0),
    ContinuousPolling(1);

    companion object {
        fun fromInt(value: Int) = values().first { it.value == value }
    }
}