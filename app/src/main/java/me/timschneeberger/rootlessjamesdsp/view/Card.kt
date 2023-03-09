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
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.ViewCardBinding
import me.timschneeberger.rootlessjamesdsp.utils.extensions.asHtml
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.dpToPx


class Card @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.theme,
    defStyleRes: Int = 0,
) : LinearLayout(context, attrs) {

    private var onButtonClickListener: (() -> Unit)? = null
    private var onCloseClickListener: (() -> Unit)? = null
    private val binding: ViewCardBinding

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

        updateForeground()
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

    fun setOnCloseClickListener(listener: (() -> Unit)?) {
        onCloseClickListener = listener
    }
}