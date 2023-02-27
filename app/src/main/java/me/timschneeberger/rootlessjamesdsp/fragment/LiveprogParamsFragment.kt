package me.timschneeberger.rootlessjamesdsp.fragment

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.RecyclerView
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.LiveprogParamsActivity
import me.timschneeberger.rootlessjamesdsp.adapter.RoundedRipplePreferenceGroupAdapter
import me.timschneeberger.rootlessjamesdsp.liveprog.EelListProperty
import me.timschneeberger.rootlessjamesdsp.liveprog.EelNumberRangeProperty
import me.timschneeberger.rootlessjamesdsp.liveprog.EelParser
import me.timschneeberger.rootlessjamesdsp.preference.DropDownPreference
import me.timschneeberger.rootlessjamesdsp.preference.MaterialSeekbarPreference
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.NonPersistentDatastore
import timber.log.Timber

class LiveprogParamsFragment : PreferenceFragmentCompat(), NonPersistentDatastore.OnPreferenceChanged {
    private val eelParser = EelParser()
    private val dataStore = NonPersistentDatastore()
    private var isCreated = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val args = requireArguments()
        args.getString(BUNDLE_TARGET_FILE)?.let { eelParser.load(it) }
        if(!eelParser.isFileLoaded) {
            Toast.makeText(requireContext(), getString(R.string.liveprog_not_found), Toast.LENGTH_LONG).show()
            return
        }

        requireActivity().title = eelParser.description
        updateResetMenuItem()

        dataStore.setOnPreferenceChanged(this)
        preferenceManager.preferenceDataStore = dataStore
        preferenceScreen = createPreferences()

        isCreated = true
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
        if(baseProp is EelListProperty)
            return

        val prop = baseProp as? EelNumberRangeProperty<Float>
        prop ?: return
        prop.value = value
        eelParser.manipulateProperty(prop)

        updateResetMenuItem()

        requireContext().sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_RELOAD_LIVEPROG))
    }

    private fun createPreferences(): PreferenceScreen {
        val screen = preferenceManager.createPreferenceScreen(requireContext())

        eelParser.properties.forEach { prop ->
            if(prop is EelListProperty) {
                val preference = DropDownPreference(requireContext())
                preference.key = prop.key
                preference.title = prop.description
                //preference.summary = prop.options.getOrNull(prop.value) ?: getString(R.string.value_not_set);
                preference.setDefaultValue(prop.value.toString())
                preference.entries = prop.options.toTypedArray()
                preference.entryValues = (0 until(prop.options.size)).toList().map { it.toString() }.toTypedArray()
                preference.setValueIndex(prop.validateRange(prop.value))
                preference.setOnPreferenceChangeListener { _, newValue ->
                    if(!isCreated) {
                        Timber.d("onPreferenceChangeListener not yet ready")
                        return@setOnPreferenceChangeListener false
                    }

                    val currentProp = eelParser.properties.find { it.key == prop.key } as? EelListProperty
                    currentProp ?: return@setOnPreferenceChangeListener false

                    Timber.d("List item with value $newValue selected")

                    currentProp.value = (newValue as? String)?.toIntOrNull() ?: 0
                    eelParser.manipulateProperty(currentProp)

                    updateResetMenuItem()
                    requireContext().sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_RELOAD_LIVEPROG))
                    true
                }
                screen.addPreference(preference)
            }
            else if(prop is EelNumberRangeProperty<*>) {
                val slider = MaterialSeekbarPreference(requireContext())
                slider.key = prop.key
                slider.title = prop.description
                slider.mPrecision = if(prop.handleAsInt()) 0 else 2
                slider.setMin(prop.minimum.toFloat())
                slider.setMax(prop.maximum.toFloat())
                slider.setUpdatesContinuously(false)
                slider.setShowSeekBarValue(true)
                slider.setDefaultValue(prop.value)
                screen.addPreference(slider)
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

    private fun updateResetMenuItem() {
        val act = requireActivity() as LiveprogParamsActivity
        act.setResetEnabled(eelParser.canLoadDefaults())
        act.setResetVisible(eelParser.hasDefaults())
    }

    fun restoreDefaults() {
        eelParser.restoreDefaults()
        updateResetMenuItem()

        isCreated = false
        preferenceScreen = createPreferences()
        isCreated = true

        requireContext().sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_RELOAD_LIVEPROG))
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