package me.timschneeberger.rootlessjamesdsp.view

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.core.os.bundleOf
import me.timschneeberger.rootlessjamesdsp.model.GraphicEqNodeList
import me.timschneeberger.rootlessjamesdsp.utils.extensions.CompatExtensions.getParcelableAs
import me.timschneeberger.rootlessjamesdsp.utils.extensions.prettyNumberFormat
import java.util.*
import kotlin.math.*

class GraphicEqualizerSurface(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var mGridLines = Paint()
    private var mGridThickLines = Paint()
    private var mControlBarText = Paint()
    private var mFrequencyResponseBg = Paint()
    private var mFrequencyResponseHighlight = Paint()

    private var mFreqs = DoubleArray(0)
    private var mGains = DoubleArray(0)

    private var mHeight = 0.0f
    private var mWidth = 0.0f

    private var nPts = 128
    private var displayFreq = DoubleArray(nPts)
    private val precomputeCurveXAxis = FloatArray(nPts)
    private var precomputeFreqAxis = FloatArray(2)

    fun addElement(org: FloatArray, added: Float): FloatArray {
        val result = org.copyOf(org.size + 1)
        result[org.size] = added
        return result
    }

    init {
        for (i in 0 until nPts) displayFreq[i] = reverseProjectX(i / (nPts - 1).toDouble())
        for (i in 0 until nPts) precomputeCurveXAxis[i] = projectX(displayFreq[i])
        var freq = MIN_FREQ
        while (freq < MAX_FREQ) {
            precomputeFreqAxis = addElement(precomputeFreqAxis, projectX(freq))
            freq += if (freq < 100) 10 else if (freq < 1000) 100 else if (freq < 10000) 1000 else 10000
        }

        mGridLines.color = getColor(android.R.attr.colorControlHighlight)
        mGridLines.style = Paint.Style.STROKE
        mGridLines.strokeWidth = 4f

        mGridThickLines.color = getColor(android.R.attr.colorControlHighlight)
        mGridThickLines.style = Paint.Style.STROKE
        mGridThickLines.strokeWidth = 8f

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
        if(this.isInEditMode) {
            // Broken in edit mode
            return Color.BLACK
        }

        var color = 0
        context.withStyledAttributes(TypedValue().data, intArrayOf(colorAttribute)) {
            color = getColor(0, 0)
        }
        return color
    }

    override fun onSaveInstanceState() =
        bundleOf("super" to super.onSaveInstanceState(), STATE_GAIN to mGains, STATE_FREQ to mFreqs)

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState((state as Bundle).getParcelableAs("super"))
        mFreqs = state.getDoubleArray(STATE_FREQ) ?: DoubleArray(0)
        mGains = state.getDoubleArray(STATE_GAIN) ?: DoubleArray(0)
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
    private val lastFreqTextBounds = RectF()
    private val lastGainTextBounds = RectF()

    override fun onDraw(canvas: Canvas) {
        freqResponse.rewind()
        freqResponseBg.rewind()

        freqResponse.moveTo(0f, projectY(mGains.firstOrNull()?.toFloat() ?: 0f) * mHeight)

        var x: Float
        var y = projectY(0f) * mHeight
        for ((index, freq) in mFreqs.withIndex()) {
            val gain = mGains[index]
            x = projectX(freq) * mWidth
            y = projectY(gain.toFloat()) * mHeight
            if(x >= 0) {
                freqResponse.lineTo(x, y)
            }
        }

        freqResponse.lineTo(mWidth, y)

        lastFreqTextBounds.set(0f,0f,0f,0f)
        lastGainTextBounds.set(0f,0f,0f,0f)

        for (i in mGains.indices) {
            x = projectX(mFreqs[i]) * mWidth
            y = projectY(mGains[i].toFloat()) * mHeight
            canvas.drawLine(x, mHeight, x, y, mGridLines)

            if (mGains.size > 1 /* only if more than 1 nodes defined */ &&
                x > projectX(25.0) * mWidth /* only >25Hz */ &&
                x > lastFreqTextBounds.right + lastFreqTextBounds.width() /* check for freq text overlap */ &&
                x > lastGainTextBounds.right + lastGainTextBounds.width() /* check for gain text overlap */) {

                val freqText = mFreqs[i].prettyNumberFormat()
                val freqWidth = mControlBarText.measureText(freqText)
                val gainText = String.format(Locale.ROOT, "%.1f", mGains[i])
                val gainWidth = mControlBarText.measureText(gainText)

                lastFreqTextBounds.set(x - (freqWidth/2), mHeight - 16, x + freqWidth - (freqWidth/2),  mHeight - 4)
                lastGainTextBounds.set(x - (gainWidth/2), mControlBarText.textSize + 8, x + gainWidth - (gainWidth/2),  mControlBarText.textSize + 20)

                canvas.drawText(freqText, x, mHeight - 16, mControlBarText)
                canvas.drawText(gainText, x, mControlBarText.textSize + 8, mControlBarText)
            }
        }

        // alternative labels for less than 2 nodes
        if (mGains.size <= 1) {
            val gain = mGains.firstOrNull() ?: 0.0
            for (scale in FreqScale) {
                x = projectX(scale) * mWidth
                // UNUSED: y = projectY(gain.toFloat()) * mHeight

                val freqText = scale.prettyNumberFormat()
                val gainText = String.format(Locale.ROOT, "%.1f", gain)

                canvas.drawText(freqText, x, mHeight - 16, mControlBarText)
                canvas.drawText(gainText, x, mControlBarText.textSize + 8, mControlBarText)
            }
        }

        // draw horizontal lines
        var dB = MIN_DB + 3
        while (dB <= MAX_DB - 3) {
            y = projectY(dB.toFloat()) * mHeight
            if(dB == 0)
                canvas.drawLine(0f, y, mWidth, y, mGridThickLines)
            else
                canvas.drawLine(0f, y, mWidth, y, mGridLines)

            dB += 3
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
        y1: Float, responseColors: IntArray, responsePositions: FloatArray,
    ) = LinearGradient(0f, 0f, 0f, y1, responseColors, responsePositions, Shader.TileMode.CLAMP)

    private fun reverseProjectX(position: Double): Double {
        val minimumPosition = ln(MIN_FREQ)
        val maximumPosition = ln(MAX_FREQ)
        return exp(position * (maximumPosition - minimumPosition) + minimumPosition)
    }

    private fun projectX(frequency: Double): Float {
        val position = ln(frequency)
        val minimumPosition = ln(MIN_FREQ)
        val maximumPosition = ln(MAX_FREQ)
        return ((position - minimumPosition) / (maximumPosition - minimumPosition)).toFloat()
    }

    private fun projectY(dB: Float): Float {
        val pos = (dB - MIN_DB) / (MAX_DB - MIN_DB)
        return (1.0f - pos)
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
        for (i in mGains.indices) {
            val freq = 15.625 * 1.6.pow((i + 1).toDouble())
            val cx = (projectX(freq) * mWidth).toDouble()
            val distance = abs(cx - px)
            if (distance < best) {
                idx = i
                best = distance
            }
        }
        return idx
    }

    fun setNodes(nodeArray: GraphicEqNodeList) {
        nodeArray.sortBy { it.freq }

        val (freq, gain, _) = nodeArray.toArrays()
        setNodes(freq, gain)
    }

    private fun setNodes(freqArray: DoubleArray, gainArray: DoubleArray) {
        mFreqs = freqArray
        mGains = gainArray

        MIN_DB = floor(mGains.minOrNull() ?: -15.0).toInt()
        MAX_DB = ceil(mGains.maxOrNull() ?: 15.0).toInt()

        if(MIN_DB > -15) MIN_DB = -15
        if(MAX_DB < 15) MAX_DB = 15

        postInvalidate()
    }

    private var MIN_DB = -15
    private var MAX_DB = 15

    companion object {
        private const val STATE_FREQ = "freq"
        private const val STATE_GAIN = "gain"

        private const val MIN_FREQ = 20.0
        private const val MAX_FREQ = 20000.0

        private val FreqScale = doubleArrayOf(
            25.0,
            40.0,
            63.0,
            100.0,
            160.0,
            250.0,
            400.0,
            630.0,
            1000.0,
            1600.0,
            2500.0,
            4000.0,
            6300.0,
            10000.0,
            16000.0
        )
    }
}