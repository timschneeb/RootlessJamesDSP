package me.timschneeberger.rootlessjamesdsp.utils.preferences

import androidx.preference.PreferenceDataStore

class NonPersistentDatastore : PreferenceDataStore() {
    private var preferenceChangedListener: OnPreferenceChanged? = null

    override fun putFloat(key: String?, value: Float) {
        key ?: return
        this.preferenceChangedListener?.onFloatPreferenceChanged(key, value)
    }

    override fun putString(key: String?, value: String?) {}

    fun setOnPreferenceChanged(preferenceChangedListener: OnPreferenceChanged) {
        this.preferenceChangedListener = preferenceChangedListener
    }

    interface OnPreferenceChanged {
        fun onFloatPreferenceChanged(key: String, value: Float)
    }
}