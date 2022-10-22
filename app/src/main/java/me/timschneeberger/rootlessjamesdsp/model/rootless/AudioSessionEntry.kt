package me.timschneeberger.rootlessjamesdsp.model.rootless

data class AudioSessionEntry(val uid: Int, val packageName: String, val usage: String, val content: String)
{
    override fun toString(): String {
        return "uid=$uid; package=$packageName; usage=$usage; content=$content; isUsageRecordable=${isUsageRecordable(usage)}"
    }

    fun isUsageRecordable(): Boolean
    {
        return isUsageRecordable(usage)
    }

    companion object
    {
        const val USAGE_UNKNOWN = "USAGE_UNKNOWN"
        const val USAGE_MEDIA = "USAGE_MEDIA"
        const val USAGE_GAME = "USAGE_GAME"

        fun isUsageRecordable(usage: String): Boolean
        {
            val u = usage.uppercase().trim()
            return u.contains(USAGE_UNKNOWN) || u.contains(USAGE_MEDIA) || u.contains(USAGE_GAME)
        }
    }
}