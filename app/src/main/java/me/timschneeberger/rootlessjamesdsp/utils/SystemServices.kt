package me.timschneeberger.rootlessjamesdsp.utils

import android.content.Context

object SystemServices
{
    inline fun <reified T> get(context: Context): T
    {
        return context.getSystemService(T::class.java) as T
    }
}