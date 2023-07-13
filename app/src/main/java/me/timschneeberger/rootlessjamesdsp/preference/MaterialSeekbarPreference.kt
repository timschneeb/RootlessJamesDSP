package me.timschneeberger.rootlessjamesdsp.preference
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import com.google.android.material.slider.Slider
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showInputAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import timber.log.Timber
import java.math.BigDecimal
import java.math.MathContext
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

// TODO stepValue is broken in recyclerview!!!
class MaterialSeekbarPreference : Preference {
    var mSeekBarValue/* synthetic access */ = 0f
    var mMin/* synthetic access */ = 0f
    private var mMax = 0f
    private var mSeekBarIncrement = 0f
    var mTrackingTouch/* synthetic access */ = false
    lateinit var mSeekBar: /* synthetic access */Slider
    private var mSeekBarValueTextView: TextView? = null

    var mUnit: String = ""
    var mPrecision: Int = 2
    var mLabelMinWidth: Int = 0

    // Whether the SeekBar should respond to the left/right keys
    var mAdjustable/* synthetic access */ = false

    // Whether to show the SeekBar value TextView next to the bar
    private var mShowSeekBarValue = false

    // Whether the SeekBarPreference should continuously save the Seekbar value while it is being
    // dragged.
    var mUpdatesContinuously/* synthetic access */ = false

    var valueLabelOverride: ((Float) -> String)? = null

    /**
     * Listener reacting to the [SeekBar] changing value by the user
     */
    private val mSeekBarChangeListener =
        Slider.OnChangeListener { slider, value, fromUser ->
            if (fromUser && mUpdatesContinuously || !mTrackingTouch) {
                syncValueInternal(slider)
            } else {
                // We always want to update the text while the seekbar is being dragged
                updateLabelValue(value)
            }
        }
    private val mSeekBarTouchListener = object :  Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(seekBar: Slider) {
            mTrackingTouch = true
        }

