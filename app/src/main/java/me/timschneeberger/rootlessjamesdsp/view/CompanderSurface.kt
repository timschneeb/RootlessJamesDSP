package me.timschneeberger.rootlessjamesdsp.view

import android.content.Context
import android.util.AttributeSet
import me.timschneeberger.rootlessjamesdsp.interop.JdspImpResToolbox

class CompanderSurface(context: Context?, attrs: AttributeSet?) : BaseEqualizerSurface(context, attrs, 7, 40.0, 20000.0, -1.2, 1.2, 0.2f) {
    override fun computeCurve(
        freqs: DoubleArray,
        gains: DoubleArray,
        resolution: Int,
        dispFreq: DoubleArray,
        response: FloatArray
    ) {
        JdspImpResToolbox.ComputeCompResponse(freqs, gains, resolution, dispFreq, response)
    }

    override val frequencyScale: DoubleArray
        get() = SCALE

    companion object {
        val SCALE = doubleArrayOf(95.0, 200.0, 400.0, 800.0, 1600.0, 3400.0, 7500.0)
    }
}