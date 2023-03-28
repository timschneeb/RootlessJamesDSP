package me.timschneeberger.rootlessjamesdsp.model.preset

import android.content.Context
import android.content.Intent
import android.system.ErrnoException
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.backup.BackupManager
import me.timschneeberger.rootlessjamesdsp.liveprog.EelParser
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.broadcastPresetLoadEvent
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import me.timschneeberger.rootlessjamesdsp.utils.storage.Tar
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.xml.sax.SAXException
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class Preset(val name: String, externalPath: File? = null): KoinComponent {
    private val ctx: Context by inject()
    private val externalPath = externalPath ?: File("${ctx.getExternalFilesDir(null)!!.path}/Presets")

    fun file(): File = File(externalPath, name)

    fun rename(newName: String): Boolean {
        return file().renameTo(File(externalPath, newName))
    }

    fun validate(): Boolean {
        return validate(FileInputStream(file()))
    }

    /**
     * @exception Exception if preset cannot be loaded
     */
    fun load(): PresetMetadata {
        val file = file()
        Timber.d("Loading preset from ${file.path}")
        return load(
            ctx,
            FileInputStream(file)
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
            Tar.Composer(targetFile).use { c ->
                c.metadata = mutableMapOf(
                    META_VERSION to PRESET_VERSION,
                    META_APP_VERSION to BuildConfig.VERSION_NAME,
                    META_APP_FLAVOR to BuildConfig.FLAVOR,
                    META_LIVEPROG_INCLUDED to false.toString(),
                    META_MIN_VERSION_CODE to MIN_VERSION_CODE
                )

                currentPath(ctx)
                    .listFiles()
                    ?.filter { it.name.startsWith("dsp_") }
                    ?.filter { it.extension == "xml" }
                    ?.forEach(c::add)

                findLiveprogScriptPath(ctx)?.let { path ->
                    val liveprogFile = File(path)
                    if (liveprogFile.exists()) {
                        Timber.d("Saving included liveprog script state from '$path'")

                        c.metadata[META_LIVEPROG_INCLUDED] = true.toString()
                        File(ctx.cacheDir, FILE_LIVEPROG).let {
                            liveprogFile.copyTo(it, overwrite = true)
                            c.add(it)
                        }
                    }
                }
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
        /* Update constants as needed */
        const val PRESET_VERSION = "3"
        const val MIN_VERSION_CODE = "26"

        const val FILE_LIVEPROG = "liveprog"

        const val META_VERSION = "version"
        const val META_APP_VERSION = "app_version"
        const val META_APP_FLAVOR = "app_flavor"
        const val META_LIVEPROG_INCLUDED = "liveprog_included" /* version 2+ */
        const val META_MIN_VERSION_CODE = "min_version_code" /* version 3+ */

        private fun currentPath(ctx: Context) = File(ctx.applicationInfo.dataDir + "/shared_prefs")
        private fun isKnownEntry(n: String) = (n.startsWith("dsp_") && n.endsWith("xml")) || n == FILE_LIVEPROG

        fun validate(inputStream: InputStream) = Tar.Reader(inputStream, ::isKnownEntry).validate()

        /**
         * @exception Exception if preset cannot be loaded
         */
        fun load(ctx: Context, stream: InputStream): PresetMetadata {
            Timber.d("Loading preset from stream")

            val targetFolder = File(ctx.cacheDir, "preset")
            val metadata = Tar.Reader(stream, ::isKnownEntry).extract(targetFolder)
            metadata ?: throw Exception(ctx.getString(R.string.filelibrary_corrupted))

            if(metadata[BackupManager.META_IS_BACKUP]?.toBoolean() == true) {
                Timber.e("This is a backup file, not a preset file")
                targetFolder.deleteRecursively()
                throw Exception(ctx.getString(R.string.filelibrary_is_backup_not_preset))
            }

            val version = metadata[META_VERSION]?.toIntOrNull() ?: 2
            Timber.d("Loaded preset file version $version")

            val minVersionCode = metadata[META_MIN_VERSION_CODE]?.toIntOrNull() ?: 0
            if(BuildConfig.VERSION_CODE < minVersionCode) {
                Timber.w("Preset too new. Version code $minVersionCode or later required")
                targetFolder.deleteRecursively()
                throw Exception(ctx.getString(R.string.filelibrary_file_too_new))
            }

            val files = targetFolder.listFiles()
            if(files == null || files.isEmpty()) {
                Timber.e("Preset archive did not contain any useful data")
                targetFolder.deleteRecursively()
                throw Exception(ctx.getString(R.string.filelibrary_corrupted))
            }

            files.forEach next@ { f ->
                if(!isKnownEntry(f.name))
                    return@next

                val target = File(currentPath(ctx), f.name)
                f.copyTo(target, overwrite = true)
                Timber.d("Copying to ${target.absolutePath}")
            }

            if (files.any { it.name == FILE_LIVEPROG }) {
                findLiveprogScriptPath(ctx)?.let {
                    val originalFile = File(it)
                    val targetFile =
                        File("${ctx.getExternalFilesDir(null)!!.path}/Liveprog", originalFile.name)
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

            // clean up
            targetFolder.deleteRecursively()

            ctx.broadcastPresetLoadEvent()

            return metadata.toMutableMap()
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
                        return node.textContent.let {
                            ctx.getExternalFilesDir(null)!!.absolutePath + "/" + it
                        }.also {
                            Timber.d("Found liveprog file path: $it")
                        }
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


typealias PresetMetadata = MutableMap<String, String>