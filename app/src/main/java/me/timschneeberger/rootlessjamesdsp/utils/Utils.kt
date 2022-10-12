package me.timschneeberger.rootlessjamesdsp.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import androidx.appcompat.view.ContextThemeWrapper
import me.timschneeberger.rootlessjamesdsp.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.*


fun <T> concatenate(vararg lists: List<T>): List<T> {
    return listOf(*lists).flatten()
}

@Suppress("DEPRECATION")
fun loadHtml(html: String): Spanned {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(html)
    }
}

fun prettyNumberFormat(input: Double): String {
    if( input == 0.0 ) return "0"

    val prefix = if( input < 0 ) "-" else ""
    val num = abs(input)

    // figure out what group of suffixes we are in and scale the number
    val pow = floor(log10(num) /3).roundToInt()
    val base = num / 10.0.pow(pow * 3)

    // Using consistent rounding behavior, always rounding down since you want
    // 999999999 to show as 999.99M and not 1B
    val roundedDown = floor(base*100) /100.0

    // Convert the number to a string with up to 1 decimal place
    var baseStr = BigDecimal(roundedDown)
        .setScale(1, RoundingMode.HALF_EVEN)
        .toString()

    // Drop trailing zeros, then drop any trailing '.' if present
    baseStr = baseStr.dropLastWhile { it == '0' }.dropLastWhile { it == '.' }

    val suffixes = listOf("","k","M","B","T")

    return when {
        pow < suffixes.size -> "$prefix$baseStr${suffixes[pow]}"
        else -> "${prefix}∞"
    }
}

@Suppress("UNCHECKED_CAST")
fun <T : Serializable> Bundle.getSerializableAs(key: String, clazz: Class<T>): T? {
    return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getSerializable(key, clazz)
    } else {
        this.getSerializable(key)
    }) as? T
}
