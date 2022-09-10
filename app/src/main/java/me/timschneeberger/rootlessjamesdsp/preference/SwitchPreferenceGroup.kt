package me.timschneeberger.rootlessjamesdsp.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.TransitionDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceViewHolder
import androidx.preference.children
import com.google.android.material.materialswitch.MaterialSwitch
import me.timschneeberger.rootlessjamesdsp.R
import timber.log.Timber


@SuppressLint("PrivateResource")
class SwitchPreferenceGroup(context: Context, attrs: AttributeSet) : PreferenceGroup(
    context, attrs, androidx.preference.R.attr.preferenceStyle,
    androidx.preference.R.style.Preference_SwitchPreferenceCompat_Material
) {
    private var childrenVisible = false
    private var switch: MaterialSwitch? = null
    private var itemView: View? = null
    private var state = false

    init {
        androidx.preference.R.layout.preference_material
        layoutResource = R.layout.preference_switchgroup
        widgetLayoutResource = R.layout.preference_materialswitch
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        setValue(getPersistedBoolean((defaultValue as? Boolean) ?: false))
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any = a.getBoolean(index, false)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        itemView = holder.itemView

        val transition = AppCompatResources.getDrawable(context, R.drawable.transition_cardheader_background)
        itemView?.background = transition
        setChildrenVisibility(state)
        animateHeaderState(state)

        switch = (holder.findViewById(R.id.switchWidget) as MaterialSwitch).apply {
            // Apply initial state
            isChecked = state

            setOnCheckedChangeListener { _, isChecked ->
                setValueInternal(isChecked, false)
            }
        }

        holder.itemView.apply {
            setOnClickListener {
                switch?.toggle()
            }
        }
    }

    override fun onPrepareAddPreference(preference: Preference): Boolean {
        preference.isVisible = childrenVisible
        return super.onPrepareAddPreference(preference)
    }

    fun setValue(value: Boolean) {
        setValueInternal(value, true)
    }

    private fun setValueInternal(value: Boolean, notifyChanged: Boolean) {
        setChildrenVisibility(value)
        animateHeaderState(value)

        if (state != value) {
            state = value
            persistBoolean(state)
            if (notifyChanged) {
                notifyChanged()
            }
        }
    }

    private fun animateHeaderState(selected: Boolean) {
        val transition = itemView?.background as? TransitionDrawable
        transition ?: return // View holder not yet bound

        if(selected) {
            transition.startTransition(TRANSITION_DURATION)
        }
        else if(!selected) {
            transition.startTransition(0) // ensure animation has been played
            transition.reverseTransition(TRANSITION_DURATION)
        }
    }

    private fun setChildrenVisibility(visible: Boolean) {
        children.forEach { it.isVisible = visible }
    }

    companion object {
        const val TRANSITION_DURATION = 100
    }
}