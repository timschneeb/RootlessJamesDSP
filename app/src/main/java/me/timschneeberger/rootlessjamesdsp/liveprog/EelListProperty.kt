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
        if (minimum != 0) {
            throw NumberFormatException("Minimum must be zero for list-type parameters")
        }
    }

    override fun toString(): String {
        return "${super.toString()}; options=${options.joinToString(",")}"
    }

    override fun valueAsString() = value.toString()

    override fun manipulateProperty(contents: String): String? {
        return replaceVariable(key, valueAsString(), contents)
    }


    companion object : IPropertyCompanion {
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

        override val definitionRegex =
            """^\s*(?<var>\w+)\s*:\s*(?<def>-?\d+\.?\d*)?\s*<\s*(?<min>-?\d+\.?\d*)\s*,\s*(?<max>-?\d+\.?\d*)\s*,\s*(?<step>-?\d+\.?\d*)?\s*\{(?<opt>[^\}]*)\}\s*>\s*(?<desc>[\s\S][^\n]*)""".toRegex()

        @Suppress("UNUSED_VARIABLE")
        override fun parse(line: String, contents: String): EelBaseProperty? {
            val matchList = definitionRegex.find(line)
            val groupsList = matchList?.groups ?: return null

            val key = groupsList["var"]?.value
            val def = groupsList["def"]?.value
            val min = groupsList["min"]?.value
            val max = groupsList["max"]?.value
            val step = groupsList["step"]?.value ?: "1"
            val opt = groupsList["opt"]?.value
            val desc = groupsList["desc"]?.value?.trim()

            if (key == null || desc == null || min == null || max == null || opt == null) {
                return null
            }

            // Decouple discovery from assignments: use declared default or 0
            val current = def?.toIntOrNull() ?: 0

            try {
                return EelListProperty(
                    key,
                    desc,
                    def?.toIntOrNull(),
                    current,
                    min.toInt(),
                    max.toInt(),
                    1,
                    opt.split(',').map(String::trim)
                ).also { Timber.d("EelParser: Found list option parameter: $it") }
            } catch (ex: NumberFormatException) {
                Timber.e("EelParser: Failed to parse list option parameter (key=$key)")
                Timber.e(ex)
            }
            return null
        }
    }
}