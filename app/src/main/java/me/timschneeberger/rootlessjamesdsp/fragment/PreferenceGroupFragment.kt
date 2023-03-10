package me.timschneeberger.rootlessjamesdsp.fragment

import android.content.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.XmlRes
import androidx.preference.*
import androidx.preference.Preference.SummaryProvider
import androidx.recyclerview.widget.RecyclerView
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.GraphicEqualizerActivity
import me.timschneeberger.rootlessjamesdsp.activity.LiveprogEditorActivity
import me.timschneeberger.rootlessjamesdsp.activity.LiveprogParamsActivity
import me.timschneeberger.rootlessjamesdsp.adapter.RoundedRipplePreferenceGroupAdapter
import me.timschneeberger.rootlessjamesdsp.liveprog.EelParser
import me.timschneeberger.rootlessjamesdsp.preference.EqualizerPreference
import me.timschneeberger.rootlessjamesdsp.preference.FileLibraryPreference
import me.timschneeberger.rootlessjamesdsp.preference.MaterialSeekbarPreference
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import timber.log.Timber


class PreferenceGroupFragment : PreferenceFragmentCompat() {
    private val eelParser = EelParser()
    private var recyclerView: RecyclerView? = null

    private val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            requireContext().sendLocalBroadcast(Intent(Constants.ACTION_PREFERENCES_UPDATED))
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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val args = requireArguments()
        preferenceManager.sharedPreferencesName = args.getString(BUNDLE_PREF_NAME)
        @Suppress("DEPRECATION")
        preferenceManager.sharedPreferencesMode = Context.MODE_MULTI_PROCESS
        addPreferencesFromResource(args.getInt(BUNDLE_XML_RES))

        requireContext().registerLocalReceiver(receiver, IntentFilter(Constants.ACTION_PRESET_LOADED))

        when(args.getInt(BUNDLE_XML_RES)) {
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
                        "No script selected"
                    }
                    else
                        eelParser.description
                }

                if(liveprogFile != null) {
                    updateLiveprog(liveprogFile.value)
                }

                liveprogFile?.setOnPreferenceChangeListener { _, newValue ->
                    updateLiveprog(newValue as String)
                    true
                }

                liveprogParams?.setOnPreferenceClickListener {
                    val intent = Intent(requireContext(), LiveprogParamsActivity::class.java)
                    intent.putExtra(LiveprogParamsActivity.EXTRA_TARGET_FILE, liveprogFile?.value)
                    startActivity(intent)
                    true
                }

                liveprogEdit?.setOnPreferenceClickListener {
                    val intent = Intent(requireContext(), LiveprogEditorActivity::class.java)
                    intent.putExtra(LiveprogEditorActivity.EXTRA_TARGET_FILE, liveprogFile?.value)
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

        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?,
    ): RecyclerView {
        recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        recyclerView!!.itemAnimator = null // Fix to prevent RecyclerView crash if group is toggled rapidly
        recyclerView!!.isNestedScrollingEnabled = false
        return recyclerView!!
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        return RoundedRipplePreferenceGroupAdapter(preferenceScreen)
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterLocalReceiver(receiver)
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
            val fragment = PreferenceGroupFragment()

            val args = Bundle()
            args.putString(BUNDLE_PREF_NAME, preferencesName)
            args.putInt(BUNDLE_XML_RES, preferencesXmlRes)
            fragment.arguments = args
            return fragment
        }

        fun cloneInstance(fragment: PreferenceGroupFragment): PreferenceGroupFragment {
            val args = fragment.requireArguments()
            return newInstance(args.getString(BUNDLE_PREF_NAME), args.getInt(BUNDLE_XML_RES))
        }
    }
}