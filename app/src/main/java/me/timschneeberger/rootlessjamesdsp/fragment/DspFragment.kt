package me.timschneeberger.rootlessjamesdsp.fragment

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.view.Card

class DspFragment : Fragment() {
    private val prefsVar by lazy { requireContext().getSharedPreferences(Constants.PREF_VAR, Context.MODE_PRIVATE) }

    private var translateNotice: Card? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dsp, container, false)
        translateNotice = view.findViewById(R.id.translation_notice)
        translateNotice?.setOnCloseClickListener(::hideTranslationNotice)
        translateNotice?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://crowdin.com/project/rootlessjamesdsp")))
            hideTranslationNotice()
        }

        // Should show translation notice?
        translateNotice?.isVisible =
            prefsVar.getLong(getString(R.string.key_snooze_translation_notice), 0L) < (System.currentTimeMillis() / 1000)

        val transition = LayoutTransition()
        transition.enableTransitionType(LayoutTransition.CHANGING)
        view.findViewById<ViewGroup>(R.id.card_container).layoutTransition = transition

        childFragmentManager.beginTransaction()
            .replace(
                R.id.card_output_control, PreferenceGroupFragment.newInstance(Constants.PREF_OUTPUT,
                    R.xml.dsp_output_control_preferences
                ))
            .replace(
                R.id.card_compressor, PreferenceGroupFragment.newInstance(Constants.PREF_COMPRESSOR,
                    R.xml.dsp_compressor_preferences
                ))
            .replace(
                R.id.card_bass, PreferenceGroupFragment.newInstance(Constants.PREF_BASS,
                    R.xml.dsp_bass_preferences
                ))
            .replace(
                R.id.card_eq, PreferenceGroupFragment.newInstance(Constants.PREF_EQ,
                    R.xml.dsp_equalizer_preferences
                ))
            .replace(
                R.id.card_geq, PreferenceGroupFragment.newInstance(Constants.PREF_GEQ,
                    R.xml.dsp_graphiceq_preferences
                ))
            .replace(
                R.id.card_ddc, PreferenceGroupFragment.newInstance(Constants.PREF_DDC,
                    R.xml.dsp_ddc_preferences
                ))
            .replace(
                R.id.card_convolver, PreferenceGroupFragment.newInstance(Constants.PREF_CONVOLVER,
                    R.xml.dsp_convolver_preferences
                ))
            .replace(
                R.id.card_liveprog, PreferenceGroupFragment.newInstance(Constants.PREF_LIVEPROG,
                    R.xml.dsp_liveprog_preferences
                ))
            .replace(
                R.id.card_tube, PreferenceGroupFragment.newInstance(Constants.PREF_TUBE,
                    R.xml.dsp_tube_preferences
                ))
            .replace(
                R.id.card_stereowide, PreferenceGroupFragment.newInstance(Constants.PREF_STEREOWIDE,
                    R.xml.dsp_stereowide_preferences
                ))
            .replace(
                R.id.card_crossfeed, PreferenceGroupFragment.newInstance(Constants.PREF_CROSSFEED,
                    R.xml.dsp_crossfeed_preferences
                ))
            .replace(
                R.id.card_reverb, PreferenceGroupFragment.newInstance(Constants.PREF_REVERB,
                    R.xml.dsp_reverb_preferences
                ))
            .commit()
        return view
    }

    private fun hideTranslationNotice() {
        translateNotice?.isVisible = false
        // Set timer +1y
        prefsVar.edit()
            .putLong(getString(R.string.key_snooze_translation_notice), (System.currentTimeMillis() / 1000) + 31536000)
            .apply()
    }

    fun restartFragment(id: Int, newFragment: Fragment) {
        try {
            childFragmentManager.beginTransaction()
                .replace(id, newFragment)
                .commit()
        }
        catch(_: IllegalStateException) {}
    }

    companion object {
        fun newInstance(): DspFragment {
            return DspFragment()
        }
    }
}