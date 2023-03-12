package me.timschneeberger.rootlessjamesdsp.liveprog

import kotlin.reflect.full.companionObjectInstance

object EelPropertyFactory {
    private val typeOrder = arrayOf(
        EelNumberRangeProperty::class.companionObjectInstance,
        EelListProperty::class.companionObjectInstance
    ).map {
        it as IPropertyCompanion
    }

    fun create(line: String, contents: String): EelBaseProperty? {
        typeOrder.forEach { factory ->
            factory.parse(line, contents)?.let { return it }
        }
        return null
    }
}