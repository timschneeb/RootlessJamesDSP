package me.timschneeberger.rootlessjamesdsp.model

import android.content.Context
import me.timschneeberger.rootlessjamesdsp.BuildConfig
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
    private val currentPath = File(ctx.applicationInfo.dataDir + "/shared_prefs")

    fun load(): PresetMetadata {
        val file = File(externalPath, name)
        Timber.d("Loading preset from ${file.path}")

        val targetFolder = File(ctx.cacheDir, "preset")
        if(targetFolder.exists())
            targetFolder.delete()
        targetFolder.mkdir()

        val metadataBytes = ByteArrayOutputStream()
        TarInputStream(BufferedInputStream(FileInputStream(file))).use { tis ->
            var entry: TarEntry
            while (tis.nextEntry.also { entry = it } != null) {
                var count: Int
                val data = ByteArray(2048)
                BufferedOutputStream(FileOutputStream(
                    targetFolder.absolutePath + "/" + entry.name
                )).use { dest ->
                    while (tis.read(data).also { count = it } != -1) {
                        if(entry.name == "metadata")
                            metadataBytes.write(data)
                        else
                            dest.write(data, 0, count)
                    }
                    dest.flush()
                }
            }
            metadataBytes.flush()
        }

        val metadata = mutableMapOf<String, String>()
        metadataBytes.toString().lines().forEach {
            val args = it.split("=")
            if(args.count() < 2)
                return@forEach
            metadata[args[0]] = args[1]
        }

        Timber.d("Loaded preset file version ${metadata[META_VERSION]}")

        targetFolder.listFiles()?.forEach next@ { f ->
            if(!f.startsWith("dsp_") || f.extension != "xml") {
                Timber.w("load: Unknown file in archive ${f.name}")
                return@next
            }

            f.copyTo(currentPath, overwrite = true)
        }

        // TODO send force reload broadcast
        return metadata
    }

    fun save(): Boolean {
        val targetFile = File(externalPath, name)
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

            currentPath
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
    }
}

