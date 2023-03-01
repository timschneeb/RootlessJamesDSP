package me.timschneeberger.rootlessjamesdsp.interop

import android.content.Context
import androidx.annotation.StringRes
import me.timschneeberger.rootlessjamesdsp.flavor.CrashlyticsImpl
import kotlin.reflect.KClass

class PreferenceCache(val context: Context) {
    val changedNamespaces = ArrayList<String>()
    val cache: HashMap<String, Any> = hashMapOf()
    var selectedNamespace: String? = null

    fun select(namespace: String) {
        selectedNamespace = namespace
    }

    fun clear() {
        try {
            cache.clear()
        }
        catch (_: Exception) {}
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(@StringRes nameRes: Int, default: T, type: KClass<T>): T {
        if(selectedNamespace == null)
            throw IllegalStateException("No active namespace selected")

        val name = context.getString(nameRes)
        @Suppress("DEPRECATION")
        val prefs = context.getSharedPreferences(selectedNamespace, Context.MODE_MULTI_PROCESS)
        val current: T = when(type) {
            Boolean::class -> prefs.getBoolean(name, default as Boolean) as T
            String::class -> prefs.getString(name, default as String) as T
            Int::class -> prefs.getInt(name, default as Int) as T
            Float::class -> prefs.getFloat(name, default as Float) as T
            else -> throw IllegalArgumentException("Unknown type")
        }

        val unchanged = cache.containsKey(name) && cache[name] == current
        if(!unchanged && !changedNamespaces.contains(selectedNamespace)) {
            selectedNamespace?.let {
                changedNamespaces.add(it)
            }
        }

        CrashlyticsImpl.setCustomKey("dsp_$name", current.toString())
        cache[name] = current as Any
        return current
    }

    inline fun <reified T : Any> get(@StringRes nameRes: Int, default: T) =
        get(nameRes, default, T::class)

    fun markChangesAsCommitted() {
        changedNamespaces.clear()
    }
}