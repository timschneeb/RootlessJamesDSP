package me.timschneeberger.rootlessjamesdsp.model

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.timschneeberger.rootlessjamesdsp.R
import timber.log.Timber

@Serializable
data class Translator(
    val id: String,
    val name: String,
    val user: String,
    val translated: Int,
    val approved: Int,
    val languages: List<String>
) {
    companion object {
        fun readAll(context: Context): List<Translator> {
            return try {
                Json.decodeFromString<List<Translator>>(
                    context.resources
                        .openRawResource(R.raw.translators)
                        .bufferedReader()
                        .use { it.readText() }
                )
            } catch (ex: Exception) {
                Timber.e(ex)
                listOf()
            }
        }
    }
}
