package me.timschneeberger.rootlessjamesdsp.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.OnboardingActivity
import me.timschneeberger.rootlessjamesdsp.preference.MaterialSeekbarPreference
import me.timschneeberger.rootlessjamesdsp.preference.MaterialSwitchPreference
import me.timschneeberger.rootlessjamesdsp.service.RootAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.requestIgnoreBatteryOptimizations
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.showAlert
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.toast
import me.timschneeberger.rootlessjamesdsp.utils.PermissionExtensions.hasDumpPermission
import me.timschneeberger.rootlessjamesdsp.utils.Preferences
import org.koin.android.ext.android.inject
import timber.log.Timber

class SettingsAudioFormatFragment : PreferenceFragmentCompat() {

    private val encoding by lazy { findPreference<ListPreference>(getString(R.string.key_audioformat_encoding)) }
    private val bufferSize by lazy { findPreference<MaterialSeekbarPreference>(getString(R.string.key_audioformat_buffersize)) }
    private val legacyMode by lazy { findPreference<MaterialSwitchPreference>(getString(R.string.key_audioformat_processing)) }
    private val enhancedMode by lazy { findPreference<MaterialSwitchPreference>(getString(R.string.key_audioformat_enhanced_processing)) }
    private val enhancedModeInfo by lazy { findPreference<Preference>(getString(R.string.key_audioformat_enhanced_processing_info)) }

    private val preferences: Preferences.App by inject()

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
            else
                enhancedMode?.isChecked = false

            RootAudioProcessorService.updateLegacyMode(requireContext(), newValue)
            true
        }
        enhancedMode?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                // Check DUMP permissions
                if(!requireContext().hasDumpPermission()) {
                    Timber.i("Launching enhanced processing onboarding")

                    Intent(requireContext(), OnboardingActivity::class.java).let {
                        it.putExtra(OnboardingActivity.EXTRA_ROOT_SETUP_DUMP_PERM, true)
                        startActivity(it)
                    }
                    return@setOnPreferenceChangeListener false
                }

                RootAudioProcessorService.startServiceEnhanced(requireContext())
            }
            else {
                requireContext().toast(R.string.audio_format_media_apps_need_restart)
                RootAudioProcessorService.stopService(requireContext())
            }
            true
        }
        enhancedModeInfo?.setOnPreferenceClickListener {
            context?.showAlert(
                R.string.audio_format_enhanced_processing_info_title,
                R.string.audio_format_enhanced_processing_info_content
            )
            true
        }

        bufferSize?.setDefaultValue(preferences.getDefault<Float>(R.string.key_audioformat_buffersize))
        bufferSize?.setOnPreferenceChangeListener { _, newValue ->
            if((newValue as Float) <= 1024){
                requireContext().toast(R.string.audio_format_buffer_size_warning_low_value, false)
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