package me.timschneeberger.rootlessjamesdsp.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import com.google.android.material.checkbox.MaterialCheckBox
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.ViewCardBinding
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.dpToPx
import me.timschneeberger.rootlessjamesdsp.utils.extensions.asHtml


class Card @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.theme,
    defStyleRes: Int = 0,
) : LinearLayout(context, attrs) {

    private var onButtonClickListener: (() -> Unit)? = null
    private var onCheckChangedListener: ((Boolean) -> Unit)? = null
    private var onCloseClickListener: (() -> Unit)? = null
    private var onClickListener: (() -> Unit)? = null
    private val binding: ViewCardBinding

    var checkboxVisible: Boolean = true
        set(value) {
            field = value
            binding.closeButtonLayout.isVisible = value
            binding.checkbox.isVisible = value
            binding.close.isVisible = false
        }
    var checkboxIsChecked: Boolean
        get() = binding.checkbox.isChecked
        set(value) { binding.checkbox.isChecked = value }
    var buttonEnabled: Boolean = true
        set(value) {
            field = value
            binding.button.isEnabled = value
        }
    var buttonText: String? = null
        set(value) {
            field = value
            binding.button.isVisible = value != null
            if(value != null) {
                binding.button.text = value
            }
        }
    var closeButtonVisible: Boolean = false
        set(value) {
            field = value
            binding.closeButtonLayout.isVisible = value
            binding.close.isVisible = value
            binding.checkbox.isVisible = false
        }
    var titleText: String? = null
        set(value) {
            field = value
            binding.title.isVisible = value != null
            if(value != null) {
                binding.title.text = value.asHtml()
            }
        }
    var bodyText: String? = null
        set(value) {
            field = value
            binding.text.text = value?.asHtml() ?: ""
        }
    @DrawableRes var iconSrc: Int = 0
        set(value) {
            field = value
            binding.icon.isVisible = value != 0
            if(value != 0) {
                binding.icon.setImageResource(value)
            }
        }
    var iconTint: ColorStateList? = null
        set(value) {
            field = value
            binding.icon.imageTintList = value
        }
    var cardBackground: Int? = null
        set(value) {
            field = value
            value?.let {
                if(it != 0) {
                    binding.root.setCardBackgroundColor(it)
                }
            }
        }

    override fun setClickable(clickable: Boolean) {
        updateForeground()
        super.setClickable(clickable)
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.Card, defStyleAttr, defStyleRes)
        binding = ViewCardBinding.inflate(LayoutInflater.from(context), this, true)
        cardBackground = a.getColor(R.styleable.Card_cardBackground, 0)
        titleText = a.getString(R.styleable.Card_titleText)
        bodyText = a.getString(R.styleable.Card_bodyText)
        closeButtonVisible = a.getBoolean(R.styleable.Card_closeButtonVisible, false)
        buttonText = a.getString(R.styleable.Card_buttonText)
        buttonEnabled = a.getBoolean(R.styleable.Card_buttonEnabled, true)
        checkboxVisible = a.getBoolean(R.styleable.Card_checkboxVisible, false)
        iconSrc = a.getResourceId(R.styleable.Card_iconSrc, 0)
        iconTint = a.getColorStateList(R.styleable.Card_iconTint)

        val iconCentered = a.getBoolean(R.styleable.Card_iconCentered, false)
        val iconParams = LayoutParams(binding.icon.layoutParams)
        iconParams.setMargins(0, 0, 14.dpToPx, 0)
        iconParams.gravity = if(iconCentered) Gravity.CENTER_VERTICAL else Gravity.TOP
        binding.icon.layoutParams = iconParams

        val margin = a.getDimensionPixelSize(R.styleable.Card_cardMargin, 0)
        val rootParams = LayoutParams(binding.root.layoutParams)
        rootParams.setMargins(margin)
        binding.root.layoutParams = rootParams

        a.recycle()

        binding.button.setOnClickListener {
            onButtonClickListener?.invoke()
        }

        binding.close.setOnClickListener {
            onCloseClickListener?.invoke()
        }

        binding.root.setOnClickListener {
            onClickListener?.invoke()
            binding.checkbox.isChecked = !binding.checkbox.isChecked
        }

        updateForeground()
    }

    override fun onAttachedToWindow() {
        binding.checkbox.addOnCheckedStateChangedListener { _, state ->
            onCheckChangedListener?.invoke(state == MaterialCheckBox.STATE_CHECKED)
        }
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        binding.checkbox.clearOnCheckedStateChangedListeners()
        super.onDetachedFromWindow()
    }

    private fun updateForeground() {
        binding.root.foreground = if(isClickable) {
            val value = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, value, true)
            ContextCompat.getDrawable(context, value.resourceId)
        } else {
            AppCompatResources.getDrawable(context, android.R.color.transparent)
        }
    }

    fun setOnButtonClickListener(listener: (() -> Unit)?) {
        onButtonClickListener = listener
    }

    fun setOnCheckChangedListener(listener: ((Boolean) -> Unit)?) {
        onCheckChangedListener = listener
    }

    fun setOnCloseClickListener(listener: (() -> Unit)?) {
        onCloseClickListener = listener
    }

    fun setOnRootClickListener(listener: (() -> Unit)?) {
        onClickListener = listener
    }
}