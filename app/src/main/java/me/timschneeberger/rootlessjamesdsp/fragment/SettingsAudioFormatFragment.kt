package me.timschneeberger.rootlessjamesdsp.fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.preference.*
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.preference.MaterialSeekbarPreference
import me.timschneeberger.rootlessjamesdsp.preference.MaterialSwitchPreference
import me.timschneeberger.rootlessjamesdsp.service.RootAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.requestIgnoreBatteryOptimizations
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.sendLocalBroadcast

class SettingsAudioFormatFragment : PreferenceFragmentCompat() {

    private val encoding by lazy { findPreference<ListPreference>(getString(R.string.key_audioformat_encoding)) }
    private val bufferSize by lazy { findPreference<MaterialSeekbarPreference>(getString(R.string.key_audioformat_buffersize)) }
    private val legacyMode by lazy { findPreference<MaterialSwitchPreference>(getString(R.string.key_audioformat_legacymode)) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = Constants.PREF_APP
        setPreferencesFromResource(R.xml.app_audio_format_preferences, rootKey)

        // Root: Hide audio format category
        encoding?.parent?.isVisible = BuildConfig.ROOTLESS

        // Rootless: Hide audio processing category
        legacyMode?.parent?.isVisible = !BuildConfig.ROOTLESS
        legacyMode?.setOnPreferenceChangeListener { _, newValue ->
            if (!(newValue as Boolean))
                requireContext().requestIgnoreBatteryOptimizations()
            RootAudioProcessorService.updateLegacyMode(requireContext(), newValue)
            true
        }

        bufferSize?.setOnPreferenceChangeListener { _, newValue ->
            if((newValue as Float) <= 1024){
                Toast.makeText(requireContext(), getString(R.string.audio_format_buffer_size_warning_low_value), Toast.LENGTH_SHORT).show()
            }
            context?.sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_HARD_REBOOT_CORE))
            true
        }
        encoding?.setOnPreferenceChangeListener { _, _ ->
            context?.sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_HARD_REBOOT_CORE))
            true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val a = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.windowBackground, a, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && a.isColorType) {
            view.setBackgroundColor(a.data)
        } else {
            view.background = ResourcesCompat.getDrawable(requireContext().resources, a.resourceId, requireContext().theme)
        }
        return view
    }

    companion object {
        fun newInstance(): SettingsAudioFormatFragment {
            return SettingsAudioFormatFragment()
        }
    }
}