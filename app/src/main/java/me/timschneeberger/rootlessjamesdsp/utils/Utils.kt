package me.timschneeberger.rootlessjamesdsp.utils

import android.text.Html
import android.text.Spanned
import me.timschneeberger.rootlessjamesdsp.R

object Utils {
    fun <T> concatenate(vararg lists: List<T>): List<T> {
        return listOf(*lists).flatten()
    }

    fun loadHtml(html: String): Spanned {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(html)
        }
    }
}