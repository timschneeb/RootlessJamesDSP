package me.timschneeberger.rootlessjamesdsp.model

import android.content.Context
import android.content.Intent
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.sendLocalBroadcast
import org.kamranzafar.jtar.TarEntry
import org.kamranzafar.jtar.TarInputStream
import org.kamranzafar.jtar.TarOutputStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.*

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
       return load(ctx, FileInputStream(file))
    }

    fun save(): Boolean {
        val targetFile = file()
        if (targetFile.exists())
            targetFile.delete()

        Timber.d("Saving preset $name to ${targetFile.path}")

        // Create a TarOutputStream
        TarOutputStream(BufferedOutputStream(FileOutputStream(targetFile))).use { out ->
            fun addFile(file: File) {
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

            val metadataFile = File(ctx.cacheDir, "metadata")

            metadataFile.writeText(
                mutableMapOf(
                    META_VERSION to "1",
                    META_APP_VERSION to BuildConfig.VERSION_NAME,
                    META_APP_FLAVOR to BuildConfig.FLAVOR
                )
                    .map{ "${it.key}=${it.value}" }
                    .joinToString("\n")
            )
            addFile(metadataFile)
        }

        return true
    }

    companion object {
        const val META_VERSION = "version"
        const val META_APP_VERSION = "app_version"
        const val META_APP_FLAVOR = "app_flavor"

        private fun currentPath(ctx: Context) = File(ctx.applicationInfo.dataDir + "/shared_prefs")
        private fun isKnownEntry(n: String) = (n.startsWith("dsp_") && n.endsWith("xml")) || n == "metadata"

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
                                if (entryName == "metadata")
                                    metadataBytes.write(data)
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
                if(args.count() < 2)
                    return@forEach
                metadata[args[0]] = args[1]
            }

            Timber.d("Loaded preset file version ${metadata[META_VERSION]}")

            val files = targetFolder.listFiles()
            if(files == null || files.isEmpty()) {
                Timber.e("Preset archive did not contain any useful data")
                return null
            }

            files.forEach next@ { f ->
                if(!isKnownEntry(f.name) || f.name == "metadata")
                    return@next

                val target = File(currentPath(ctx), f.name)
                f.copyTo(target, overwrite = true)
                Timber.d("Extracting to ${target.absolutePath}")
            }

            ctx.sendLocalBroadcast(Intent(Constants.ACTION_PREFERENCES_UPDATED))
            ctx.sendLocalBroadcast(Intent(Constants.ACTION_PRESET_LOADED))
            return metadata
        }
    }
}

