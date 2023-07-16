package me.timschneeberger.rootlessjamesdsp.preference

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.PreferenceGraphicEqualizerBinding
import me.timschneeberger.rootlessjamesdsp.model.GraphicEqNodeList
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import timber.log.Timber
import java.util.MissingFormatArgumentException

class GraphicEqualizerPreference : Preference {

    private var binding: PreferenceGraphicEqualizerBinding? = null
    private var initialValue: String = ""

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateFromPreferences()
        }
    }

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
    }

    override fun onAttached() {
        context.registerLocalReceiver(broadcastReceiver, IntentFilter(Constants.ACTION_GRAPHIC_EQ_CHANGED))
        super.onAttached()
    }

    override fun onDetached() {
        context.unregisterLocalReceiver(broadcastReceiver)
        super.onDetached()
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        initialValue = getPersistedString(defaultValue as? String ?: "")
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getString(index).toString()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        binding = PreferenceGraphicEqualizerBinding.bind(holder.itemView)
        setEqualizerViewValues(initialValue)
    }

    fun updateFromPreferences() {
        initialValue = getPersistedString(initialValue)
        setEqualizerViewValues(initialValue)
    }

    private fun setEqualizerViewValues(value: String) {
        val nodes = GraphicEqNodeList()
        nodes.deserialize(value)

        binding?.layoutEqualizer?.setNodes(nodes)
        try {
            binding?.nodeCount?.text =
                context.resources.getQuantityString(R.plurals.nodes, nodes.size, nodes.size)
        }
        catch (ex: MissingFormatArgumentException) {
            Timber.e(ex)
            binding?.nodeCount?.text = context.getString(R.string.geq_node_editor)
        }
    }
}