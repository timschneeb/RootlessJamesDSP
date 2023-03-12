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
            """(?<var>\w+):(?<def>-?\d+\.?\d*)?<(?<min>-?\d+\.?\d*),(?<max>-?\d+\.?\d*),?(?<step>-?\d+\.?\d*)?\{(?<opt>[^\}]*)\}>(?<desc>[\s\S][^\n]*)""".toRegex()

        @Suppress("UNUSED_VARIABLE")
        override fun parse(line: String, contents: String): EelBaseProperty? {
            val matchList = definitionRegex.find(line)
            val groupsList = matchList?.groups ?: return null

            val key = groupsList[1]?.value
            val def = groupsList[2]?.value
            val min = groupsList[3]?.value
            val max = groupsList[4]?.value
            val step = groupsList[5]?.value ?: "1"
            val opt = groupsList[6]?.value
            val desc = groupsList[7]?.value?.trim()

            if (key == null || desc == null || min == null || max == null || opt == null) {
                return null
            }

            val current = findVariable(key, contents)
            if (current == null) {
                Timber.e("Failed to find current value of list option parameter (key=$key)")
                return null
            }

            try {
                return EelListProperty(
                    key,
                    desc,
                    def?.toInt(),
                    current,
                    min.toInt(),
                    max.toInt(),
                    1,
                    opt.split(',').map(String::trim)
                ).also { Timber.d("Found list option property: $it") }
            } catch (ex: NumberFormatException) {
                Timber.e("Failed to parse list option parameter (key=$key)")
                Timber.e(ex)
            }
            return null
        }
    }
}