package me.timschneeberger.rootlessjamesdsp.model

data class RestrictedSessionEntry(val uid: Int, val packageName: String, val isRestricted: Boolean)
{
    override fun toString(): String {
        return "uid=$uid; package=$packageName; isRestricted=$isRestricted"
    }
}