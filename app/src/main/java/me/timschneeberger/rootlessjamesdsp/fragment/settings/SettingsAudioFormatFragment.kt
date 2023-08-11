package me.timschneeberger.rootlessjamesdsp.fragment.settings

import android.content.Intent
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.OnboardingActivity
import me.timschneeberger.rootlessjamesdsp.interop.BenchmarkManager
import me.timschneeberger.rootlessjamesdsp.preference.MaterialSeekbarPreference
import me.timschneeberger.rootlessjamesdsp.preference.MaterialSwitchPreference
import me.timschneeberger.rootlessjamesdsp.service.RootAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.requestIgnoreBatteryOptimizations
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.PermissionExtensions.hasDumpPermission
import me.timschneeberger.rootlessjamesdsp.utils.isRoot
import me.timschneeberger.rootlessjamesdsp.utils.isRootless
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import org.koin.android.ext.android.inject
import timber.log.Timber

class SettingsAudioFormatFragment : SettingsBaseFragment() {

    private val encoding by lazy { findPreference<ListPreference>(getString(R.string.key_audioformat_encoding)) }
    private val bufferSize by lazy { findPreference<MaterialSeekbarPreference>(getString(R.string.key_audioformat_buffersize)) }
    private val legacyMode by lazy { findPreference<MaterialSwitchPreference>(getString(R.string.key_audioformat_processing)) }
    private val enhancedMode by lazy { findPreference<MaterialSwitchPreference>(getString(R.string.key_audioformat_enhanced_processing)) }
    private val enhancedModeInfo by lazy { findPreference<Preference>(getString(R.string.key_audioformat_enhanced_processing_info)) }
    private val benchmark by lazy { findPreference<MaterialSwitchPreference>(getString(R.string.key_audioformat_optimization_benchmark)) }
    private val benchmarkRefresh by lazy { findPreference<Preference>(getString(R.string.key_audioformat_optimization_refresh)) }

    private val preferences: Preferences.App by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = Constants.PREF_APP
        setPreferencesFromResource(R.xml.app_audio_format_preferences, rootKey)

        // Root: Hide audio format & benchmark category
        encoding?.parent?.isVisible = isRootless()
        benchmark?.parent?.isVisible = !isRoot()

        // Rootless: Hide audio processing category
        legacyMode?.parent?.isVisible = isRoot()
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

        fun runBenchmark() = context?.let { ctx ->
            BenchmarkManager.runBenchmarks(ctx) {
                CoroutineScope(Dispatchers.Main).launch {
                    benchmark?.isChecked = BenchmarkManager.hasBenchmarksCached()
                }
            }
        }

        benchmark?.isChecked = BenchmarkManager.hasBenchmarksCached()
        benchmark?.setOnPreferenceChangeListener { _, newValue ->
            if(newValue as Boolean)
                runBenchmark()
            else
                BenchmarkManager.clearBenchmarks()
            true
        }
        benchmarkRefresh?.setOnPreferenceClickListener {
            runBenchmark()
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

    companion object {
        fun newInstance(): SettingsAudioFormatFragment {
            return SettingsAudioFormatFragment()
        }
    }
}