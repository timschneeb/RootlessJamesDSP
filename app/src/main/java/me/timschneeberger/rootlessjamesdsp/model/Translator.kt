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
    val username: String,
    val picture: String,
    val translated: Int,
    val approved: Int,
    val languages: List<Language>
) {
    @Serializable
    data class Language(val id: String, val name: String)

    companion object {
        fun readLanguageMap(context: Context): Map<String, List<Translator>> {
            val languageMap = mutableMapOf<String, MutableList<Translator>>()
            readAll(context)
                .sortedByDescending { it.translated }
                .forEach { tl ->
                    // At least 8 words
                    if (tl.translated < 8)
                        return@forEach
                    tl.languages.forEach next@{ langObj ->
                        val lang = langObj.id

                        // Fix: only display my name for German, not all languages
                        if (tl.username == "ThePBone" && lang != "de")
                            return@next

                        if (languageMap[lang] == null)
                            languageMap[lang] = mutableListOf(tl)
                        else
                            languageMap[lang]!!.add(tl)
                    }
                }
            return languageMap
        }

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
