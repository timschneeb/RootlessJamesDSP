package me.timschneeberger.rootlessjamesdsp.native

import android.content.Context
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.native.struct.EelVariable
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.io.FileReader
import java.lang.NumberFormatException

class JamesDspEngine(val context: Context, val callbacks: JamesDspWrapper.JamesDspCallbacks? = null) : AutoCloseable {
    var handle: JamesDspHandle = JamesDspWrapper.alloc(callbacks ?: DummyCallbacks())

    var bypass: Boolean = false
    var sampleRate: Float = 48000.0f
        private set
    private val cache = mutableMapOf<String, Any>()

    override fun close() {
        Timber.tag(TAG).d("Engine freed. Handle $handle can't be used anymore")
        JamesDspWrapper.free(handle)
        handle = 0
    }

    // Processing
    fun processInt16(input: ShortArray): ShortArray
    {
        if(bypass)
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
        if(bypass)
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
        if(bypass)
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


    fun setSamplingRate(sampleRate: Float)
    {
        JamesDspWrapper.setSamplingRate(handle, sampleRate, false)
        this.sampleRate = sampleRate
    }


    private fun syncCache(key: String, newValue: Any): Boolean
    {
        return if(cache.containsKey(key) && cache[key] == newValue) {
            false
        } else {
            cache[key] = newValue;
            true
        }
    }

    fun syncWithPreferences() {
        Timber.tag(TAG).d("Synchronizing with preferences...")

        val output = context.getSharedPreferences(Constants.PREF_OUTPUT, Context.MODE_PRIVATE)
        val compressor = context.getSharedPreferences(Constants.PREF_COMPRESSOR, Context.MODE_PRIVATE)
        val bass = context.getSharedPreferences(Constants.PREF_BASS, Context.MODE_PRIVATE)
        val eq = context.getSharedPreferences(Constants.PREF_EQ, Context.MODE_PRIVATE)
        val geq = context.getSharedPreferences(Constants.PREF_GEQ, Context.MODE_PRIVATE)
        val ddc = context.getSharedPreferences(Constants.PREF_DDC, Context.MODE_PRIVATE)
        val convolver = context.getSharedPreferences(Constants.PREF_CONVOLVER, Context.MODE_PRIVATE)
        val liveprog = context.getSharedPreferences(Constants.PREF_LIVEPROG, Context.MODE_PRIVATE)
        val tube = context.getSharedPreferences(Constants.PREF_TUBE, Context.MODE_PRIVATE)
        val stereo = context.getSharedPreferences(Constants.PREF_STEREOWIDE, Context.MODE_PRIVATE)
        val crossfeed = context.getSharedPreferences(Constants.PREF_CROSSFEED, Context.MODE_PRIVATE)
        val reverb = context.getSharedPreferences(Constants.PREF_REVERB, Context.MODE_PRIVATE)

        val compressorEnabled = compressor.getBoolean(context.getString(R.string.key_compression_enable), false)
        val bassBoostEnabled = bass.getBoolean(context.getString(R.string.key_bass_enable), false)
        val equalizerEnabled = eq.getBoolean(context.getString(R.string.key_eq_enable), false)
        val graphicEqEnabled = geq.getBoolean(context.getString(R.string.key_geq_enable), false)
        val reverbEnabled = reverb.getBoolean(context.getString(R.string.key_reverb_enable), false)
        val stereoWideEnabled = stereo.getBoolean(context.getString(R.string.key_stereowide_enable), false)
        val crossfeedEnabled = crossfeed.getBoolean(context.getString(R.string.key_crossfeed_enable), false)
        val convolverEnabled = convolver.getBoolean(context.getString(R.string.key_convolver_enable), false)
        val analogModelEnabled = tube.getBoolean(context.getString(R.string.key_tube_enable), false)
        val ddcEnabled = ddc.getBoolean(context.getString(R.string.key_ddc_enable), false)
        val liveProgEnabled = liveprog.getBoolean(context.getString(R.string.key_liveprog_enable), false)

        //  try {
        val limThreshold = output.getFloat(context.getString(R.string.key_limiter_threshold), -0.1f)
        val limRelease = output.getFloat(context.getString(R.string.key_limiter_release), 60f)
        val postGain = output.getFloat(context.getString(R.string.key_output_postgain), 0f)

        if (syncCache(context.getString(R.string.key_limiter_threshold), limThreshold) ||
            syncCache(context.getString(R.string.key_limiter_release), limRelease) ||
            syncCache(context.getString(R.string.key_output_postgain), postGain)
        ) {
            setLimiter(limThreshold, limRelease)
            setPostGain(postGain)
        }

        val maxAttack = compressor.getFloat(context.getString(R.string.key_compression_max_atk), 30f)
        val maxRelease = compressor.getFloat(context.getString(R.string.key_compression_max_rel), 200f)
        val adaptSpeed = compressor.getFloat(context.getString(R.string.key_compression_adapt_speed), 800f)
        setCompressor(compressorEnabled, maxAttack, maxRelease, adaptSpeed)

        val maxBassGain = bass.getFloat(context.getString(R.string.key_bass_max_gain), 5f)
        setBassBoost(bassBoostEnabled, maxBassGain);

        val eqBands = eq.getString(context.getString(R.string.key_eq_bands), Constants.DEFAULT_EQ)
        val eqFilterType = eq.getString(context.getString(R.string.key_eq_filter_type), "0")!!.toInt()
        val eqInterpolationMode = eq.getString(context.getString(R.string.key_eq_interpolation), "0")!!.toInt()
        setFirEqualizer(
            equalizerEnabled,
            eqFilterType,
            eqInterpolationMode,
            eqBands ?: Constants.DEFAULT_EQ
        );

        val graphicEqBands =
            geq.getString(context.getString(R.string.key_geq_nodes), Constants.DEFAULT_GEQ) ?: Constants.DEFAULT_GEQ
        if (syncCache(context.getString(R.string.key_geq_enable), graphicEqEnabled) ||
            syncCache(context.getString(R.string.key_geq_nodes), graphicEqBands)
        ) {
            setGraphicEq(graphicEqEnabled, graphicEqBands)
        }

        val reverbPreset = reverb.getString(context.getString(R.string.key_reverb_preset), "0")!!.toInt()
        setReverb(reverbEnabled, reverbPreset)

        val stereoWideMode = stereo.getFloat(context.getString(R.string.key_stereowide_mode), 60f)
        setStereoEnhancement(stereoWideEnabled, stereoWideMode)

        val crossfeedMode = crossfeed.getString(context.getString(R.string.key_crossfeed_mode), "0")!!.toInt()
        setCrossfeed(crossfeedEnabled, crossfeedMode)

        val tubeDrive = tube.getFloat(context.getString(R.string.key_tube_drive), 2f)
        setVacuumTube(analogModelEnabled, tubeDrive)

        val ddcFile = ddc.getString(context.getString(R.string.key_ddc_file), "") ?: ""
        if (syncCache(context.getString(R.string.key_ddc_enable), ddcEnabled) ||
            syncCache(context.getString(R.string.key_ddc_file), ddcFile)
        ) {
            setVdc(ddcEnabled, ddcFile)
        }

        val liveprogFile = liveprog.getString(context.getString(R.string.key_liveprog_file), "") ?: ""
        if (syncCache(context.getString(R.string.key_liveprog_enable), liveProgEnabled) ||
            syncCache(context.getString(R.string.key_liveprog_file), liveprogFile)
        ) {
            setLiveprog(liveProgEnabled, liveprogFile)
        }

        val convolverFile = convolver.getString(context.getString(R.string.key_convolver_file), "") ?: ""
        val convolverMode = convolver.getString(context.getString(R.string.key_convolver_mode), "0")!!.toInt()
        val convolverAdvImp =
            convolver.getString(context.getString(R.string.key_convolver_adv_imp), Constants.DEFAULT_CONVOLVER_ADVIMP)
                ?: Constants.DEFAULT_CONVOLVER_ADVIMP
        if (syncCache(context.getString(R.string.key_convolver_enable), convolverEnabled) ||
            syncCache(context.getString(R.string.key_convolver_file), convolverFile) ||
            syncCache(context.getString(R.string.key_convolver_mode), convolverMode) ||
            syncCache(context.getString(R.string.key_convolver_adv_imp), convolverAdvImp)
        ) {
            setConvolver(convolverEnabled, convolverFile, convolverMode, convolverAdvImp)
        }
        // }
        //catch(ex: Exception){}
    }

    // Effect config
    fun setLimiter(threshold: Float, release: Float)
    {
        JamesDspWrapper.setLimiter(handle, threshold, release)
    }

    fun setPostGain(postGain: Float)
    {
        JamesDspWrapper.setPostGain(handle, postGain)
    }

    fun setFirEqualizer(enable: Boolean, filterType: Int, interpolationMode: Int, bands: String)
    {
        val doubleArray = DoubleArray(30)
        val array = bands.split(";")
        var i = 0;
        for(str in array)
        {
            val number = str.toDoubleOrNull()
            if(number == null) {
                Timber.tag(TAG).e("setFirEqualizer: malformed EQ string")
                return
            }
            doubleArray[i] = number;
            i++
        }

        JamesDspWrapper.setFirEqualizer(handle, enable, filterType, interpolationMode, doubleArray)
    }

    fun setVdc(enable: Boolean, vdcPath: String)
    {
        if(!File(vdcPath).exists()) {
            Timber.tag(TAG).w("setVdc: file does not exist")
            JamesDspWrapper.setVdc(handle, false, "")
            return;
        }

        val reader = FileReader(vdcPath)
        JamesDspWrapper.setVdc(handle, enable, reader.readText())
        reader.close()
    }

    fun setCompressor(enable: Boolean, maxAttack: Float, maxRelease: Float, adaptSpeed: Float)
    {
        JamesDspWrapper.setCompressor(handle, enable, maxAttack, maxRelease, adaptSpeed)
    }

    fun setReverb(enable: Boolean, preset: Int)
    {
        JamesDspWrapper.setReverb(handle, enable, preset)
    }

    fun setConvolver(enable: Boolean, impulseResponsePath: String, optimizationMode: Int, waveEditStr: String)
    {
        if(!File(impulseResponsePath).exists()) {
            Timber.tag(TAG).w("setConvolver: file does not exist")
            JamesDspWrapper.setConvolver(handle, false, FloatArray(0), 0, 0)
            return;
        }

        val advConv = waveEditStr.split(";")
        val advSetting = IntArray(6)
        try
        {
            advSetting[0] = -100
            advSetting[1] = -100
            if (advConv.size == 6)
            {
                for (i in advConv.indices) advSetting[i] = Integer.valueOf(advConv[i])
            }
            else {
                Timber.tag(TAG)
                    .w("setConvolver: AdvImp setting has the wrong size (${advConv.size})")
            }
        }
        catch(ex: NumberFormatException) {
            Timber.tag(TAG)
                .e("setConvolver: NumberFormatException while parsing AdvImp setting. Using defaults.")

            advSetting[0] = -80
            advSetting[1] = -100
            advSetting[2] = 23
            advSetting[3] = 12
            advSetting[4] = 17
            advSetting[5] = 28
        }

        val info = IntArray(2)
        val imp = JdspImpResToolbox.ReadImpulseResponseToFloat(
            impulseResponsePath,
            sampleRate.toInt(),
            info,
            optimizationMode,
            advSetting
        )
        if(imp == null) {
            Timber.tag(TAG).e("setConvolver: Failed to read IR")
            JamesDspWrapper.setConvolver(handle, false, FloatArray(0), 0, 0)
            return
        }

        JamesDspWrapper.setConvolver(handle, enable, imp, info[0], info[1])
    }

    fun setGraphicEq(enable: Boolean, bands: String)
    {
        JamesDspWrapper.setGraphicEq(handle, enable, bands)
    }

    fun setCrossfeed(enable: Boolean, mode: Int)
    {
        JamesDspWrapper.setCrossfeed(handle, enable, mode, 0, 0)
    }

    fun setCrossfeedCustom(enable: Boolean, fcut: Int, feed: Int)
    {
        JamesDspWrapper.setCrossfeed(handle, enable, 99, fcut, feed)
    }

    fun setBassBoost(enable: Boolean, maxGain: Float)
    {
        JamesDspWrapper.setBassBoost(handle, enable, maxGain)
    }

    fun setStereoEnhancement(enable: Boolean, level: Float)
    {
        JamesDspWrapper.setStereoEnhancement(handle, enable, level)
    }

    fun setVacuumTube(enable: Boolean, level: Float)
    {
        JamesDspWrapper.setVacuumTube(handle, enable, level)
    }

    fun setLiveprog(enable: Boolean, path: String)
    {
        if(!File(path).exists()) {
            Timber.tag(TAG).w("setLiveprog: file does not exist")
            JamesDspWrapper.setLiveprog(handle, false, "", "")
            return;
        }

        val reader = FileReader(path)
        val name = File(path).name;
        JamesDspWrapper.setLiveprog(handle, enable, name, reader.readText())
        reader.close()
    }

    // EEL VM utilities
    fun enumerateEelVariables(): ArrayList<EelVariable>
    {
        return JamesDspWrapper.enumerateEelVariables(handle)
    }

    fun manipulateEelVariable(name: String, value: Float): Boolean
    {
        return JamesDspWrapper.manipulateEelVariable(handle, name, value)
    }

    fun freezeLiveprogExecution(freeze: Boolean)
    {
        JamesDspWrapper.freezeLiveprogExecution(handle, freeze)
    }

    private inner class DummyCallbacks : JamesDspWrapper.JamesDspCallbacks
    {
        override fun onLiveprogOutput(message: String) {}
        override fun onLiveprogExec(id: String) {}
        override fun onLiveprogResult(resultCode: Int, id: String, errorMessage: String?) {}
        override fun onVdcParseError() {}
    }

    companion object
    {
        const val TAG = "JamesDspEngine"
    }
}