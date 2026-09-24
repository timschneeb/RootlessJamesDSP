package me.timschneeberger.rootlessjamesdsp.analysis

import java.io.BufferedInputStream
import java.io.EOFException
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

data class DecodedWavPcm(
    val displayName: String,
    val sampleRate: Int,
    val channels: Int,
    val durationMs: Long,
    val interleavedStereo: FloatArray
)

/**
 * Streams a WAV file into interleaved stereo floats (mono is duplicated, further channels dropped).
 * Reads at most [MAX_SECONDS] of audio, so long high-resolution files don't exhaust memory.
 * Supports 16/24/32-bit PCM and 32-bit float, including WAVE_FORMAT_EXTENSIBLE headers.
 */
object WavPcmDecoder {
    fun decode(inputStream: InputStream, displayName: String): DecodedWavPcm {
        val input = BufferedInputStream(inputStream, 1 shl 16)
        val riff = input.readExactly(RIFF_HEADER_SIZE)
        require(riff.ascii(0, 4) == "RIFF" && riff.ascii(8, 4) == "WAVE") { "Unsupported WAV container" }

        var audioFormat = 0
        var channels = 0
        var sampleRate = 0
        var blockAlign = 0
        var bitsPerSample = 0

        while (true) {
            val header = try {
                input.readExactly(CHUNK_HEADER_SIZE)
            }
            catch (_: EOFException) {
                throw IllegalArgumentException("WAV has no audio data")
            }
            val id = header.ascii(0, 4)
            val size = header.readIntLe(4).toLong() and 0xffffffffL

            if (id == "fmt ") {
                require(size in 16..1024) { "Invalid WAV fmt chunk" }
                val fmt = input.readExactly(size.toInt())
                audioFormat = fmt.readShortLe(0)
                channels = fmt.readShortLe(2)
                sampleRate = fmt.readIntLe(4)
                blockAlign = fmt.readShortLe(12)
                bitsPerSample = fmt.readShortLe(14)
                // WAVE_FORMAT_EXTENSIBLE: the real format is the first two bytes of the sub-format GUID
                if (audioFormat == WAVE_FORMAT_EXTENSIBLE && size >= 40)
                    audioFormat = fmt.readShortLe(24)
                if (size % 2 == 1L) input.skipFully(1)
                continue
            }

            if (id != "data") {
                input.skipFully(size + (size and 1))
                continue
            }

            require(audioFormat == WAVE_FORMAT_PCM || audioFormat == WAVE_FORMAT_IEEE_FLOAT) {
                "Only PCM and 32-bit float WAV files are supported"
            }
            require(channels > 0) { "WAV has no channels" }
            require(sampleRate > 0) { "WAV has invalid sample rate" }
            require(blockAlign > 0) { "WAV has invalid block alignment" }
            require(bitsPerSample == 16 || bitsPerSample == 24 || bitsPerSample == 32) {
                "Unsupported WAV bit depth: $bitsPerSample"
            }
            require(audioFormat == WAVE_FORMAT_PCM || bitsPerSample == 32) { "Float WAV must be 32-bit" }

            val maxFrames = MAX_SECONDS.toLong() * sampleRate
            val frames = min(size / blockAlign, maxFrames).toInt()
            require(frames > 0) { "WAV has no audio data" }
            return DecodedWavPcm(
                displayName = displayName,
                sampleRate = sampleRate,
                channels = channels,
                durationMs = frames * 1_000L / max(1, sampleRate),
                interleavedStereo = readFrames(input, frames, channels, blockAlign, bitsPerSample, audioFormat)
            )
        }
    }

    private fun readFrames(
        input: InputStream, frames: Int, channels: Int, blockAlign: Int, bitsPerSample: Int, audioFormat: Int
    ): FloatArray {
        val output = FloatArray(frames * OUTPUT_CHANNELS)
        val bytesPerSample = bitsPerSample / 8
        val framesPerRead = max(1, (1 shl 16) / blockAlign)
        val buffer = ByteArray(framesPerRead * blockAlign)
        var frame = 0
        while (frame < frames) {
            val count = min(framesPerRead, frames - frame)
            val read = input.readUpTo(buffer, count * blockAlign) / blockAlign
            if (read <= 0) break
            for (k in 0 until read) {
                val offset = k * blockAlign
                val left = readSample(buffer, offset, bitsPerSample, audioFormat)
                val right = if (channels > 1) readSample(buffer, offset + bytesPerSample, bitsPerSample, audioFormat) else left
                output[(frame + k) * OUTPUT_CHANNELS] = left
                output[(frame + k) * OUTPUT_CHANNELS + 1] = right
            }
            frame += read
        }
        return if (frame == frames) output else output.copyOf(frame * OUTPUT_CHANNELS)
    }

    private fun readSample(bytes: ByteArray, offset: Int, bitsPerSample: Int, audioFormat: Int): Float {
        return if (audioFormat == WAVE_FORMAT_IEEE_FLOAT) {
            Float.fromBits(bytes.readIntLe(offset)).takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: 0f
        }
        else {
            when (bitsPerSample) {
                16 -> bytes.readSigned16Le(offset) / 32768f
                24 -> bytes.readSigned24Le(offset) / 8388608f
                32 -> bytes.readIntLe(offset) / 2147483648f
                else -> 0f
            }
        }
    }

    private fun InputStream.readExactly(count: Int): ByteArray {
        val bytes = ByteArray(count)
        if (readUpTo(bytes, count) < count) throw EOFException()
        return bytes
    }

    private fun InputStream.readUpTo(buffer: ByteArray, count: Int): Int {
        var total = 0
        while (total < count) {
            val n = read(buffer, total, count - total)
            if (n < 0) break
            total += n
        }
        return total
    }

    private fun InputStream.skipFully(count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped <= 0) {
                if (read() < 0) throw EOFException()
                remaining--
            }
            else remaining -= skipped
        }
    }

    private fun ByteArray.ascii(offset: Int, length: Int): String {
        return String(this, offset, length, Charsets.US_ASCII)
    }

    private fun ByteArray.readShortLe(offset: Int): Int {
        return (this[offset].toInt() and 0xff) or ((this[offset + 1].toInt() and 0xff) shl 8)
    }

    private fun ByteArray.readSigned16Le(offset: Int): Int {
        val value = readShortLe(offset)
        return if (value and 0x8000 != 0) value or -0x10000 else value
    }

    private fun ByteArray.readSigned24Le(offset: Int): Int {
        val value = (this[offset].toInt() and 0xff) or
            ((this[offset + 1].toInt() and 0xff) shl 8) or
            ((this[offset + 2].toInt() and 0xff) shl 16)
        return if (value and 0x800000 != 0) value or -0x1000000 else value
    }

    private fun ByteArray.readIntLe(offset: Int): Int {
        return (this[offset].toInt() and 0xff) or
            ((this[offset + 1].toInt() and 0xff) shl 8) or
            ((this[offset + 2].toInt() and 0xff) shl 16) or
            ((this[offset + 3].toInt() and 0xff) shl 24)
    }

    const val MAX_SECONDS = 180
    private const val RIFF_HEADER_SIZE = 12
    private const val CHUNK_HEADER_SIZE = 8
    private const val OUTPUT_CHANNELS = 2
    private const val WAVE_FORMAT_PCM = 1
    private const val WAVE_FORMAT_IEEE_FLOAT = 3
    private const val WAVE_FORMAT_EXTENSIBLE = 0xFFFE
}
