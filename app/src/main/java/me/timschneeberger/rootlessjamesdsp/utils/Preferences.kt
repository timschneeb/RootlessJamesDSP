package me.timschneeberger.rootlessjamesdsp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.flavor.CrashlyticsImpl

class Preferences(val context: Context) {

    inner class App : AbstractPreferences(context) {
        override fun namespace() = Constants.PREF_APP
    }

    inner class Var : AbstractPreferences(context) {
        override fun namespace() = Constants.PREF_VAR
    }

    abstract class AbstractPreferences(val context: Context) {
        abstract fun namespace(): String

        val prefs: SharedPreferences by lazy {
            context.getSharedPreferences(namespace(), Context.MODE_PRIVATE)
        }

        val defaultCache: HashMap<String, Any> = hashMapOf()

        /**
         * @remarks This function takes a StringRes pointing to a preference key.
         *          There MUST be a default value with the same key name in defaults.xml,
         *          otherwise an exception will be thrown when loading the default value.
         *
         * @return Returns the current value of the preference or the default value if none is set
         */
        inline fun <reified T> get(@StringRes nameRes: Int): T {

            val key = context.getString(nameRes)
            val default = if(defaultCache.containsKey(key)) {
                defaultCache[key] as T
            }
            else {
                @SuppressLint("DiscouragedApi")
                val defaultRes = context.resources.getIdentifier(
                    key,
                    when(T::class)
                    {
                        Boolean::class -> "bool"
                        String::class -> "string"
                        Int::class -> "integer"
                        Float::class -> "float"
                        else -> throw IllegalArgumentException("Unknown type")
                    },
                    BuildConfig.APPLICATION_ID
                )
                (when(T::class) {
                    Boolean::class -> context.resources.getBoolean(defaultRes)
                    String::class -> context.resources.getString(defaultRes)
                    Int::class -> context.resources.getInteger(defaultRes)
                    Float::class -> context.resources.getFloat(defaultRes)
                    else -> throw IllegalArgumentException("Unknown type")
                } as T).also {
                    defaultCache[key] = it as Any
                }
            }

            return when(T::class) {
                Boolean::class -> prefs.getBoolean(key, default as Boolean) as T
                String::class -> prefs.getString(key, default as String) as T
                Int::class -> prefs.getInt(key, default as Int) as T
                Float::class -> prefs.getFloat(key, default as Float) as T
                else -> throw IllegalArgumentException("Unknown type")
            }.also {
                CrashlyticsImpl.setCustomKey("${namespace()}_$key", it.toString())
            }
        }

        @SuppressLint("ApplySharedPref")
        inline fun <reified T> set(@StringRes nameRes: Int, value: T, async: Boolean = true) {
            val key = context.getString(nameRes)
            val edit = prefs.edit()
            CrashlyticsImpl.setCustomKey("${namespace()}_$key", value.toString())

            when(T::class) {
                Boolean::class -> edit.putBoolean(key, value as Boolean)
                String::class -> edit.putString(key, value as String)
                Int::class -> edit.putInt(key, value as Int)
                Float::class -> edit.putFloat(key, value as Float)
                else -> throw IllegalArgumentException("Unknown type")
            }.run {
                if(async)
                    apply()
                else
                    commit()
            }
        }

        open fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            prefs.registerOnSharedPreferenceChangeListener(listener)
        }

        open fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
}