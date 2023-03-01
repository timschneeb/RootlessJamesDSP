package me.timschneeberger.rootlessjamesdsp.utils

import android.content.Context

object SystemServices
{
    // TODO use reified inlined function and omit Class<T>
    fun <T> get(context: Context, cls: Class<T>): T
    {
        return context.getSystemService(cls) as T
    }
}