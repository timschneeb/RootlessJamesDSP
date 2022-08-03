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

class JamesDspEngine(val context: Context, val callbacks: JamesDspWrapper.JamesDspCallbacks? = null) : AutoCloseable {
    var handle: JamesDspHandle = JamesDspWrapper.alloc(callbacks ?: DummyCallbacks())

    var bypass: Boolean = false
    var sampleRate: Float = 48000.0f
        private set

    private val syncScope = CoroutineScope(Dispatchers.Main)
    private val syncMutex = Mutex()
    private val cache = PreferenceCache(context)

    override fun close() {
        Timber.tag(TAG).d("Closing engine. Handle $handle can't be used anymore")
        syncScope.cancel()
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

    fun syncWithPreferences(forceUpdateNamespaces: Array<String>? = null) {
        syncScope.launch {
            syncWithPreferencesAsync(forceUpdateNamespaces)
        }
    }

    private suspend fun syncWithPreferencesAsync(forceUpdateNamespaces: Array<String>? = null) {
        Timber.tag(TAG).d("Synchronizing with preferences... (forced: %s)", forceUpdateNamespaces?.joinToString(";") { it })

        syncMutex.withLock {

            cache.select(Constants.PREF_OUTPUT)
            val outputPostGain = cache.get(R.string.key_output_postgain, 0f)
            val limiterThreshold = cache.get(R.string.key_limiter_threshold, -0.1f)
            val limiterRelease = cache.get(R.string.key_limiter_release, 60f)

            cache.select(Constants.PREF_COMPRESSOR)
            val compEnabled = cache.get(R.string.key_compression_enable, false)
            val compMaxAtk = cache.get(R.string.key_compression_max_atk, 30f)
            val compMaxRel = cache.get(R.string.key_compression_max_rel, 200f)
            val compAdaptSpeed = cache.get(R.string.key_compression_adapt_speed, 800f)

            cache.select(Constants.PREF_BASS)
            val bassEnabled = cache.get(R.string.key_bass_enable, false)
            val bassMaxGain = cache.get(R.string.key_bass_max_gain, 5f)

            cache.select(Constants.PREF_EQ)
            val eqEnabled = cache.get(R.string.key_eq_enable, false)
            val eqFilterType = cache.get(R.string.key_eq_filter_type, "0").toInt()
            val eqInterpolationMode = cache.get(R.string.key_eq_interpolation, "0").toInt()
            val eqBands = cache.get(R.string.key_eq_bands, Constants.DEFAULT_EQ)

            cache.select(Constants.PREF_GEQ)
            val geqEnabled = cache.get(R.string.key_geq_enable, false)
            val geqBands = cache.get(R.string.key_geq_nodes, Constants.DEFAULT_GEQ)

            cache.select(Constants.PREF_REVERB)
            val reverbEnabled = cache.get(R.string.key_reverb_enable, false)
            val reverbPreset = cache.get(R.string.key_reverb_preset, "0").toInt()

            cache.select(Constants.PREF_STEREOWIDE)
            val swEnabled = cache.get(R.string.key_stereowide_enable, false)
            val swMode = cache.get(R.string.key_stereowide_mode, 60f)

            cache.select(Constants.PREF_CROSSFEED)
            val crossfeedEnabled = cache.get(R.string.key_crossfeed_enable, false)
            val crossfeedMode = cache.get(R.string.key_crossfeed_mode, "0").toInt()

            cache.select(Constants.PREF_TUBE)
            val tubeEnabled = cache.get(R.string.key_tube_enable, false)
            val tubeDrive = cache.get(R.string.key_tube_drive, 2f)

            cache.select(Constants.PREF_DDC)
            val ddcEnabled = cache.get(R.string.key_ddc_enable, false)
            val ddcFile = cache.get(R.string.key_ddc_file, "")

            cache.select(Constants.PREF_LIVEPROG)
            val liveProgEnabled = cache.get(R.string.key_liveprog_enable, false)
            val liveprogFile = cache.get(R.string.key_liveprog_file, "")

            cache.select(Constants.PREF_CONVOLVER)
            val convolverEnabled = cache.get(R.string.key_convolver_enable, false)
            val convolverFile = cache.get(R.string.key_convolver_file, "")
            val convolverAdvImp = cache.get(R.string.key_convolver_adv_imp, Constants.DEFAULT_CONVOLVER_ADVIMP)
            val convolverMode = cache.get(R.string.key_convolver_mode, "0").toInt()

            val targets = cache.changedNamespaces.toTypedArray() + (forceUpdateNamespaces ?: arrayOf())
            targets.forEach {
                Timber.tag(TAG).i("Committing new changes in namespace '$it'")

                when (it) {
                    Constants.PREF_OUTPUT -> {
                        setLimiter(limiterThreshold, limiterRelease)
                        setPostGain(outputPostGain)
                    }
                    Constants.PREF_COMPRESSOR -> setCompressor(compEnabled, compMaxAtk, compMaxRel, compAdaptSpeed)
                    Constants.PREF_BASS -> setBassBoost(bassEnabled, bassMaxGain)
                    Constants.PREF_EQ -> setFirEqualizer(eqEnabled, eqFilterType, eqInterpolationMode, eqBands)
                    Constants.PREF_GEQ -> setGraphicEq(geqEnabled, geqBands)
                    Constants.PREF_REVERB -> setReverb(reverbEnabled, reverbPreset)
                    Constants.PREF_STEREOWIDE -> setStereoEnhancement(swEnabled, swMode)
                    Constants.PREF_CROSSFEED -> setCrossfeed(crossfeedEnabled, crossfeedMode)
                    Constants.PREF_TUBE -> setVacuumTube(tubeEnabled, tubeDrive)
                    Constants.PREF_DDC -> setVdc(ddcEnabled, ddcFile)
                    Constants.PREF_LIVEPROG -> setLiveprog(liveProgEnabled, liveprogFile)
                    Constants.PREF_CONVOLVER -> setConvolver(convolverEnabled, convolverFile, convolverMode, convolverAdvImp)
                }
            }

            cache.markChangesAsCommitted()
            Timber.tag(TAG).i("Preferences synchronized")
        }
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
        for((i, str) in array.withIndex())
        {
            val number = str.toDoubleOrNull()
            if(number == null) {
                Timber.tag(TAG).e("setFirEqualizer: malformed EQ string")
                return
            }
            doubleArray[i] = number
        }

        JamesDspWrapper.setFirEqualizer(handle, enable, filterType, interpolationMode, doubleArray)
    }

    fun setVdc(enable: Boolean, vdcPath: String)
    {
        if(!File(vdcPath).exists()) {
            Timber.tag(TAG).w("setVdc: file does not exist")
            JamesDspWrapper.setVdc(handle, false, "")
            return
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
            return
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
            return
        }

        val reader = FileReader(path)
        val name = File(path).name
        JamesDspWrapper.setLiveprog(handle, enable, name, reader.readText())
        reader.close()
    }

    // EEL VM utilities
    fun enumerateEelVariables(): ArrayList<EelVmVariable>
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