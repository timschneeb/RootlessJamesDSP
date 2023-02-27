package me.timschneeberger.rootlessjamesdsp.view

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.ViewNumberInputBoxBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class NumberInputBox @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.theme,
    defStyleRes: Int = 0,
) : LinearLayout(context, attrs) {

    private var onValueChangedListener: ((Float) -> Unit)? = null
    private val binding: ViewNumberInputBoxBinding
    private val df = DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH))

    init {
        df.maximumFractionDigits = 4
    }

    var customStepScale: ((Float /* current value */, Boolean /* increasing */) -> Float)? = null

    var precision: Int
        get() = df.maximumFractionDigits
        set(value) {
            df.maximumFractionDigits = value
            binding.input.setText(df.format(this.value))
        }
    var min: Float = Float.MIN_VALUE
        set(value) {
            field = value
            validateValue()
        }
    var max: Float = Float.MAX_VALUE
        set(value) {
            field = value
            validateValue()
        }
    var step: Float = 1f
    var value: Float
        set(newValue) {
            // Preview bug fix
            if(this.isInEditMode) {
                return
            }

            val str = df.format(newValue)
            binding.input.setText(str)
            onValueChangedListener?.invoke(newValue)
        }
        get() {
            return binding.input.text.toString().toFloatOrNull() ?: 0f
        }
    var suffixText: String = ""
        set(value) {
            field = value
            binding.inputLayout.suffixText = value
        }
    var helperText: String = ""
        set(value) {
            field = value
            binding.inputLayout.helperText = value
        }
    var helperTextEnabled: Boolean = false
        set(value) {
            field = value
            binding.inputLayout.isHelperTextEnabled = value
        }
    var hintText: String = ""
        set(value) {
            field = value
            binding.inputLayout.hint = value
        }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            if (s.toString().isNotEmpty()) {
                val input = s.toString().toFloatOrNull() ?: 0f
                val validated = validateNumber(input)
                if(validated != null)
                    value = validated

                onValueChangedListener?.invoke(value)
            }
        }
    }

    fun isCurrentValueValid(): Boolean {
        return (binding.input.text?.isNotBlank() ?: false) && validateNumber(value) == null
    }

    private fun validateValue(input: Float = value) {
        val validNumber = validateNumber(input)
        validNumber ?: return
        value = validNumber
    }

    private fun validateNumber(input: Float): Float? {
        if(input > max) {
            return max
        }
        else if(input < min) {
             return min
        }
        return null
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.NumberInputBox, defStyleAttr, defStyleRes)
        binding = ViewNumberInputBoxBinding.inflate(LayoutInflater.from(context), this, true)

        precision = a.getInteger(R.styleable.NumberInputBox_floatPrecision, precision)
        step = a.getFloat(R.styleable.NumberInputBox_step, 1f)
        value = a.getFloat(R.styleable.NumberInputBox_value, 0f)
        min = a.getFloat(R.styleable.NumberInputBox_android_min, min)
        max = a.getFloat(R.styleable.NumberInputBox_android_max, max)
        suffixText = a.getString(R.styleable.NumberInputBox_suffixText) ?: suffixText
        helperText = a.getString(R.styleable.NumberInputBox_helperText) ?: helperText
        helperTextEnabled = a.getBoolean(R.styleable.NumberInputBox_helperTextEnabled, helperTextEnabled)
        hintText = a.getString(R.styleable.NumberInputBox_hintText) ?: hintText

        a.recycle()

        binding.plus.setOnClickListener {
            val finalStep = if(customStepScale == null) step
                else customStepScale?.invoke(value, true) ?: value
            val newValue = (value + finalStep)

            value = validateNumber(newValue) ?: newValue
        }
        binding.minus.setOnClickListener {
            val finalStep = if(customStepScale == null) step
                else customStepScale?.invoke(value, true) ?: value
            val newValue = (value - finalStep)

            value = validateNumber(newValue) ?: newValue
        }
    }

    override fun onAttachedToWindow() {
        binding.input.addTextChangedListener(textWatcher)
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        binding.input.removeTextChangedListener(textWatcher)
        super.onDetachedFromWindow()
    }

    fun setOnValueChangedListener(listener: ((Float) -> Unit)?) {
        onValueChangedListener = listener
    }
}