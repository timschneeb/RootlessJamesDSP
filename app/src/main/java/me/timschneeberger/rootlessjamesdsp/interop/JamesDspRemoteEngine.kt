package me.timschneeberger.rootlessjamesdsp.interop

import android.content.Context
import android.media.audiofx.AudioEffect
import android.media.audiofx.AudioEffectHidden
import dev.rikka.tools.refine.Refine
import me.timschneeberger.rootlessjamesdsp.interop.structure.EelVmVariable
import timber.log.Timber
import java.util.*

class JamesDspRemoteEngine(
    context: Context,
    sessionId: Int,
    priority: Int,
    callbacks: JamesDspWrapper.JamesDspCallbacks? = null
) : JamesDspBaseEngine(context, callbacks) {
    val effect: AudioEffectHidden = try {
        Refine.unsafeCast(
            AudioEffect::class.java
                .getConstructor(UUID::class.java, UUID::class.java, Integer.TYPE, Integer.TYPE)
                .newInstance(EFFECT_TYPE_CUSTOM, EFFECT_JAMESDSP, priority, sessionId)
        )
    } catch (e: Exception) {
        Timber.e("Failed to create JamesDSP effect")
        Timber.e(e)
        throw IllegalStateException(e) // TODO make sure to handle
    }

    override var bypass: Boolean = false
    override var sampleRate: Float = 48000.0f
        set(value) {
            field = value
            // TODO set sample rate
        }

    override fun setLimiter(threshold: Float, release: Float): Boolean {
        TODO("Not yet implemented")
    }

    override fun setPostGain(postGain: Float): Boolean {
        TODO("Not yet implemented")
    }

    override fun setCompressor(
        enable: Boolean,
        maxAttack: Float,
        maxRelease: Float,
        adaptSpeed: Float
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun setReverb(enable: Boolean, preset: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun setCrossfeed(enable: Boolean, mode: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun setCrossfeedCustom(enable: Boolean, fcut: Int, feed: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun setBassBoost(enable: Boolean, maxGain: Float): Boolean {
        TODO("Not yet implemented")
    }

    override fun setStereoEnhancement(enable: Boolean, level: Float): Boolean {
        TODO("Not yet implemented")
    }

    override fun setVacuumTube(enable: Boolean, level: Float): Boolean {
        TODO("Not yet implemented")
    }

    override fun setFirEqualizerInternal(
        enable: Boolean,
        filterType: Int,
        interpolationMode: Int,
        bands: DoubleArray
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun setVdcInternal(enable: Boolean, vdcPath: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun setConvolverInternal(
        enable: Boolean,
        impulseResponse: FloatArray,
        irChannels: Int,
        irFrames: Int
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun setGraphicEqInternal(enable: Boolean, bands: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun setLiveprogInternal(enable: Boolean, name: String, path: String): Boolean {
        TODO("Not yet implemented")
    }


    // EEL VM utilities
    override fun supportsEelVmAccess(): Boolean { return false }

    override fun enumerateEelVariables(): ArrayList<EelVmVariable>
    {
        return arrayListOf()
    }

    override fun manipulateEelVariable(name: String, value: Float): Boolean
    {
        return false
    }

    override fun freezeLiveprogExecution(freeze: Boolean) {}

    private inner class DummyCallbacks : JamesDspWrapper.JamesDspCallbacks
    {
        override fun onLiveprogOutput(message: String) {}
        override fun onLiveprogExec(id: String) {}
        override fun onLiveprogResult(resultCode: Int, id: String, errorMessage: String?) {}
        override fun onVdcParseError() {}
    }

    companion object {
        private val EFFECT_TYPE_CUSTOM = UUID.fromString("f98765f4-c321-5de6-9a45-123459495ab2")
        private val EFFECT_JAMESDSP = UUID.fromString("f27317f4-c984-4de6-9a90-545759495bf2")
    }
}