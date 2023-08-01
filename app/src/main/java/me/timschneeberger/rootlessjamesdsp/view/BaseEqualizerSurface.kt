package me.timschneeberger.rootlessjamesdsp.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.core.os.bundleOf
import me.timschneeberger.rootlessjamesdsp.utils.extensions.CompatExtensions.getParcelableAs
import me.timschneeberger.rootlessjamesdsp.utils.extensions.prettyNumberFormat
import timber.log.Timber
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln


abstract class BaseEqualizerSurface(
    context: Context?,
    attrs: AttributeSet?,
    val bandsNum: Int,
    val minHz: Double,
    val maxHz: Double,
    val minDb: Double,
    val maxDb: Double,
    val horizLineInterval: Float
) : View(context, attrs) {

    var areKnobsVisible = false
        set(value) {
            field = value
            if(this.isAttachedToWindow)
            {
                postInvalidate()
            }
        }

    private var mGridLines = Paint()
    private var mControlBarText = Paint()
    private var mFrequencyResponseBg = Paint()
    private var mFrequencyResponseHighlight = Paint()
    private var mControlBarKnob = Paint()
    private var mLevels = DoubleArray(bandsNum)
    private var mHeight = 0.0f
    private var mWidth = 0.0f

    protected var nPts = 128
    private var displayFreq = DoubleArray(nPts)
    private var response = FloatArray(nPts)
    private val precomputeCurveXAxis = MutableList(nPts) { 0.0f }
    private var precomputeFreqAxis = FloatArray(2)

    fun addElement(org: FloatArray, added: Float): FloatArray {
        val result = org.copyOf(org.size + 1)
        result[org.size] = added
        return result
    }

    init {
        for (i in 0 until nPts) displayFreq[i] = reverseProjectX(i / (nPts - 1).toDouble())
        for (i in 0 until nPts) precomputeCurveXAxis[i] = projectX(displayFreq[i])
        var freq = minHz
        while (freq < maxHz) {
            precomputeFreqAxis = addElement(precomputeFreqAxis, projectX(freq))
            freq += if (freq < 100) 10 else if (freq < 1000) 100 else if (freq < 10000) 1000 else 10000
        }

        mControlBarKnob.style = Paint.Style.FILL
        mControlBarKnob.isAntiAlias = true
        mControlBarKnob.color = getColor(android.R.attr.colorAccent)

        mGridLines.color = getColor(android.R.attr.colorControlHighlight)
        mGridLines.style = Paint.Style.STROKE
        mGridLines.strokeWidth = 4f

        mControlBarText.textAlign = Paint.Align.CENTER
        mControlBarText.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            11f, getContext().resources.displayMetrics
        )
        mControlBarText.color = getColor(android.R.attr.textColorPrimary)
        mControlBarText.isAntiAlias = true

        mFrequencyResponseBg.style = Paint.Style.FILL
        mFrequencyResponseBg.alpha = 192

        mFrequencyResponseHighlight.style = Paint.Style.STROKE
        mFrequencyResponseHighlight.color = getColor(android.R.attr.colorAccent)
        mFrequencyResponseHighlight.isAntiAlias = true
        mFrequencyResponseHighlight.strokeWidth = 8f
    }

    private fun getColor(colorAttribute: Int): Int {
        var color = 0
        context.withStyledAttributes(TypedValue().data, intArrayOf(colorAttribute)) {
            color = getColor(0, 0)
        }
        return color
    }

    override fun onSaveInstanceState() =
        bundleOf("super" to super.onSaveInstanceState(), "levels" to mLevels)

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState((state as Bundle).getParcelableAs("super"))
        mLevels = state.getDoubleArray("levels") ?: DoubleArray(bandsNum)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        mWidth = (right - left).toFloat()
        mHeight = (bottom - top).toFloat()

        val responseColors =
            intArrayOf(getColor(android.R.attr.colorAccent), getColor(android.R.color.transparent))
        val responsePositions = floatArrayOf(0.0f, 1f)
        mFrequencyResponseBg.shader = getLinearGradient(mHeight, responseColors, responsePositions)
    }

    private val freqResponse = Path()
    private val freqResponseBg = Path()

    abstract fun computeCurve(freqs: DoubleArray, gains: DoubleArray, resolution: Int, dispFreq: DoubleArray, response: FloatArray)
    abstract val frequencyScale: DoubleArray



    override fun onDraw(canvas: Canvas) {
        freqResponse.rewind()
        freqResponseBg.rewind()

        computeCurve(frequencyScale, mLevels, nPts, displayFreq, response)

        var x: Float
        var y: Float
        for (i in 0 until nPts) {
            /* Magnitude response, dB */
            x = precomputeCurveXAxis[i] * mWidth
            y = projectY(response[i]) * mHeight
            /* Set starting point at first point */
            if (i == 0) freqResponse.moveTo(x, y)
            else freqResponse.lineTo(x, y)
        }

        for (i in mLevels.indices) {
            x = projectX(frequencyScale[i]) * mWidth
            y = projectY(mLevels[i].toFloat()) * mHeight

            canvas.drawLine(x, mHeight, x, y, mGridLines)
            if(areKnobsVisible)
            {
                canvas.drawCircle(x, y, 16f, mControlBarKnob)
            }
            canvas.drawText(frequencyScale[i].prettyNumberFormat(), x, mHeight - 16, mControlBarText)
            val gainText = String.format(Locale.ROOT, "%.1f", mLevels[i])
            canvas.drawText(gainText, x, mControlBarText.textSize + 8, mControlBarText)
        }

        // draw horizontal lines
        var dB = minDb + horizLineInterval
        while (dB <= maxDb - horizLineInterval) {
            y = projectY(dB.toFloat()) * mHeight
            canvas.drawLine(0f, y, mWidth, y, mGridLines)
            dB += horizLineInterval
        }

        with(freqResponseBg) {
            addPath(freqResponse)
            offset(0f, -4f)
            lineTo(mWidth, mHeight)
            lineTo(0f, mHeight)
            close()
        }
        canvas.drawPath(freqResponseBg, mFrequencyResponseBg)
        canvas.drawPath(freqResponse, mFrequencyResponseHighlight)
    }

    private fun getLinearGradient(
        y1: Float, responseColors: IntArray, responsePositions: FloatArray
    ) = LinearGradient(0f, 0f, 0f, y1, responseColors, responsePositions, Shader.TileMode.CLAMP)

    private fun reverseProjectX(position: Double): Double {
        val minimumPosition = ln(minHz)
        val maximumPosition = ln(maxHz)
        return exp(position * (maximumPosition - minimumPosition) + minimumPosition)
    }

    private fun projectX(frequency: Double): Float {
        val position = ln(frequency)
        val minimumPosition = ln(minHz)
        val maximumPosition = ln(maxHz)
        return ((position - minimumPosition) / (maximumPosition - minimumPosition)).toFloat()
    }

    private fun projectY(dB: Float): Float {
        val pos = (dB - minDb) / (maxDb - minDb)
        return (1.0f - pos.toFloat())
    }

    /**
     * Find the closest control to given horizontal pixel for adjustment
     *
     * @param px pixel
     * @return index of best match
     */
    fun findClosest(px: Float): Int {
        var idx = 0
        var best = 1e8
        for (i in mLevels.indices) {
            val freq = frequencyScale[i]//15.625 * 1.6.pow((i + 1).toDouble())
            val cx = (projectX(freq) * mWidth).toDouble()
            val distance = abs(cx - px)
            if (distance < best) {
                idx = i
                best = distance
            }
        }
        return idx
    }

    fun setBand(i: Int, value: Double) {
        if(i < 0 || i >= bandsNum) {
            Timber.e("setBand($i): $i is out of range")
            return
        }

        mLevels[i] = value
        postInvalidate()
    }
}