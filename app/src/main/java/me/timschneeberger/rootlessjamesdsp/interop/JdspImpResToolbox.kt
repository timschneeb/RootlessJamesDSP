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
        path: String?,
        filename: String?,
        targetSampleRate: Int
    ): String?

    external fun ComputeEqResponse(
        n: Int,
        freq: DoubleArray,
        gain: DoubleArray,
        interpolationMode: Int,
        queryPts: Int,
        dispFreq: DoubleArray,
        response: FloatArray
    ): Int

    init {
        System.loadLibrary("jdspimprestoolbox")
    }
}
