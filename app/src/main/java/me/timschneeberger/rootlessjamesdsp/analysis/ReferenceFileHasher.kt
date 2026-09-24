package me.timschneeberger.rootlessjamesdsp.analysis

import android.content.ContentResolver
import android.net.Uri
import java.security.MessageDigest

data class ReferenceFileHashes(
    val sha256: String,
    val md5: String?
)

object ReferenceFileHasher {
    fun hash(contentResolver: ContentResolver, uri: Uri): ReferenceFileHashes {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val md5 = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

        contentResolver.openInputStream(uri)?.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                sha256.update(buffer, 0, read)
                md5.update(buffer, 0, read)
            }
        } ?: throw IllegalArgumentException("Unable to open reference file")

        return ReferenceFileHashes(
            sha256 = sha256.digest().toHexString(),
            md5 = md5.digest().toHexString()
        )
    }

    private fun ByteArray.toHexString(): String {
        return joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
