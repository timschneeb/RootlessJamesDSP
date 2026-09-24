package me.timschneeberger.rootlessjamesdsp.model

import java.io.Serializable
import java.util.*

enum class ParametricEqFilterType(val code: Int, val apoLabel: String, val displayLabel: String) {
    PEAKING(0, "PK", "PK"),
    LOW_SHELF(1, "LSC", "LS"),
    HIGH_SHELF(2, "HSC", "HS");

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: PEAKING
        fun fromApoLabel(label: String) = when (label.uppercase()) {
            "PK" -> PEAKING
            "LSC", "LS" -> LOW_SHELF
            "HSC", "HS" -> HIGH_SHELF
            else -> null
        }
    }
}

enum class ParametricEqChannel(val code: Int, val apoLabel: String, val displayLabel: String) {
    LEFT_RIGHT(0, "", "L+R"),
    LEFT(1, "L", "L"),
    RIGHT(2, "R", "R");

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: LEFT_RIGHT
        fun fromApoLabel(label: String?) = when (label?.uppercase()) {
            "L" -> LEFT
            "R" -> RIGHT
            else -> LEFT_RIGHT
        }
    }
}

/**
 * A parametric EQ band definition.
 *
 * [uuid] is excluded from equals/hashCode so that two bands with the
 * same audio parameters compare as equal regardless of identity.
 */
class ParametricEqBand(
    val frequency: Double,
    val gain: Double,
    val q: Double,
    val filterType: ParametricEqFilterType = ParametricEqFilterType.PEAKING,
    val channel: ParametricEqChannel = ParametricEqChannel.LEFT_RIGHT,
    val uuid: UUID = UUID.randomUUID()
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParametricEqBand) return false
        return frequency == other.frequency &&
                gain == other.gain &&
                q == other.q &&
                filterType == other.filterType &&
                channel == other.channel
    }

    override fun hashCode(): Int {
        var result = frequency.hashCode()
        result = 31 * result + gain.hashCode()
        result = 31 * result + q.hashCode()
        result = 31 * result + filterType.hashCode()
        result = 31 * result + channel.hashCode()
        return result
    }

    override fun toString(): String =
        "ParametricEqBand(frequency=$frequency, gain=$gain, q=$q, filterType=$filterType, channel=$channel, uuid=$uuid)"
}
