package me.timschneeberger.rootlessjamesdsp.utils

import android.content.Context

object SystemServices
{
    fun <T> get(context: Context, cls: Class<T>): T
    {
        return context.getSystemService(cls) as T
    }
}