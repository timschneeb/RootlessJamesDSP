package me.timschneeberger.rootlessjamesdsp.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.XmlRes
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.RecyclerView
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.GraphicEqualizerActivity
import me.timschneeberger.rootlessjamesdsp.activity.LiveprogEditorActivity
import me.timschneeberger.rootlessjamesdsp.activity.LiveprogParamsActivity
import me.timschneeberger.rootlessjamesdsp.adapter.RoundedRipplePreferenceGroupAdapter
import me.timschneeberger.rootlessjamesdsp.liveprog.EelParser
import me.timschneeberger.rootlessjamesdsp.preference.CompanderPreference
import me.timschneeberger.rootlessjamesdsp.preference.EqualizerPreference
import me.timschneeberger.rootlessjamesdsp.preference.FileLibraryPreference
import me.timschneeberger.rootlessjamesdsp.preference.MaterialSeekbarPreference
import me.timschneeberger.rootlessjamesdsp.preference.SwitchPreferenceGroup
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import kotlin.math.roundToInt


class PreferenceGroupFragment : PreferenceFragmentCompat(), KoinComponent {
    private val prefsApp: Preferences.App by inject()
    private val eelParser = EelParser()
    private var recyclerView: RecyclerView? = null

    private val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            requireContext().sendLocalBroadcast(Intent(Constants.ACTION_PREFERENCES_UPDATED))
        }

    private val listenerApp =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when(key) {
                context?.resources?.getString(R.string.key_appearance_show_icons) -> updateIconState()
            }
        }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent?.action == Constants.ACTION_PRESET_LOADED) {
                val id = this@PreferenceGroupFragment.id
                Timber.d("Reloading group fragment for ${this@PreferenceGroupFragment.preferenceManager.sharedPreferencesName}")
                (requireParentFragment() as DspFragment).restartFragment(id, cloneInstance(this@PreferenceGroupFragment))
            }
        }
    }

    private fun updateIconState() {
        if(preferenceScreen.preferenceCount > 0) {
            (preferenceScreen.getPreference(0) as? SwitchPreferenceGroup?)
                ?.setIsIconVisible(prefsApp.get<Boolean>(R.string.key_appearance_show_icons))
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val args = requireArguments()
        preferenceManager.sharedPreferencesName = args.getString(BUNDLE_PREF_NAME)
        @Suppress("DEPRECATION")
        preferenceManager.sharedPreferencesMode = Context.MODE_MULTI_PROCESS
        addPreferencesFromResource(args.getInt(BUNDLE_XML_RES))

        requireContext().registerLocalReceiver(receiver, IntentFilter(Constants.ACTION_PRESET_LOADED))

        when(args.getInt(BUNDLE_XML_RES)) {
            R.xml.dsp_compander_preferences -> {
                findPreference<MaterialSeekbarPreference>(getString(R.string.key_compander_granularity))?.valueLabelOverride =
                    fun(it: Float): String {
                        return when(it.roundToInt()) {
                            0 -> getString(R.string.compander_granularity_very_low)
                            1 -> getString(R.string.compander_granularity_low)
                            2 -> getString(R.string.compander_granularity_medium)
                            3 -> getString(R.string.compander_granularity_high)
                            4 -> getString(R.string.compander_granularity_extreme)
                            else -> it.roundToInt().toString()
                        }
                    }
            }
            R.xml.dsp_stereowide_preferences -> {
                findPreference<MaterialSeekbarPreference>(getString(R.string.key_stereowide_mode))?.valueLabelOverride =
                    fun(it: Float): String {
                        return if (it in 49.0..51.0)
                            getString(R.string.stereowide_level_none)
                        else if(it >= 60)
                            getString(R.string.stereowide_level_very_wide)
                        else if(it >= 51)
                            getString(R.string.stereowide_level_wide)
                        else if(it <= 40)
                            getString(R.string.stereowide_level_very_narrow)
                        else if(it <= 49)
                            getString(R.string.stereowide_level_narrow)
                        else
                            it.toString()
                    }
            }
            R.xml.dsp_liveprog_preferences -> {
                val liveprogParams = findPreference<Preference>(getString(R.string.key_liveprog_params))
                val liveprogEdit = findPreference<Preference>(getString(R.string.key_liveprog_edit))
                val liveprogFile = findPreference<FileLibraryPreference>(getString(R.string.key_liveprog_file))

                fun updateLiveprog(newValue: String) {
                    eelParser.load(FileLibraryPreference.createFullPathCompat(requireContext(), newValue))
                    val count = eelParser.properties.size
                    val filePresent = eelParser.contents != null
                    val uiUpdate = {
                        liveprogEdit?.isEnabled = filePresent

                        liveprogParams?.isEnabled = count > 0

                        try {
                            liveprogParams?.summary = if (count > 0)
                                resources.getQuantityString(R.plurals.custom_parameters, count, count)
                            else
                                getString(R.string.liveprog_additional_params_not_supported)
                        }
                        catch(ex: IllegalStateException) {
                            /* Because this lambda is executed async, it is possible that it is called
                               while the fragment is destroyed, leading to accessing a detached context */
                            Timber.d(ex)
                        }
                    }

                    if (recyclerView == null)
                        // Recycler view doesn't exist yet, directly setup the preference
                        uiUpdate()
                    else
                        // Recycler view does exist, queue on UI thread
                        recyclerView!!.post(uiUpdate)
                }

                liveprogFile?.summaryProvider = SummaryProvider<FileLibraryPreference> {
                    updateLiveprog(it.value)
                    if(it.value == null || it.value.isBlank() || !eelParser.isFileLoaded) {
                        getString(R.string.liveprog_no_script_selected)
                    }
                    else
                        eelParser.description
                }

                FileLibraryPreference.createFullPathNullCompat(requireContext(), liveprogFile?.value)?.let {
                    updateLiveprog(it)
                }

                liveprogFile?.setOnPreferenceChangeListener { _, newValue ->
                    updateLiveprog(newValue as String)
                    true
                }

                liveprogParams?.setOnPreferenceClickListener {
                    val intent = Intent(requireContext(), LiveprogParamsActivity::class.java)
                    intent.putExtra(LiveprogParamsActivity.EXTRA_TARGET_FILE, FileLibraryPreference.createFullPathNullCompat(requireContext(), liveprogFile?.value))
                    startActivity(intent)
                    true
                }

                liveprogEdit?.setOnPreferenceClickListener {
                    val intent = Intent(requireContext(), LiveprogEditorActivity::class.java)
                    intent.putExtra(LiveprogEditorActivity.EXTRA_TARGET_FILE, FileLibraryPreference.createFullPathNullCompat(requireContext(), liveprogFile?.value))
                    startActivity(intent)
                    true
                }
            }
            R.xml.dsp_graphiceq_preferences -> {
                findPreference<Preference>(getString(R.string.key_geq_nodes))?.setOnPreferenceClickListener {
                    val intent = Intent(requireContext(), GraphicEqualizerActivity::class.java)
                    startActivity(intent)
                    true
                }
            }
        }

        updateIconState()

        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(listener)
        prefsApp.registerOnSharedPreferenceChangeListener(listenerApp)
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?,
    ): RecyclerView {
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState).apply {
            itemAnimator = null // Fix to prevent RecyclerView crash if group is toggled rapidly
            isNestedScrollingEnabled = false

            this@PreferenceGroupFragment.recyclerView = this
        }
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        return RoundedRipplePreferenceGroupAdapter(preferenceScreen)
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterLocalReceiver(receiver)
        prefsApp.unregisterOnSharedPreferenceChangeListener(listener)
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(listener)
    }

    @Suppress("DEPRECATION")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {
            is EqualizerPreference -> {
                val dialogFragment = EqualizerDialogFragment.newInstance(preference.key)
                dialogFragment.setTargetFragment(this, 0)
                dialogFragment.show(parentFragmentManager, null)
            }
            is CompanderPreference -> {
                val dialogFragment = CompanderDialogFragment.newInstance(preference.key)
                dialogFragment.setTargetFragment(this, 0)
                dialogFragment.show(parentFragmentManager, null)
            }
            is FileLibraryPreference -> {
                val dialogFragment = FileLibraryDialogFragment.newInstance(preference.key)
                dialogFragment.setTargetFragment(this, 0)
                dialogFragment.show(parentFragmentManager, null)
            }
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

    companion object {
        private const val BUNDLE_PREF_NAME = "preferencesName"
        private const val BUNDLE_XML_RES = "preferencesXmlRes"

        fun newInstance(preferencesName: String?, @XmlRes preferencesXmlRes: Int): PreferenceGroupFragment {
            return PreferenceGroupFragment().apply {
                arguments = Bundle().apply {
                    putString(BUNDLE_PREF_NAME, preferencesName)
                    putInt(BUNDLE_XML_RES, preferencesXmlRes)
                }
            }
        }

        fun cloneInstance(fragment: PreferenceGroupFragment): PreferenceGroupFragment {
            return fragment.requireArguments().let { args ->
                 newInstance(args.getString(BUNDLE_PREF_NAME), args.getInt(BUNDLE_XML_RES))
            }
        }
    }
}