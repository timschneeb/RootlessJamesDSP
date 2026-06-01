package me.timschneeberger.rootlessjamesdsp.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.*
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.RecyclerView
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.LiveprogParamsActivity
import me.timschneeberger.rootlessjamesdsp.adapter.RoundedRipplePreferenceGroupAdapter
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspLocalEngine
import me.timschneeberger.rootlessjamesdsp.interop.PreferenceCache
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import me.timschneeberger.rootlessjamesdsp.liveprog.EelListProperty
import me.timschneeberger.rootlessjamesdsp.liveprog.EelNumberRangeProperty
import me.timschneeberger.rootlessjamesdsp.liveprog.EelParser
import me.timschneeberger.rootlessjamesdsp.preference.DropDownPreference
import me.timschneeberger.rootlessjamesdsp.preference.MaterialSeekbarPreference
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.preferences.NonPersistentDatastore
import timber.log.Timber

class LiveprogParamsFragment : PreferenceFragmentCompat(), NonPersistentDatastore.OnPreferenceChanged {
    private val eelParser = EelParser()
    private val dataStore = NonPersistentDatastore()
    private var isCreated = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        (savedInstanceState?.getString(BUNDLE_TARGET_FILE)
            ?: requireArguments().getString(BUNDLE_TARGET_FILE))
            ?.let { eelParser.load(it) }

        if(!eelParser.isFileLoaded) {
            requireContext().toast(R.string.liveprog_not_found)
            return
        }

        loadAndApplyParameters()

        requireActivity().title = eelParser.description
        updateResetMenuItem()

        dataStore.setOnPreferenceChanged(this)
        preferenceManager.preferenceDataStore = dataStore
        preferenceScreen = createPreferences()

