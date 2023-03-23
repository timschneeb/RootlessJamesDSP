package me.timschneeberger.rootlessjamesdsp.liveprog

import me.timschneeberger.rootlessjamesdsp.utils.extensions.md5
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException

class EelParser {
    var isFileLoaded: Boolean
        get() = path != null
        private set (_) {}
    var path: String? = null
        private set
    var fileName: String? = null
        private set
    var contents: String? = null
    var description: String? = null
        private set
    var tags = listOf<String>()
        private set
    var hasDescription: Boolean = false
        private set
    var properties = ArrayList<EelBaseProperty>()
        private set
    private var lastFileHash: ByteArray? = null

    private fun reset() {
        properties.clear()
        tags = listOf()
        hasDescription = false
        description = null
        path = null
        fileName = null
        contents = null
        lastFileHash = null
    }

    fun load(path: String, force: Boolean = false, skipParse: Boolean = false, skipProperties: Boolean = false): Boolean {
        if(path.isBlank()) {
            reset()
            return false
        }

        this.path = path
        try {
            contents = File(path).readText()
            fileName = File(path).nameWithoutExtension
        }
        catch (ex: FileNotFoundException) {
            Timber.e("File not found '$path'")
            reset()
            return false
        }

        Timber.d("Loaded '$path'")

        if(!force && lastFileHash != null && lastFileHash.contentEquals(contents?.md5)) {
            Timber.w("Parsing skipped. Script identical with previous file.")
            return false
        }

        properties.clear()
        tags = listOf()
        hasDescription = false
        description = null
        lastFileHash = null

        if(!skipParse) {
            parse(skipProperties)
        }

        lastFileHash = contents?.md5
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

    fun parse(skipProperties: Boolean = false) {
        // Parse description & tags
        parseDescription()
        parseTags()

        properties = arrayListOf()

        if(skipProperties)
            return

        // Parse number range parameters
        contents?.lines()?.forEach next@ {
            EelPropertyFactory.create(it, contents!!)?.let(properties::add)
        }
    }

    fun findAnnotationLine(name: String): Int {
        val regex = """(?:^|(?<=\n))(?:\s*)(\@[^\s\W]*)""".toRegex()
        var count = 0
        contents?.lines()?.forEach { line ->
            count++
            val match = regex.find(line)?.groups?.get(1)?.value
            match?.let {
                if(it == name)
                    return count
            }
        }
        return -1
    }

    fun refresh(): Boolean {
        if(!isFileLoaded || path == null)
            return false

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

    fun restoreDefaults(): Boolean {
        if(!isFileLoaded)
            return false

        properties.forEach { prop ->
            prop.restoreDefaults()
            manipulateProperty(prop)
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

        Timber.d("Manipulating property ${prop.key} to value '${prop.valueAsString()}'")
        val result = contents?.let { prop.manipulateProperty(it) }
        if(result == null) {
            Timber.e("Failed to manipulate '${prop.key}' by replacing its value with '${prop.valueAsString()}' (source=${prop})")
            return false
        }
        contents = result
        return save()
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

    private fun parseTags() {
        tags = listOf()

        if(path == null)
            return

        val tagRegex = """(?:^|(?<=\n))(?:[\W\/]*tags:)([\s\S][^\n]*)""".toRegex()
        val match = tagRegex.find(contents ?: "")

        tags = match?.groups?.get(1)?.value?.trim()?.split(" ")?.map(String::trim) ?: listOf()

        Timber.d("Found tags: $tags")
    }
}