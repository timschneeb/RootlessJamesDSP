package me.timschneeberger.rootlessjamesdsp.interop

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.interop.structure.EelVmVariable
import timber.log.Timber
import java.io.File
import java.io.FileReader
import java.lang.NumberFormatException

class JamesDspLocalEngine(context: Context, callbacks: JamesDspWrapper.JamesDspCallbacks? = null) : JamesDspBaseEngine(context, callbacks) {
    var handle: JamesDspHandle = JamesDspWrapper.alloc(callbacks ?: DummyCallbacks())

    override var sampleRate: Float = 48000.0f
        set(value) {
            field = value
            JamesDspWrapper.setSamplingRate(handle, value, false)
        }
    override var enabled: Boolean = true

    override fun close() {
        JamesDspWrapper.free(handle)
        Timber.d("Handle $handle has been freed")
        handle = 0
    }

    // Processing
    fun processInt16(input: ShortArray): ShortArray
    {
        if(!enabled)
        {
            return input
        }

        if(!JamesDspWrapper.isHandleValid(handle))
        {
            Timber.e("Invalid handle")
            return input
        }

        return JamesDspWrapper.processInt16(handle, input)
    }

    fun processInt32(input: IntArray): IntArray
    {
        if(!enabled)
        {
            return input
        }

        if(!JamesDspWrapper.isHandleValid(handle))
        {
            Timber.e("Invalid handle")
            return input
        }

        return JamesDspWrapper.processInt32(handle, input)
    }

    fun processFloat(input: FloatArray): FloatArray
    {
        if(!enabled)
        {
            return input
        }

        if(!JamesDspWrapper.isHandleValid(handle))
        {
            Timber.e("Invalid handle")
            return input
        }

        return JamesDspWrapper.processFloat(handle, input)
    }

    // Effect config
    override fun setOutputControl(threshold: Float, release: Float, postGain: Float): Boolean {
        return JamesDspWrapper.setLimiter(handle, threshold, release) and JamesDspWrapper.setPostGain(handle, postGain)
    }

    override fun setCompressor(enable: Boolean, maxAttack: Float, maxRelease: Float, adaptSpeed: Float): Boolean
    {
        return JamesDspWrapper.setCompressor(handle, enable, maxAttack, maxRelease, adaptSpeed)
    }

    override fun setReverb(enable: Boolean, preset: Int): Boolean
    {
        return JamesDspWrapper.setReverb(handle, enable, preset)
    }

    override fun setCrossfeed(enable: Boolean, mode: Int): Boolean
    {
        return JamesDspWrapper.setCrossfeed(handle, enable, mode, 0, 0)
    }

    override fun setCrossfeedCustom(enable: Boolean, fcut: Int, feed: Int): Boolean
    {
        return JamesDspWrapper.setCrossfeed(handle, enable, 99, fcut, feed)
    }

    override fun setBassBoost(enable: Boolean, maxGain: Float): Boolean
    {
        return JamesDspWrapper.setBassBoost(handle, enable, maxGain)
    }

    override fun setStereoEnhancement(enable: Boolean, level: Float): Boolean
    {
        return JamesDspWrapper.setStereoEnhancement(handle, enable, level)
    }

    override fun setVacuumTube(enable: Boolean, level: Float): Boolean
    {
        return JamesDspWrapper.setVacuumTube(handle, enable, level)
    }

    override fun setFirEqualizerInternal(
        enable: Boolean,
        filterType: Int,
        interpolationMode: Int,
        bands: DoubleArray
    ): Boolean {
        return JamesDspWrapper.setFirEqualizer(handle, enable, filterType, interpolationMode, bands)
    }

    override fun setVdcInternal(enable: Boolean, vdc: String): Boolean {
        return JamesDspWrapper.setVdc(handle, enable, vdc)
    }

    override fun setConvolverInternal(
        enable: Boolean,
        impulseResponse: FloatArray,
        irChannels: Int,
        irFrames: Int
    ): Boolean {
        return JamesDspWrapper.setConvolver(handle, enable, impulseResponse, irChannels, irFrames)
    }

    override fun setGraphicEqInternal(enable: Boolean, bands: String): Boolean {
        return JamesDspWrapper.setGraphicEq(handle, enable, bands)
    }

    override fun setLiveprogInternal(enable: Boolean, name: String, path: String): Boolean {
        return JamesDspWrapper.setLiveprog(handle, enable, name, path)
    }

    // Feature support
    override fun supportsEelVmAccess(): Boolean { return true }
    override fun supportsCustomCrossfeed(): Boolean { return true }

    // EEL VM utilities
    override fun enumerateEelVariables(): ArrayList<EelVmVariable>
    {
        return JamesDspWrapper.enumerateEelVariables(handle)
    }

    override fun manipulateEelVariable(name: String, value: Float): Boolean
    {
        return JamesDspWrapper.manipulateEelVariable(handle, name, value)
    }

    override fun freezeLiveprogExecution(freeze: Boolean)
    {
        JamesDspWrapper.freezeLiveprogExecution(handle, freeze)
    }
}