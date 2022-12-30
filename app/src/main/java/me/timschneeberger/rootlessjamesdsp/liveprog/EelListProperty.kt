package me.timschneeberger.rootlessjamesdsp.liveprog

import timber.log.Timber


class EelListProperty(
    key: String,
    description: String,
    default: Int?,
    value: Int,
    minimum: Int,
    maximum: Int,
    step: Int,
    val options: List<String>
) : EelNumberRangeProperty<Int>(key, description, default, value, minimum, maximum, step) {

    init {
        if(minimum != 0) {
            throw NumberFormatException("Minimum must be zero for list-type parameters")
        }
    }

    override fun toString(): String {
        return "${super.toString()}; options=${options.joinToString(",")}"
    }

    companion object {
        private fun matchVariable(key: String, contents: String): MatchResult? {
            val regex = """$key\s*=\s*(-?\d+\.?\d*)\s*;""".toRegex()
            return regex.find(contents)
        }

        fun findVariable(key: String, contents: String): Int? {
            val match = matchVariable(key, contents)
            return match?.groups?.get(1)?.value?.toIntOrNull()
        }

        fun replaceVariable(key: String, replacement: String, contents: String): String? {
            val match = matchVariable(key, contents)
            match ?: return null

            return match.groups[1]?.range?.let {
                contents.replaceRange(it, replacement)
            }
        }
    }
}