        isCreated = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState.apply {
            putString(BUNDLE_TARGET_FILE, eelParser.path)
        })
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        return RoundedRipplePreferenceGroupAdapter(preferenceScreen)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onFloatPreferenceChanged(key: String, value: Float) {
        if(!isCreated) {
            Timber.d("onFloatPreferenceChanged not yet ready")
            return
        }

        Timber.d("onFloatPreferenceChanged: $key=$value")

        val baseProp = eelParser.properties.find { it.key == key }
        val index = eelParser.properties.indexOf(baseProp)
        if(baseProp is EelListProperty)
            return

        (baseProp as? EelNumberRangeProperty<Float>)?.apply {
            this.value = value
            saveSlidersToPrefs()

            if (index >= 0) {
                JamesDspLocalEngine.activeInstance?.setSlider(index, value.toDouble())
            }
        }

        updateResetMenuItem()
    }

    private fun createPreferences(): PreferenceScreen {
        val screen = preferenceManager.createPreferenceScreen(requireContext())

        eelParser.properties.forEach { prop ->
            if(prop is EelListProperty) {
                DropDownPreference(requireContext()).apply {
                    key = prop.key
                    title = prop.description
                    setDefaultValue(prop.value.toString())
                    entries = prop.options.toTypedArray()
                    entryValues = (0 until(prop.options.size)).toList().map { it.toString() }.toTypedArray()
                    setValueIndex(prop.validateRange(prop.value))
                    setOnPreferenceChangeListener { _, newValue ->
                        if(!isCreated) {
                            Timber.d("onPreferenceChangeListener not yet ready")
                            return@setOnPreferenceChangeListener false
                        }

                        val currentProp = eelParser.properties.find { it.key == prop.key } as? EelListProperty
                        currentProp ?: return@setOnPreferenceChangeListener false
                        val index = eelParser.properties.indexOf(currentProp)

                        Timber.d("List item with value $newValue selected")

                        currentProp.value = (newValue as? String)?.toIntOrNull() ?: 0
                        saveSlidersToPrefs()

                        if (index >= 0) {
                            JamesDspLocalEngine.activeInstance?.setSlider(index, currentProp.value.toDouble())
                        }

                        updateResetMenuItem()
                        true
                    }
                }.let(screen::addPreference)
            }
            else if(prop is EelNumberRangeProperty<*>) {
                MaterialSeekbarPreference(requireContext()).apply {
                    key = prop.key
                    title = prop.description
                    mPrecision = if(prop.handleAsInt()) 0 else 2
                    setMin(prop.minimum.toFloat())
                    setMax(prop.maximum.toFloat())
                    setUpdatesContinuously(false)
                    setShowSeekBarValue(true)
                    setDefaultValue(prop.value)
                }.let(screen::addPreference)
            }
        }

        return screen
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?,
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        recyclerView.itemAnimator = null // Fix to prevent RecyclerView crash if group is toggled rapidly
        recyclerView.isNestedScrollingEnabled = false
        return recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.setDivider(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requireContext().registerLocalReceiver(broadcastReceiver, IntentFilter(Constants.ACTION_PRESET_LOADED))
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        requireContext().unregisterLocalReceiver(broadcastReceiver)
        super.onDestroy()
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                Constants.ACTION_PRESET_LOADED -> reload()
            }
        }
    }

    private fun updateResetMenuItem() {
        val act = requireActivity() as LiveprogParamsActivity
        act.setResetEnabled(eelParser.canLoadDefaults())
        act.setResetVisible(eelParser.hasDefaults())
    }

    private fun reload() {
        val newPath = context?.let { ctx ->
            ctx.getExternalFilesDir(null)!!.absolutePath + "/" + PreferenceCache.uncachedGet(
                ctx,
                Constants.PREF_LIVEPROG,
                R.string.key_liveprog_file,
                ""
            )
        }

        if(newPath != eelParser.path && newPath != null) {
            Timber.d("Liveprog path changed. Switching to $newPath")
            eelParser.load(newPath, true)
        }
        else {
            eelParser.refresh()
        }

        loadAndApplyParameters()

        activity?.title = eelParser.description
        if(eelParser.properties.isEmpty())
            activity?.finish()

        updateResetMenuItem()

        isCreated = false
        preferenceScreen = createPreferences()
        isCreated = true
    }

    fun restoreDefaults() {
        requireContext().getSharedPreferences(Constants.PREF_LIVEPROG, Context.MODE_PRIVATE)
            .edit()
            .remove(getString(R.string.key_liveprog_sliders))
            .remove(getParamsKey())
            .apply()

        reload()
        
        // Push initial defaults to engine
        eelParser.properties.forEachIndexed { index, prop ->
            JamesDspLocalEngine.activeInstance?.setSlider(index, prop.getNumericValue())
        }
    }

    private fun getParamsKey() = "liveprog_params_${eelParser.getScriptIdentity()}"

    private fun loadAndApplyParameters() {
        val sharedPrefs = requireContext().getSharedPreferences(Constants.PREF_LIVEPROG, Context.MODE_PRIVATE)
        val paramsKey = getParamsKey()
        
        if (sharedPrefs.contains(paramsKey)) {
            val json = sharedPrefs.getString(paramsKey, "{}")
            try {
                val type = object : TypeToken<Map<String, Double>>() {}.type
                val map: Map<String, Double> = Gson().fromJson(json, type)
                eelParser.applyNamedSliders(map)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse named parameters")
            }
        } else {
            // Attempt migration from filename-based key if we have an explicit @id
            val fileNameIdentity = eelParser.fileName ?: "unknown"
            val fallbackKey = "liveprog_params_$fileNameIdentity"
            
            if (eelParser.scriptId != null && sharedPrefs.contains(fallbackKey)) {
                Timber.i("Migrating LiveProg parameters from filename to @id for ${eelParser.getScriptIdentity()}")
                val json = sharedPrefs.getString(fallbackKey, "{}")
                try {
                    val type = object : TypeToken<Map<String, Double>>() {}.type
                    val map: Map<String, Double> = Gson().fromJson(json, type)
                    eelParser.applyNamedSliders(map)
                    saveParameters() // Save in new format
                    return
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse named parameters during filename-to-id migration")
                }
            }

            // Fallback to legacy index-based sliders migration
            val oldSliders = sharedPrefs.getString(getString(R.string.key_liveprog_sliders), "")
            if (!oldSliders.isNullOrEmpty()) {
                Timber.i("Migrating LiveProg parameters from legacy string for ${eelParser.getScriptIdentity()}")
                eelParser.applySliders(oldSliders)
                saveParameters() // Save in new format immediately
            }
        }
    }

    private fun saveParameters() {
        val map = eelParser.properties.associate { it.key to it.getNumericValue() }
        val json = Gson().toJson(map)
        
        requireContext().getSharedPreferences(Constants.PREF_LIVEPROG, Context.MODE_PRIVATE)
            .edit()
            .putString(getParamsKey(), json)
            .apply()
    }

    private fun saveSlidersToPrefs() {
        saveParameters()
        
        // Keep index-based version for engine sync compatibility
        val sliders = eelParser.properties.joinToString(";") { it.getNumericValue().toString() }
        requireContext().getSharedPreferences(Constants.PREF_LIVEPROG, Context.MODE_PRIVATE)
            .edit()
            .putString(getString(R.string.key_liveprog_sliders), sliders)
            .apply()
    }

    companion object {
        private const val BUNDLE_TARGET_FILE = "TargetFile"

        fun newInstance(targetFile: String): LiveprogParamsFragment {
            val fragment = LiveprogParamsFragment()

            val args = Bundle()
            args.putString(BUNDLE_TARGET_FILE, targetFile)
            fragment.arguments = args
            return fragment
        }
    }
}