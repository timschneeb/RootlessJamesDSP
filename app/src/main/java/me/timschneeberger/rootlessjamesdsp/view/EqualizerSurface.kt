package me.timschneeberger.rootlessjamesdsp.view

import android.content.Context
import android.util.AttributeSet
import me.timschneeberger.rootlessjamesdsp.interop.JdspImpResToolbox

class EqualizerSurface(context: Context?, attrs: AttributeSet?) : BaseEqualizerSurface(context, attrs, 15, 20.0, 20000.0, -12.0, 12.0, 3.0f) {
    enum class Mode {
        Fir,
        Iir
    }

    var mode: Mode = Mode.Fir
        set(value) {
            field = value
            invalidate()
        }
    var iirOrder: Int = 4
        set(value) {
            field = value
            invalidate()
        }

    private val cplxRe = DoubleArray(nPts)
    private val cplxIm = DoubleArray(nPts)

    override fun computeCurve(
        freqs: DoubleArray,
        gains: DoubleArray,
        resolution: Int,
        dispFreq: DoubleArray,
        response: FloatArray
    ) {
        when(mode) {
            Mode.Fir -> JdspImpResToolbox.ComputeEqResponse(15, freqs, gains, 1, resolution, dispFreq, response)
            Mode.Iir -> {
                JdspImpResToolbox.ComputeIIREqualizerCplx(48000, iirOrder, freqs, gains, resolution, dispFreq, cplxRe, cplxIm)
                JdspImpResToolbox.ComputeIIREqualizerResponse(nPts, cplxRe, cplxIm, response)
            }
        }
    }

    override val frequencyScale: DoubleArray
        get() = SCALE

    companion object {
        val SCALE = doubleArrayOf(25.0, 40.0, 63.0, 100.0, 160.0, 250.0, 400.0, 630.0, 1000.0, 1600.0, 2500.0, 4000.0, 6300.0, 10000.0, 16000.0)
    }
}