package me.timschneeberger.rootlessjamesdsp.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.XmlRes
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.preference.EqualizerPreference
import me.timschneeberger.rootlessjamesdsp.preference.FileLibraryPreference
import me.timschneeberger.rootlessjamesdsp.preference.MaterialSeekbarPreference
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.sendLocalBroadcast

class PreferenceGroupFragment : PreferenceFragmentCompat() {
    private val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            requireContext().sendLocalBroadcast(Intent(Constants.ACTION_UPDATE_PREFERENCES))
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val args = requireArguments()
        preferenceManager.sharedPreferencesName = args.getString(BUNDLE_PREF_NAME)
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        addPreferencesFromResource(args.getInt(BUNDLE_XML_RES))

        if(args.getInt(BUNDLE_XML_RES) == R.xml.dsp_stereowide_preferences)
        {
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
        else if(args.getInt(BUNDLE_XML_RES) == R.xml.dsp_graphiceq_preferences) {
            findPreference<EditTextPreference>(getString(R.string.key_geq_nodes))?.summaryProvider = SummaryProvider<EditTextPreference> {
                val number = it.text?.split(";")?.count { sub -> sub.isNotBlank() } ?: 0
                requireContext().resources.getQuantityString(R.plurals.nodes, number, number)
            }
        }

        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        recyclerView.itemAnimator = null // Fix to prevent RecyclerView crash if group is toggled rapidly
        recyclerView.isNestedScrollingEnabled = false
        return recyclerView
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(listener)
    }

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

        fun newInstance(preferencesName: String, @XmlRes preferencesXmlRes: Int): PreferenceGroupFragment {
            val fragment = PreferenceGroupFragment()

            val args = Bundle()
            args.putString(BUNDLE_PREF_NAME, preferencesName)
            args.putInt(BUNDLE_XML_RES, preferencesXmlRes)
            fragment.arguments = args
            return fragment
        }
    }
}