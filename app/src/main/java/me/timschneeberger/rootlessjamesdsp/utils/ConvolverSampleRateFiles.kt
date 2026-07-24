package me.timschneeberger.rootlessjamesdsp.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

/** Serialization and lookup for sample-rate-specific impulse response files. */
object ConvolverSampleRateFiles {

    fun decode(value: String): Map<Int, String> {
        if (value.isBlank()) return emptyMap()

        return try {
            Json.parseToJsonElement(value).jsonObject.entries.mapNotNull { (rate, path) ->
                val sampleRate = rate.toIntOrNull() ?: return@mapNotNull null
                val filePath = path.jsonPrimitive.content
                if (isSupportedSampleRate(sampleRate) && filePath.isNotBlank()) {
                    sampleRate to filePath
                } else {
                    null
                }
            }.toMap()
        } catch (exception: Exception) {
            Timber.w(exception, "Invalid convolver sample-rate file mapping")
            emptyMap()
        }
    }

    fun encode(files: Map<Int, String>): String {
        return JsonObject(
            files
                .filter { (rate, path) ->
                    isSupportedSampleRate(rate) && path.isNotBlank()
                }
                .toSortedMap()
                .mapKeys { it.key.toString() }
                .mapValues { JsonPrimitive(it.value) }
        ).toString()
    }

    fun resolve(value: String, sampleRate: Int, fallbackFile: String): String {
        return decode(value)[sampleRate] ?: fallbackFile
    }

    fun formatKilohertz(sampleRate: Int): String {
        val whole = sampleRate / 1_000
        val fraction = (sampleRate % 1_000)
            .toString()
            .padStart(3, '0')
            .trimEnd('0')
        return if (fraction.isEmpty()) whole.toString() else "$whole.$fraction"
    }

    fun isSupportedSampleRate(sampleRate: Int): Boolean =
        sampleRate in MIN_SAMPLE_RATE..MAX_SAMPLE_RATE

    private const val MIN_SAMPLE_RATE = 1
    private const val MAX_SAMPLE_RATE = 192_000
}
