package me.timschneeberger.rootlessjamesdsp.view

import android.animation.AnimatorInflater
import android.content.Context
import android.util.AttributeSet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import me.timschneeberger.rootlessjamesdsp.R

class FloatingToggleButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FloatingActionButton(context, attrs) {

    private var onToggledListener: OnToggledListener? = null
    private var onClickListener: OnToggleClickListener? = null

    var isToggled = false
        set(value) {
            field = value
            isSelected = value
            onToggledListener?.onToggled(value)
        }

    var toggleOnClick = true

    init {
        setOnClickListener {
            onClickListener?.onClick()
            if(toggleOnClick) {
                isToggled = !isToggled
            }
        }

        stateListAnimator = AnimatorInflater.loadStateListAnimator(context, com.google.android.material.R.animator.mtrl_extended_fab_state_list_animator)
        supportBackgroundTintList = context.getColorStateList(R.color.selector_floating_toggle_tint)
        drawable.setTintList(context.getColorStateList(R.color.selector_floating_toggle_foreground_tint))
    }

    fun setOnToggledListener(listener: OnToggledListener) {
        onToggledListener = listener
    }

    fun setOnToggleClickListener(listener: OnToggleClickListener) {
        onClickListener = listener
    }

    interface OnToggleClickListener {
        fun onClick()
    }

    interface OnToggledListener {
        fun onToggled(enabled: Boolean)
    }
}