package me.timschneeberger.rootlessjamesdsp.utils.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import java.io.File

object ApiExtensions {
    sealed class DownloadState {
        data class Downloading(val progress: Int, val currentBytes: Long, val totalBytes: Long) : DownloadState()
        data class Finished(val file: File) : DownloadState()
        data class Failed(val error: Throwable? = null) : DownloadState()
    }

    fun ResponseBody.save(destinationFile: File): Flow<DownloadState> {
        return flow {
            emit(DownloadState.Downloading(0, 0, contentLength()))

            try {
                byteStream().use { inputStream ->
                    destinationFile.outputStream().use { outputStream ->
                        val totalBytes = contentLength()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var progressBytes = 0L

                        var bytes = inputStream.read(buffer)
                        while (bytes >= 0) {
                            outputStream.write(buffer, 0, bytes)
                            progressBytes += bytes
                            bytes = inputStream.read(buffer)
                            emit(
                                DownloadState.Downloading(
                                    ((progressBytes * 100) / totalBytes).toInt(),
                                    progressBytes,
                                    totalBytes
                                )
                            )
                        }
                    }
                }
                emit(DownloadState.Finished(destinationFile))
            } catch (e: Exception) {
                emit(DownloadState.Failed(e))
            }
        }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()
    }
}
