package me.timschneeberger.rootlessjamesdsp.liveprog


abstract class EelBaseProperty(val key: String, val description: String) {
    /** Whether the property has a default value */
    abstract fun hasDefault(): Boolean
    /** Whether the value is the property is currently set to the default value */
    abstract fun isDefault(): Boolean
    /** Assign default value to current value if available */
    abstract fun restoreDefaults()
    /** Value to string */
    abstract fun valueAsString(): String
    /** Manipulate property in script file and return modified file contents */
    abstract fun manipulateProperty(contents: String): String?
}