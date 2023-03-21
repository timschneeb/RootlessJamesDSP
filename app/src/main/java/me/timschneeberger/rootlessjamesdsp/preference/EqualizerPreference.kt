package me.timschneeberger.rootlessjamesdsp.preference

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import androidx.preference.PreferenceViewHolder
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.PreferenceEqualizerBinding
import me.timschneeberger.rootlessjamesdsp.view.EqualizerSurface

class EqualizerPreference : DialogPreference {

    private var equalizerView: EqualizerSurface? = null
    var initialValue: String = ""

    var entries = arrayOf<CharSequence>()
    var entryValues = arrayOf<CharSequence>()

    constructor(
        context: Context, attrs: AttributeSet?,
        defStyleAttr: Int
    ) : this(context, attrs, defStyleAttr, 0)

    constructor(
        context: Context, attrs: AttributeSet?
    ) : this(context, attrs, androidx.preference.R.attr.preferenceStyle)

    constructor(
        context: Context
    ) : this(context, null)

    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        layoutResource = R.layout.preference_equalizer
        dialogLayoutResource = R.layout.preference_equalizer_dialog

        this.positiveButtonText = context.getString(android.R.string.ok)
        this.negativeButtonText = context.getString(android.R.string.cancel)

        val a = context.obtainStyledAttributes(
            attrs, R.styleable.EqualizerPreference, defStyleAttr, defStyleRes
        )

        entries = a.getTextArray(R.styleable.EqualizerPreference_android_entries)
        entryValues = a.getTextArray(R.styleable.EqualizerPreference_android_entryValues)
        a.recycle()
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        initialValue = getPersistedString(defaultValue as? String ?: "")
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getString(index).toString()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        equalizerView = PreferenceEqualizerBinding.bind(holder.itemView).layoutEqualizer
        setEqualizerViewValues(initialValue)
    }

    fun updateFromPreferences() {
        initialValue = getPersistedString(initialValue)
        setEqualizerViewValues(initialValue)
    }

    private fun setEqualizerViewValues(value: String) {
        value
            .split(";")
            .drop(15)
            .dropLastWhile(String::isEmpty)
            .map(String::toDoubleOrNull)
            .forEachIndexed { index, s -> equalizerView?.setBand(index, s ?: 0.0) }
    }
}