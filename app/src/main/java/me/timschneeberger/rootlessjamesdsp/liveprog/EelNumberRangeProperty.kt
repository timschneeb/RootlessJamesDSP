package me.timschneeberger.rootlessjamesdsp.liveprog

import timber.log.Timber
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

open class EelNumberRangeProperty<T:Number>(
    key: String,
    description: String,
    var default: T?,
    var value: T,
    val minimum: T,
    val maximum: T,
    val step: T
) : EelBaseProperty(key, description) {
    init {
        if(minimum.toDouble() >= maximum.toDouble()) {
            throw NumberFormatException("Minimum must be smaller than the maximum (key=$key)")
        }

        value = validateRange(value)
    }

    @Suppress("UNCHECKED_CAST")
    public fun validateRange(input: T): T {
        return when (input) {
            is Double -> min(max(minimum.toDouble(), input.toDouble()), maximum.toDouble()) as T
            is Int -> min(max(minimum.toInt(), input.toInt()), maximum.toInt()) as T
            is Float -> min(max(minimum.toFloat(), input.toFloat()), maximum.toFloat()) as T
            else -> throw IllegalArgumentException("unsupported type")
        }
    }

    fun handleAsInt(): Boolean {
        return floor(step.toDouble()) == step
    }

    override fun hasDefault(): Boolean {
        return default != null
    }

    override fun isDefault(): Boolean {
        if (!hasDefault())
            return false

        return if (value is Double || value is Float) {
            if(default == value)
                return true
            default!!.toDouble().equalsDelta(value.toDouble())
        } else {
            default == value
        }
    }

    override fun toString(): String {
        return "key=$key; desc=$description; value=$value; handleAsInt=${handleAsInt()}; default=$default; min=$minimum; max=$maximum; step=$step"
    }

    private fun Double.equalsDelta(other: Double) = abs(this - other) < 0.00001 * max(abs(this), abs(other))

    companion object {
        private fun matchVariable(key: String, contents: String): MatchResult? {
            val regex = """$key\s*=\s*(-?\d+\.?\d*)\s*;""".toRegex()
            return regex.find(contents)
        }

        fun findVariable(key: String, contents: String): Float? {
            val match = matchVariable(key, contents)
            return match?.groups?.get(1)?.value?.toFloatOrNull()
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