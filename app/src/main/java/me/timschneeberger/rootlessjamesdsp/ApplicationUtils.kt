package me.timschneeberger.rootlessjamesdsp

import android.content.Context
import android.util.Base64
import android.util.Log
import android.widget.Toast

// Very simple & naive checks to prevent App duplicators/multi account apps from functioning correctly
object ApplicationUtils {
    private const val PKGNAME_REF = "bWUudGltc2NobmVlYmVyZ2VyLnJvb3RsZXNzamFtZXNkc3A="
    private const val APPNAME_REF = "Um9vdGxlc3NKYW1lc0RTUA=="

    fun check(ctx: Context): Int {
        if(decode(PKGNAME_REF) != ctx.packageName) return 1;
        if(decode(APPNAME_REF) != ctx.getText(R.string.app_name)) return 2;
        return 0;
    }

    private fun decode(input: String): String {
        return String(Base64.decode(input, 0), Charsets.UTF_8)
    }
}