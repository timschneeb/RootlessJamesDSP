package me.timschneeberger.rootlessjamesdsp.interop

object JdspImpResToolbox {
    external fun ReadImpulseResponseToFloat(
        path: String?,
        targetSampleRate: Int,
        audioInfo: IntArray?,
        convMode: Int,
        advParam: IntArray?
    ): FloatArray?

    external fun OfflineAudioResample(
        path: String,
        filename: String,
        targetSampleRate: Int
    ): String

    external fun ComputeEqResponse(
        n: Int,
        freq: DoubleArray,
        gain: DoubleArray,
        interpolationMode: Int,
        queryPts: Int,
        dispFreq: DoubleArray,
        response: FloatArray
    ): Int

    external fun ComputeCompResponse(
        freq: DoubleArray,
        gain: DoubleArray,
        queryPts: Int,
        dispFreq: DoubleArray,
        response: FloatArray
    )

    external fun ComputeIIREqualizerCplx(
        srate: Int,
        order: Int,
        freq: DoubleArray,
        gain: DoubleArray,
        nPts: Int,
        dispFreq: DoubleArray,
        cplxRe: DoubleArray,
        cplxIm: DoubleArray
    )

    external fun ComputeIIREqualizerResponse(
        nPts: Int,
        cplxRe: DoubleArray,
        cplxIm: DoubleArray,
        response: FloatArray,
    )

    init {
        System.loadLibrary("jdspimprestoolbox")
    }
}
