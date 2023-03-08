package me.timschneeberger.rootlessjamesdsp.model

import android.content.Context
import android.content.Intent
import android.system.ErrnoException
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.liveprog.EelParser
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.toast
import org.kamranzafar.jtar.TarEntry
import org.kamranzafar.jtar.TarInputStream
import org.kamranzafar.jtar.TarOutputStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.xml.sax.SAXException
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory


typealias PresetMetadata = Map<String, String>

class Preset(val name: String): KoinComponent {

    private val ctx: Context by inject()
    private val externalPath = File("${ctx.getExternalFilesDir(null)!!.path}/Presets")

    fun file(): File = File(externalPath, name)

    fun rename(newName: String): Boolean {
        return file().renameTo(File(externalPath, newName))
    }

    fun validate(): Boolean {
        return Companion.validate(FileInputStream(file()))
    }

    fun load(): PresetMetadata? {
        val file = file()
        Timber.d("Loading preset from ${file.path}")
        return load(
            ctx,
            try { FileInputStream(file) }
            catch(ex: Exception) {
                Timber.e(ex)
                return null
            }
        )
    }

    fun save(): Boolean {
        val targetFile = file()
        if (targetFile.exists())
            targetFile.delete()

        try {
            externalPath.mkdirs()
        } catch (_: Exception) {}

        Timber.d("Saving preset $name to ${targetFile.path}")

        // Create a TarOutputStream
        try {
            TarOutputStream(BufferedOutputStream(FileOutputStream(targetFile))).use { out ->
                fun addFile(file: File) {
                    if (!file.exists() || file.isDirectory) {
                        Timber.e("addFile: ${file.absolutePath} is not valid")
                        return
                    }

                    out.putNextEntry(TarEntry(file, file.name))
                    BufferedInputStream(FileInputStream(file)).use { origin ->
                        var count: Int
                        val data = ByteArray(2048)
                        while (origin.read(data).also { count = it } != -1) {
                            out.write(data, 0, count)
                        }
                        out.flush()
                    }
                }

                currentPath(ctx)
                    .listFiles()
                    ?.filter { it.name.startsWith("dsp_") }
                    ?.filter { it.extension == "xml" }
                    ?.forEach(::addFile)

                val metadata = mutableMapOf(
                    META_VERSION to "2",
                    META_APP_VERSION to BuildConfig.VERSION_NAME,
                    META_APP_FLAVOR to BuildConfig.FLAVOR,
                    META_LIVEPROG_INCLUDED to false.toString()
                )

                val liveprogPath = findLiveprogScriptPath(ctx)
                if (liveprogPath != null) {
                    val liveprogFile = File(liveprogPath)
                    if (liveprogFile.exists()) {
                        Timber.d("Saving included liveprog script state from '$liveprogPath'")

                        metadata[META_LIVEPROG_INCLUDED] = true.toString()
                        File(ctx.cacheDir, FILE_LIVEPROG).let {
                            liveprogFile.copyTo(it, overwrite = true)
                            addFile(it)
                        }
                    }
                }

                val metadataFile = File(ctx.cacheDir, FILE_METADATA)
                metadataFile.writeText(
                    metadata
                        .map { "${it.key}=${it.value}" }
                        .joinToString("\n")
                )
                addFile(metadataFile)
            }
        }
        catch (ex: ErrnoException) {
            Timber.d(ex)
            ex.localizedMessage?.let { ctx.toast(it) }
            return false
        }
        catch (ex: Exception) {
            Timber.d(ex)
            return false
        }

        return true
    }