        override fun onStopTrackingTouch(seekBar: Slider) {
            mTrackingTouch = false
            if (seekBar.value != mSeekBarValue) {
                syncValueInternal(seekBar)
            }
        }
    }

    /**
     * Listener reacting to the user pressing DPAD left/right keys if `adjustable` attribute is set to true; it transfers the key presses to the [SeekBar]
     * to be handled accordingly.
     */
    private val mSeekBarKeyListener =
        View.OnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) {
                return@OnKeyListener false
            }
            if (!mAdjustable && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                        || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
            ) {
                // Right or left keys are pressed when in non-adjustable mode; Skip the keys.
                return@OnKeyListener false
            }

            // We don't want to propagate the click keys down to the SeekBar view since it will
            // create the ripple effect for the thumb.
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                return@OnKeyListener false
            }
            mSeekBar.onKeyDown(keyCode, event)
        }

    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        layoutResource = R.layout.preference_materialslider

        val a = context.obtainStyledAttributes(
            attrs, R.styleable.MaterialSeekbarPreference, defStyleAttr, defStyleRes
        )

        // The ordering of these two statements are important. If we want to set max first, we need
        // to perform the same steps by changing min/max to max/min as following:
        // mMax = a.getInt(...) and setMin(...).
        mMin = a.getFloat(R.styleable.MaterialSeekbarPreference_minValue, 0f)
        setMax(a.getFloat(R.styleable.MaterialSeekbarPreference_maxValue, 100f))
        setSeekBarIncrement(a.getFloat(R.styleable.MaterialSeekbarPreference_seekBarIncrement, 0f))
        mShowSeekBarValue = a.getBoolean(R.styleable.MaterialSeekbarPreference_showSeekBarValue, false)
        mUpdatesContinuously = a.getBoolean(
            R.styleable.MaterialSeekbarPreference_updatesContinuously,
            false
        )

        mLabelMinWidth = a.getDimensionPixelSize(R.styleable.MaterialSeekbarPreference_labelMinWidth, 0)
        mUnit = a.getString(R.styleable.MaterialSeekbarPreference_unit) ?: ""
        mPrecision = a.getInt(R.styleable.MaterialSeekbarPreference_precision, 2)

        a.recycle()
    }

    constructor(
        context: Context, attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : this(context, attrs, defStyleAttr, 0)

    constructor(
        context: Context, attrs: AttributeSet?,
    ) : this(context, attrs, R.attr.seekBarStyle)

    constructor(
        context: Context,
    ) : this(context, null)

    private fun validateValue(value: Float): Float {
        if (mSeekBarIncrement > 0 && !valueLandsOnTick(value)) {
            val newValue = mSeekBarIncrement * ((value / mSeekBarIncrement).roundToInt())
            Timber.w("setValueInternal: value corrected $value to $newValue")
            return newValue
        }
        return value
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        mSeekBar = holder.findViewById(R.id.seekbar) as Slider
        mSeekBarValueTextView = holder.findViewById(R.id.seekbar_value) as TextView
        holder.itemView.setOnKeyListener(mSeekBarKeyListener)

        if(mLabelMinWidth > 0) {
            mSeekBarValueTextView!!.minWidth = mLabelMinWidth
        }

        if (mShowSeekBarValue) {
            mSeekBarValueTextView!!.visibility = View.VISIBLE
        } else {
            mSeekBarValueTextView!!.visibility = View.GONE
            mSeekBarValueTextView = null
        }

        mSeekBar.clearOnChangeListeners()
        mSeekBar.clearOnSliderTouchListeners()
        mSeekBar.addOnChangeListener(mSeekBarChangeListener)
        mSeekBar.addOnSliderTouchListener(mSeekBarTouchListener)
        mSeekBar.valueFrom = mMin
        mSeekBar.valueTo = mMax
        // Ignore: If the increment is not zero, use that. Otherwise, use the default mKeyProgressIncrement
        // in AbsSeekBar when it's zero. This default increment value is set by AbsSeekBar
        // after calling setMax. That's why it's important to call setKeyProgressIncrement after
        // calling setMax() since setMax() can change the increment value.
        mSeekBar.stepSize = mSeekBarIncrement

        mSeekBar.value = validateValue(mSeekBarValue)
        updateLabelValue(mSeekBarValue)
        mSeekBar.isEnabled = isEnabled

        this.setOnPreferenceClickListener {
            context.showInputAlert(
                LayoutInflater.from(context), 
                context.getString(R.string.slider_dialog_title),
                title?.toString(),
                "%.${mPrecision}f".format(Locale.ROOT, getValue()),
                true,
                mUnit
            ) {
                it ?: return@showInputAlert
                try {
                    if(mSeekBar.stepSize <= 0 || valueLandsOnTick(it.toFloat())) {
                        setValue(it.toFloat())
                    }
                    else {
                        context.toast(
                            context.getString(R.string.slider_dialog_step_error, mSeekBar.stepSize.roundToInt()),
                            false
                        )
                    }
                }
                catch (ex: Exception) {
                    Timber.e("Failed to parse number input")
                    Timber.d(ex)
                    context.toast(
                        context.getString(R.string.slider_dialog_format_error),
                        false
                    )
                }
            }
            true
        }
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        setValue(getPersistedFloat((defaultValue as? Float ?: 0f)))
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return validateValue(a.getFloat(index, 0f))
    }

    /**
     * Gets the lower bound set on the [SeekBar].
     *
     * @return The lower bound set
     */
    fun getMin(): Float {
        return mMin
    }

    /**
     * Sets the lower bound on the [SeekBar].
     *
     * @param _min The lower bound to set
     */
    fun setMin(_min: Float) {
        var min = _min
        if (min > mMax) {
            min = mMax
        }
        if (min != mMin) {
            mMin = min
            notifyChanged()
        }
    }

    /**
     * Returns the amount of increment change via each arrow key click. This value is derived from
     * user's specified increment value if it's not zero. Otherwise, the default value is picked
     * from the default mKeyProgressIncrement value in [android.widget.AbsSeekBar].
     *
     * @return The amount of increment on the [SeekBar] performed after each user's arrow
     * key press
     */
    fun getSeekBarIncrement(): Float {
        return mSeekBarIncrement
    }

    /**
     * Sets the increment amount on the [SeekBar] for each arrow key press.
     *
     * @param seekBarIncrement The amount to increment or decrement when the user presses an
     * arrow key.
     */
    fun setSeekBarIncrement(seekBarIncrement: Float) {
        if (seekBarIncrement != mSeekBarIncrement) {
            mSeekBarIncrement = min(mMax - mMin, abs(seekBarIncrement))
            notifyChanged()
        }
    }

    /**
     * Gets the upper bound set on the [SeekBar].
     *
     * @return The upper bound set
     */
    fun getMax(): Float {
        return mMax
    }

    /**
     * Sets the upper bound on the [SeekBar].
     *
     * @param _max The upper bound to set
     */
    fun setMax(_max: Float) {
        var max = _max
        if (max < mMin) {
            max = mMin
        }
        if (max != mMax) {
            mMax = max
            notifyChanged()
        }
    }

    /**
     * Gets whether the [SeekBar] should respond to the left/right keys.
     *
     * @return Whether the [SeekBar] should respond to the left/right keys
     */
    fun isAdjustable(): Boolean {
        return mAdjustable
    }

    /**
     * Sets whether the [SeekBar] should respond to the left/right keys.
     *
     * @param adjustable Whether the [SeekBar] should respond to the left/right keys
     */
    fun setAdjustable(adjustable: Boolean) {
        mAdjustable = adjustable
    }

    /**
     * Gets whether the [SeekBarPreference] should continuously save the [SeekBar] value
     * while it is being dragged. Note that when the value is true,
     * [Preference.OnPreferenceChangeListener] will be called continuously as well.
     *
     * @return Whether the [SeekBarPreference] should continuously save the [SeekBar]
     * value while it is being dragged
     * @see .setUpdatesContinuously
     */
    fun getUpdatesContinuously(): Boolean {
        return mUpdatesContinuously
    }

    /**
     * Sets whether the [SeekBarPreference] should continuously save the [SeekBar] value
     * while it is being dragged.
     *
     * @param updatesContinuously Whether the [SeekBarPreference] should continuously save
     * the [SeekBar] value while it is being dragged
     * @see .getUpdatesContinuously
     */
    fun setUpdatesContinuously(updatesContinuously: Boolean) {
        mUpdatesContinuously = updatesContinuously
    }

    /**
     * Gets whether the current [SeekBar] value is displayed to the user.
     *
     * @return Whether the current [SeekBar] value is displayed to the user
     * @see .setShowSeekBarValue
     */
    fun getShowSeekBarValue(): Boolean {
        return mShowSeekBarValue
    }

    /**
     * Sets whether the current [SeekBar] value is displayed to the user.
     *
     * @param showSeekBarValue Whether the current [SeekBar] value is displayed to the user
     * @see .getShowSeekBarValue
     */
    fun setShowSeekBarValue(showSeekBarValue: Boolean) {
        mShowSeekBarValue = showSeekBarValue
        notifyChanged()
    }

    private fun setValueInternal(_seekBarValue: Float, notifyChanged: Boolean) {
        var seekBarValue = _seekBarValue
        if (seekBarValue < mMin) {
            seekBarValue = mMin
        }
        if (seekBarValue > mMax) {
            seekBarValue = mMax
        }

        seekBarValue = validateValue(seekBarValue)
        if (mSeekBarIncrement > 0 && !valueLandsOnTick(seekBarValue)) {
            seekBarValue = mSeekBarIncrement * ((seekBarValue / mSeekBarIncrement).roundToInt())
            Timber.w("setValueInternal: value corrected $_seekBarValue to $seekBarValue")
        }

        if (seekBarValue != mSeekBarValue) {
            mSeekBarValue = seekBarValue
            updateLabelValue(mSeekBarValue)
            persistFloat(seekBarValue)
            if (notifyChanged) {
                notifyChanged()
            }
        }
    }

    /**
     * Gets the current progress of the [SeekBar].
     *
     * @return The current progress of the [SeekBar]
     */
    fun getValue(): Float {
        return mSeekBarValue
    }

    /**
     * Sets the current progress of the [SeekBar].
     *
     * @param seekBarValue The current progress of the [SeekBar]
     */
    fun setValue(seekBarValue: Float) {
        setValueInternal(seekBarValue, true)
    }

    /**
     * Persist the [SeekBar]'s SeekBar value if callChangeListener returns true, otherwise
     * set the [SeekBar]'s value to the stored value.
     */
    fun  /* synthetic access */syncValueInternal(seekBar: Slider) {
        val seekBarValue = seekBar.value
        if (seekBarValue != mSeekBarValue) {
            if (callChangeListener(seekBarValue)) {
                setValueInternal(seekBarValue, false)
            } else {
                seekBar.value = validateValue(mSeekBarValue)
                updateLabelValue(mSeekBarValue)
            }
        }
    }

    /**
     * Attempts to update the TextView label that displays the current value.
     *
     * @param value the value to display next to the [SeekBar]
     */
    @SuppressLint("SetTextI18n")
    fun  /* synthetic access */updateLabelValue(value: Float) {
        if (mSeekBarValueTextView != null) {

            if(valueLabelOverride == null)
            {
                mSeekBarValueTextView!!.text = "%.${mPrecision}f${mUnit}".format(Locale.ROOT, value)
            }
            else
            {
                mSeekBarValueTextView!!.text = valueLabelOverride!!(value)
            }
        }
    }

    private fun valueLandsOnTick(value: Float): Boolean {
        // Check that the value is a multiple of stepSize given the offset of valueFrom.
        return isMultipleOfStepSize(value - mMin)
    }

    private fun isMultipleOfStepSize(value: Float): Boolean {
        // We're using BigDecimal here to avoid floating point rounding errors.
        val result = BigDecimal(value.toString())
            .divide(BigDecimal(mSeekBarIncrement.toString()), MathContext.DECIMAL64)
            .toDouble()

        // If the result is a whole number, it means the value is a multiple of stepSize.
        return abs(result.roundToInt() - result) < 1.0E-4
    }
}