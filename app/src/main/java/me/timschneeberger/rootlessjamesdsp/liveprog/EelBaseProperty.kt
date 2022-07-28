package me.timschneeberger.rootlessjamesdsp.liveprog

abstract class EelBaseProperty(val key: String, val description: String) {
    /** Whether the property has a default value */
    abstract fun hasDefault(): Boolean
    /** Whether the value is the property is currently set to the default value */
    abstract fun isDefault(): Boolean
}