    companion object {
        const val FILE_METADATA = "metadata"
        const val FILE_LIVEPROG = "liveprog"

        const val META_VERSION = "version"
        const val META_APP_VERSION = "app_version"
        const val META_APP_FLAVOR = "app_flavor"
        const val META_LIVEPROG_INCLUDED = "liveprog_included" /* version 2+ */

        private fun currentPath(ctx: Context) = File(ctx.applicationInfo.dataDir + "/shared_prefs")
        private fun isExtractableEntry(n: String) = (n.startsWith("dsp_") && n.endsWith("xml")) || n == FILE_LIVEPROG
        private fun isKnownEntry(n: String) = isExtractableEntry(n) || n == FILE_METADATA

        fun validate(stream: InputStream): Boolean {
            Timber.d("Validating preset")

            var knownCount = 0
            try {
                TarInputStream(BufferedInputStream(stream)).use { tis ->
                    var entry: TarEntry?
                    while (tis.nextEntry.also { entry = it } != null) {
                        val entryName = entry?.name
                        entryName ?: break

                        if (!isKnownEntry(entryName)) {
                            Timber.w("Unknown entry name: $entryName")
                            continue
                        }

                        knownCount++
                    }
                }
            }
            catch(ex: IOException) {
                Timber.e("Preset validation failed.")
                Timber.w(ex)
                return false
            }

            if (knownCount < 1) {
                Timber.e("Preset archive did not contain any useful data")
                return false
            }

            return true
        }

        fun load(ctx: Context, stream: InputStream): PresetMetadata? {
            Timber.d("Loading preset from stream")

            val targetFolder = File(ctx.cacheDir, "preset")
            if(targetFolder.exists())
                targetFolder.delete()
            targetFolder.mkdir()

            val metadataBytes = ByteArrayOutputStream()
            try {
                TarInputStream(BufferedInputStream(stream)).use { tis ->
                    var entry: TarEntry?
                    while (tis.nextEntry.also { entry = it } != null) {
                        val entryName = entry?.name
                        entryName ?: break

                        if (!isKnownEntry(entryName)) {
                            Timber.w("Unknown entry name: $entryName")
                            continue
                        }

                        var count: Int
                        val data = ByteArray(2048)
                        BufferedOutputStream(FileOutputStream(
                            targetFolder.absolutePath + "/" + entryName
                        )).use { dest ->
                            while (tis.read(data).also { count = it } != -1) {
                                if (entryName == FILE_METADATA)
                                    metadataBytes.write(data, 0, count)
                                else
                                    dest.write(data, 0, count)
                            }
                            dest.flush()
                        }
                    }
                    metadataBytes.flush()
                }
            }
            catch(ex: IOException) {
                Timber.e("Preset extraction failed.")
                Timber.w(ex)
                return null
            }

            val metadata = mutableMapOf<String, String>()
            metadataBytes.toString().lines().forEach {
                val args = it.split("=")
                if(args.size < 2)
                    return@forEach
                metadata[args[0]] = args[1].trim()
            }

            val version = metadata[META_VERSION]?.toIntOrNull() ?: 2
            Timber.d("Loaded preset file version $version")

            val files = targetFolder.listFiles()
            if(files == null || files.isEmpty()) {
                Timber.e("Preset archive did not contain any useful data")
                return null
            }

            files.forEach next@ { f ->
                if(!isExtractableEntry(f.name))
                    return@next

                val target = File(currentPath(ctx), f.name)
                f.copyTo(target, overwrite = true)
                Timber.d("Extracting to ${target.absolutePath}")
            }

            if (files.any { it.name == FILE_LIVEPROG }) {
                findLiveprogScriptPath(ctx)?.let {
                    val originalFile = File(it)
                    val targetFile = File("${ctx.getExternalFilesDir(null)!!.path}/Liveprog", originalFile.name)
                    val tempPath = File(currentPath(ctx), FILE_LIVEPROG)

                    if(metadata[META_LIVEPROG_INCLUDED].toBoolean()) {
                        if(!targetFile.exists()) {
                            Timber.d("Extracting embedded liveprog file to '${targetFile.absolutePath}'")
                            tempPath.copyTo(targetFile, overwrite = true)
                            tempPath.delete()
                        }
                        else {
                            Timber.d("Copying parameters of embedded liveprog file to '${targetFile.absolutePath}'")
                            val parser = EelParser()
                            val parserNew = EelParser()
                            parser.load(tempPath.absolutePath)
                            parserNew.load(targetFile.absolutePath)
                            parser.properties.forEach(parserNew::manipulateProperty)
                            parserNew.save()
                            tempPath.delete()
                        }
                        ctx.sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_RELOAD_LIVEPROG))
                    }
                }
            }

            ctx.sendLocalBroadcast(Intent(Constants.ACTION_PREFERENCES_UPDATED))
            ctx.sendLocalBroadcast(Intent(Constants.ACTION_PRESET_LOADED))

            return metadata
        }

        private fun findLiveprogScriptPath(ctx: Context): String? {
            val xmlFile = File(currentPath(ctx), "${Constants.PREF_LIVEPROG}.xml")
            if (!xmlFile.exists())
                return null
            try {
                val factory = DocumentBuilderFactory.newInstance()
                val builder = factory.newDocumentBuilder()
                val doc = builder.parse(FileInputStream(xmlFile))
                val nodes = doc.getElementsByTagName("string")

                for(i in 0 until nodes.length) {
                    val node = nodes.item(i)
                    if(node.attributes.getNamedItem("name").nodeValue ==
                        ctx.getString(R.string.key_liveprog_file)) {
                        Timber.d("Found liveprog file path: ${node.textContent}")
                        return node.textContent
                    }
                }
            } catch (e: SAXException) {
                Timber.w(e)
            }
            catch (e: IOException) {
                Timber.w(e)
            }
            return null
        }
    }
}

