package me.timschneeberger.rootlessjamesdsp.liveprog

import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.floor

class EelParser {
    var isFileLoaded: Boolean
        get() = path != null
        private set (_) {}
    var path: String? = null
        private set
    var fileName: String? = null
        private set
    var contents: String? = null
        private set
    var description: String? = null
        private set
    var hasDescription: Boolean = false
        private set
    var properties = ArrayList<EelBaseProperty>()
        private set

    fun load(path: String): Boolean {
        this.path = path
        try {
            contents = File(path).readText()
            fileName = File(path).nameWithoutExtension
        }
        catch (ex: FileNotFoundException) {
            Timber.e("File not found '$path'")
            this.path = null
            return false
        }
        properties = arrayListOf()

        Timber.d("Loaded '$path'")

        // Parse description
        parseDescription()

        // Parse number range parameters
        val rangeParamRegex = """(?<var>\w+):(?<def>-?\d+\.?\d*)?<(?<min>-?\d+\.?\d*),(?<max>-?\d+\.?\d*),?(?<step>-?\d+\.?\d*)?>(?<desc>[\s\S][^\n]*)""".toRegex()
        contents!!.lines().forEach next@ {
            val match = rangeParamRegex.find(it)
            val groups = match?.groups as? MatchGroupCollection
            if(groups != null) {
                val key = groups[1]?.value
                val def = groups[2]?.value
                val min = groups[3]?.value
                val max = groups[4]?.value
                val step = groups[5]?.value ?: "0.1"
                val desc = groups[6]?.value?.trim()

                if(key == null || desc == null || min == null || max == null) {
                    return@next
                }

                val current = EelNumberRangeProperty.findVariable(key, contents!!)
                if(current == null) {
                    Timber.e("Failed to find current value of number range parameter (key=$key)")
                    return@next
                }

                try {
                    val prop = EelNumberRangeProperty(
                        key,
                        desc,
                        def?.toFloatOrNull(),
                        current,
                        min.toFloat(),
                        max.toFloat(),
                        step.toFloatOrNull() ?: 0.1
                    )
                    Timber.d("Found number range property: $prop")
                    properties.add(prop)
                }
                catch (ex: NumberFormatException) {
                    Timber.e("Failed to parse number range parameter (key=$key)")
                    Timber.e(ex)
                }
            }
        }

        return true
    }

    fun save(): Boolean {
        if(!isFileLoaded)
            return false

        contents?.let {
            File(path!!).writeText(it)
            return true
        }

        return false
    }

    fun refresh(): Boolean {
        if(!isFileLoaded)
            return false
        path ?: return false

        load(path!!)
        return true
    }

    fun canLoadDefaults(): Boolean {
        if(!isFileLoaded)
            return false

        properties.forEach {
            if(it.hasDefault() && !it.isDefault()) {
                return true
            }
        }
        return false
    }

    fun hasDefaults(): Boolean {
        if(!isFileLoaded)
            return false

        properties.forEach {
            if(it.hasDefault()) {
                return true
            }
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    fun restoreDefaults(): Boolean {
        if(!isFileLoaded)
            return false

        properties.forEach { prop ->
            if(prop is EelNumberRangeProperty<*>) {
                // Note: Currently only Float is used with EelNumberRangeProperty
                prop as EelNumberRangeProperty<Float>
                prop.value = prop.default!!
                manipulateProperty(prop)
            }
        }
        save()

        return true
    }

    fun manipulateProperty(prop: EelBaseProperty): Boolean {
        if(!isFileLoaded)
            return false

        val index = properties.indexOfFirst { prop.key == it.key }
        if(index >= 0) {
            properties[index] = prop
        }
        else {
            Timber.w("manipulateProperty: ${prop.key} was not found in property list")
            properties.add(prop)
        }

        if(prop is EelNumberRangeProperty<*>) {
            val value: String = if(prop.handleAsInt()) {
                prop.value.toInt().toString()
            } else {
                "%.2f".format(Locale.ROOT, prop.value)
            }
            Timber.d("Manipulating property: $prop (processedValue=$value)")
            val result = EelNumberRangeProperty.replaceVariable(prop.key, value, contents ?: "")
            if(result == null) {
                Timber.e("Failed to manipulate '${prop.key}' by replacing its value with '$value' (source=${prop.value})")
                return false
            }
            contents = result
            return save()
        }
        return false
    }

    private fun parseDescription() {
        if(path == null)
            return

        val descRegex = """(?:^|(?<=\n))(?:desc:)([\s\S][^\n]*)""".toRegex()
        val match = descRegex.find(contents ?: "")
        val desc = match?.groups?.get(1)?.value?.trim()
        hasDescription = desc != null
        description = desc ?: File(path!!).name

        Timber.d("Found description: $description")
    }
}