package me.timschneeberger.rootlessjamesdsp.utils.storage

import android.content.Context
import android.system.ErrnoException
import org.kamranzafar.jtar.TarEntry
import org.kamranzafar.jtar.TarInputStream
import org.kamranzafar.jtar.TarOutputStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object Tar {
    private const val FILE_METADATA = "metadata"

    /**
     * Create tar composer
     * @throws FileNotFoundException if file already exists as a directory or cannot be created for other reasons
     * @throws SecurityException if write access is denied
     */
    class Composer: AutoCloseable, KoinComponent {
        constructor(outputStream: OutputStream) {
            stream = TarOutputStream(outputStream)
        }

        constructor(file: File) {
            stream = TarOutputStream(file)
        }

        private val context: Context by inject()
        private val stream: TarOutputStream

        var metadata = mutableMapOf<String, String>()

        fun add(file: File, entryPath: String? = null): Boolean {
            if (!file.exists() || file.isDirectory) {
                Timber.e("addFile: ${file.absolutePath} is not valid")
                return false
            }

            stream.putNextEntry(TarEntry(file, (entryPath ?: file.name)))
            BufferedInputStream(FileInputStream(file)).use { origin ->
                var count: Int
                val data = ByteArray(2048)
                while (origin.read(data).also { count = it } != -1) {
                    stream.write(data, 0, count)
                }
                stream.flush()
            }
            return true
        }

        override fun close() {
            add(
                File(context.cacheDir, FILE_METADATA).apply {
                    writeText(
                        metadata
                            .map { "${it.key}=${it.value}" }
                            .joinToString("\n")
                    )
                }
            )
            stream.close()
        }
    }

    /** Create tar reader */
    class Reader(
        private val inStream: InputStream,
        private val shouldExtract: ((entryName: String) -> Boolean) = { true }
    ) {
        private fun process(onNextEntry: (tis: TarInputStream, entryName: String) -> Unit) {
            TarInputStream(BufferedInputStream(inStream)).use { tis ->
                var entry: TarEntry?
                while (tis.nextEntry.also { entry = it } != null) {
                    val entryName = entry?.name
                    entryName ?: break

                    if (!shouldExtract(entryName) && entryName != FILE_METADATA) {
                        Timber.w("Entry name ignored: $entryName")
                        continue
                    }

                    onNextEntry(tis, entryName)
                }
            }
        }

        fun validate(): Boolean {
            Timber.d("Validating preset")

            var knownCount = 0
            try {
                process { _, _ -> knownCount++ }
            }
            catch(ex: Exception) {
                Timber.e("Validation failed due to exception")
                Timber.w(ex)
                return false
            }

            if (knownCount < 1) {
                Timber.e("Archive did not contain any useful data")
                return false
            }

            return true
        }

        fun extract(targetFolder: File) : Map<String, String>? {
            if(targetFolder.exists())
                targetFolder.delete()
            targetFolder.mkdirs()

            val metadataBytes = ByteArrayOutputStream()
            try {
                process { stream, name ->
                    var count: Int
                    val data = ByteArray(2048)
                    // create subdirectories in archive entry name
                    File(targetFolder.absolutePath + "/" + name).parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(
                        targetFolder.absolutePath + "/" + name
                    )).use { dest ->
                        while (stream.read(data).also { count = it } != -1) {
                            if (name == FILE_METADATA)
                                metadataBytes.write(data, 0, count)
                            else
                                dest.write(data, 0, count)
                        }
                        dest.flush()
                    }
                }
                metadataBytes.flush()
            }
            catch(ex: ErrnoException) {
                Timber.e("Extraction failed; errno=${ex.errno}")
                Timber.w(ex)
                return null
            }
            catch(ex: IOException) {
                Timber.e("Extraction failed")
                Timber.w(ex)
                return null
            }

            return mutableMapOf<String, String>().apply {
                metadataBytes.toString().lines().forEach {
                    val args = it.split("=")
                    if(args.size < 2)
                        return@forEach

                    this[args[0]] = args[1].trim()
                }
            }
        }
    }

}