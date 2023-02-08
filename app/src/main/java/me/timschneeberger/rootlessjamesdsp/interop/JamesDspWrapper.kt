package me.timschneeberger.rootlessjamesdsp.interop

import me.timschneeberger.rootlessjamesdsp.interop.structure.EelVmVariable
import me.timschneeberger.rootlessjamesdsp.model.ProcessorMessage
import java.util.ArrayList

typealias JamesDspHandle = Long

object JamesDspWrapper {
    // Memory management
    external fun alloc(callbacks: JamesDspCallbacks): JamesDspHandle
    external fun free(self: JamesDspHandle)
    external fun isHandleValid(self: JamesDspHandle): Boolean

    // Processing (interleaved)
    external fun processInt16(self: JamesDspHandle, input: ShortArray): ShortArray
    external fun processInt32(self: JamesDspHandle, input: IntArray): IntArray
    external fun processFloat(self: JamesDspHandle, input: FloatArray): FloatArray

    // Engine config
    external fun setSamplingRate(self: JamesDspHandle, sampleRate: Float, forceRefresh: Boolean)

    // Effect config
    external fun setLimiter(self: JamesDspHandle, threshold: Float, release: Float): Boolean
    external fun setPostGain(self: JamesDspHandle, postGain: Float): Boolean
    external fun setFirEqualizer(self: JamesDspHandle, enable: Boolean, filterType: Int, interpolationMode: Int, bands: DoubleArray): Boolean
    external fun setVdc(self: JamesDspHandle, enable: Boolean, vdcContents: String): Boolean
    external fun setCompressor(self: JamesDspHandle, enable: Boolean, maxAttack: Float, maxRelease: Float, adaptSpeed: Float): Boolean
    external fun setReverb(self: JamesDspHandle, enable: Boolean, preset: Int): Boolean
    external fun setConvolver(self: JamesDspHandle, enable: Boolean, impulseResponse: FloatArray, irChannels: Int, irFrames: Int): Boolean
    external fun setGraphicEq(self: JamesDspHandle, enable: Boolean, graphicEq: String): Boolean
    external fun setCrossfeed(self: JamesDspHandle, enable: Boolean, mode: Int, customFcut: Int, customFeed: Int): Boolean
    external fun setBassBoost(self: JamesDspHandle, enable: Boolean, maxGain: Float): Boolean
    external fun setStereoEnhancement(self: JamesDspHandle, enable: Boolean, level: Float): Boolean
    external fun setVacuumTube(self: JamesDspHandle, enable: Boolean, level: Float): Boolean
    external fun setLiveprog(self: JamesDspHandle, enable: Boolean, id: String, liveprogContent: String): Boolean

    // EEL VM utilities
    external fun enumerateEelVariables(self: JamesDspHandle): ArrayList<EelVmVariable>
    external fun manipulateEelVariable(self: JamesDspHandle, name: String, value: Float): Boolean
    external fun freezeLiveprogExecution(self: JamesDspHandle, freeze: Boolean)
    external fun eelErrorCodeToString(errorCode: Int): String

    // Callbacks
    interface JamesDspCallbacks
    {
        fun onLiveprogOutput(message: String)
        fun onLiveprogExec(id: String)
        fun onLiveprogResult(resultCode: Int, id: String, errorMessage: String?)
        fun onVdcParseError()
        fun onConvolverParseError(errorCode: ProcessorMessage.ConvolverErrorCode)
    }

    init
    {
        System.loadLibrary("jamesdsp-wrapper")
    }
}