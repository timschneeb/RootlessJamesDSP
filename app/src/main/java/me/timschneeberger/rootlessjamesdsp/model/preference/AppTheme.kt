package me.timschneeberger.rootlessjamesdsp.model.preference

import me.timschneeberger.rootlessjamesdsp.R

enum class AppTheme(val titleResId: Int?) {
    DEFAULT(R.string.theme_default),
    MONET(R.string.theme_monet),
    GREEN_APPLE(R.string.theme_greenapple),
    HONEY(R.string.theme_honey),
    STRAWBERRY_DAIQUIRI(R.string.theme_strawberrydaiquiri),
    TEALTURQUOISE(R.string.theme_tealturquoise),
    TIDAL_WAVE(R.string.theme_tidalwave),
    YINYANG(R.string.theme_yinyang),
    YOTSUBA(R.string.theme_yotsuba);

    companion object {
        fun fromInt(titleResId: Int) = values().first { it.titleResId == titleResId }
    }
}