package me.timschneeberger.rootlessjamesdsp.preference

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import androidx.preference.PreferenceViewHolder
import me.timschneeberger.rootlessjamesdsp.view.EqualizerSurface
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.PreferenceEqualizerBinding
import me.timschneeberger.rootlessjamesdsp.databinding.PreferenceGraphicEqualizerBinding
import me.timschneeberger.rootlessjamesdsp.view.GraphicEqualizerSurface

class GraphicEqualizerPreference : DialogPreference {

    var equalizerView: GraphicEqualizerSurface? = null
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
        layoutResource = R.layout.preference_graphic_equalizer
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

    override fun onSetInitialValue(_defaultValue: Any?) {
        var defaultValue = _defaultValue
        if (defaultValue == null) {
            defaultValue = ""
        }

        initialValue = getPersistedString(defaultValue as? String)
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getString(index).toString()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        equalizerView = PreferenceGraphicEqualizerBinding.bind(holder.itemView).layoutEqualizer
        setEqualizerViewValues(initialValue)

        //setEqualizerViewValues("GraphicEQ: 16.1 2.39; 29.4 7.39; 52.1 7.91; 98.3 7.91; 160 6.7; 250 6.7; 400 5.47; 630 4.24; 1000 3.5; 1600 4.92; 2500 3.5; 5541.6 1.87; 6300 3.5; 7386.7 5.7; 16000 3.5")
        setEqualizerViewValues("GraphicEQ: 400 5.47;")
    }

    fun updateFromPreferences() {
        initialValue = getPersistedString(initialValue)
        setEqualizerViewValues(initialValue)
    }

    private fun setEqualizerViewValues(value: String) {
        val nodes = value
            .replace("GraphicEQ:", "")
            .split(";")
            .map { it.trim() }
            .filter(String::isNotBlank)
            .mapNotNull { s ->
                val pair = s.split(" ").filter(String::isNotBlank)
                val freq = pair.getOrNull(0)?.toDoubleOrNull()
                val gain = pair.getOrNull(1)?.toDoubleOrNull()

                if (freq == null || gain == null) {
                    null
                } else {
                    arrayOf(freq, gain)
                }
            }

        equalizerView?.setNodes(nodes.toTypedArray())
    }
}