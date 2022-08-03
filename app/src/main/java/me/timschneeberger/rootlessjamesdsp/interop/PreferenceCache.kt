package me.timschneeberger.rootlessjamesdsp.interop

import android.content.Context
import androidx.annotation.StringRes

class PreferenceCache(val context: Context) {
    val changedNamespaces = ArrayList<String>()
    val cache: HashMap<String, Any> = hashMapOf()
    var selectedNamespace: String? = null

    fun select(namespace: String) {
        selectedNamespace = namespace
    }

    inline fun <reified T> get(@StringRes nameRes: Int, default: T): T {
        if(selectedNamespace == null)
            throw IllegalStateException("No active namespace selected")

        val name = context.getString(nameRes)
        val prefs = context.getSharedPreferences(selectedNamespace, Context.MODE_PRIVATE)
        val current: T = when(T::class) {
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

        cache[name] = current as Any
        return current
    }

    fun markChangesAsCommitted() {
        changedNamespaces.clear()
    }
}