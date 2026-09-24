package me.timschneeberger.rootlessjamesdsp.fragment.settings

import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.analysis.MNoiseFileVerifier
import me.timschneeberger.rootlessjamesdsp.analysis.ReferenceFileHasher
import me.timschneeberger.rootlessjamesdsp.analysis.TonalityReferenceBuilder
import me.timschneeberger.rootlessjamesdsp.analysis.TonalityReferenceFactory
import me.timschneeberger.rootlessjamesdsp.analysis.TonalityReferenceStore
import me.timschneeberger.rootlessjamesdsp.analysis.WavPcmDecoder
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import org.koin.android.ext.android.inject
import timber.log.Timber

class SettingsTonalityFragment : SettingsBaseFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val referenceMode by lazy { findPreference<ListPreference>(getString(R.string.key_tonality_reference_mode)) }
    private val tilt by lazy { findPreference<ListPreference>(getString(R.string.key_tonality_tilt)) }
    private val customStatus by lazy { findPreference<Preference>(getString(R.string.key_tonality_reference_status)) }
    private val importReference by lazy { findPreference<Preference>(getString(R.string.key_tonality_import_reference)) }
    private val clearReference by lazy { findPreference<Preference>(getString(R.string.key_tonality_clear_reference)) }
    private val libraryStatus by lazy { findPreference<Preference>(getString(R.string.key_tonality_library_status)) }
    private val libraryClear by lazy { findPreference<Preference>(getString(R.string.key_tonality_library_clear)) }

    private val preferences: Preferences.App by inject()

    private val referenceImportLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        importWav(uri)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = Constants.PREF_APP
        setPreferencesFromResource(R.xml.app_tonality_preferences, rootKey)

        // Values saved by earlier versions
        referenceMode?.let { mode ->
            val normalized = TonalityReferenceStore.normalizeMode(mode.value)
            if (normalized != mode.value) mode.value = normalized
        }

        importReference?.setOnPreferenceClickListener {
            referenceImportLauncher.launch(arrayOf("audio/wav", "audio/x-wav", "audio/*", "application/octet-stream"))
            true
        }

        clearReference?.setOnPreferenceClickListener {
            TonalityReferenceStore(requireContext()).clearCustom()
            if (referenceMode?.value == TonalityReferenceStore.MODE_CUSTOM)
                referenceMode?.value = TonalityReferenceStore.MODE_IEC
            notifyReferenceChanged(preferences)
            requireContext().toast(R.string.tonality_clear_reference_done)
            updateSummaries()
            true
        }

        libraryClear?.setOnPreferenceClickListener {
            TonalityReferenceStore(requireContext()).clearLibrary()
            notifyReferenceChanged(preferences)
            requireContext().toast(R.string.tonality_library_cleared)
            updateSummaries()
            true
        }

        updateSummaries()
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        updateSummaries()
    }

    override fun onPause() {
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == getString(R.string.key_tonality_reference_mode) || key == getString(R.string.key_tonality_tilt))
            updateSummaries()
    }

    private fun updateSummaries() {
        val context = context ?: return
        val store = TonalityReferenceStore(context)
        val mode = TonalityReferenceStore.normalizeMode(referenceMode?.value)
        val labels = resources.getStringArray(R.array.tonality_reference_modes)
        val values = resources.getStringArray(R.array.tonality_reference_modes_values)
        val label = labels.getOrElse(values.indexOf(mode)) { labels[0] }
        referenceMode?.summary = label + "\n" + referenceSummary(context, store, mode, preferences)

        tilt?.isVisible = mode == TonalityReferenceStore.MODE_TILT
        val customName = store.customName()
        customStatus?.summary = customName ?: getString(R.string.tonality_reference_custom_none)
        clearReference?.isEnabled = customName != null

        val songs = store.librarySongCount()
        libraryStatus?.summary = getString(R.string.tonality_library_count, songs, TonalityReferenceStore.MIN_LIBRARY_SONGS)
        libraryClear?.isEnabled = songs > 0
    }

    private fun importWav(uri: Uri) {
        importReference?.isEnabled = false
        customStatus?.summary = getString(R.string.tonality_import_running)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val verified = withContext(Dispatchers.IO) {
                    val context = requireContext().applicationContext
                    val resolver = context.contentResolver
                    val displayName = displayNameFor(uri) ?: "Imported WAV"
                    val hashes = ReferenceFileHasher.hash(resolver, uri)
                    val verified = MNoiseFileVerifier.isVerifiedMNoise(hashes.sha256, hashes.md5)
                    val decoded = resolver.openInputStream(uri)?.use {
                        WavPcmDecoder.decode(it, displayName)
                    } ?: throw IllegalArgumentException("Unable to open reference file")
                    val builder = TonalityReferenceBuilder()
                    val store = TonalityReferenceStore(context)
                    for (sampleRate in TonalityReferenceStore.SAMPLE_RATES)
                        store.saveCustom(builder.build(decoded, sampleRate, hashes, verified))
                    verified
                }

                val context = context ?: return@launch
                referenceMode?.value = TonalityReferenceStore.MODE_CUSTOM
                notifyReferenceChanged(preferences)
                context.toast(if (verified) R.string.tonality_import_verified else R.string.tonality_import_unverified)
            }
            catch (e: CancellationException) {
                throw e
            }
            catch (e: OutOfMemoryError) {
                Timber.e(e, "Reference file too large")
                context?.toast(getString(R.string.tonality_import_error, getString(R.string.tonality_import_too_large)))
            }
            catch (e: Exception) {
                Timber.e(e, "Failed to import tonality reference")
                context?.toast(getString(R.string.tonality_import_error, e.message ?: e.javaClass.simpleName))
            }
            finally {
                importReference?.isEnabled = true
                updateSummaries()
            }
        }
    }

    private fun displayNameFor(uri: Uri): String? {
        val resolver = requireContext().contentResolver
        val cursor: Cursor? = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return it.getString(index)?.substringBeforeLast('.')
            }
        }
        return uri.lastPathSegment
    }

    companion object {
        fun newInstance() = SettingsTonalityFragment()

        /** A new value makes a running analyzer reload its reference, even when the mode stays the same. */
        fun notifyReferenceChanged(preferences: Preferences.App) {
            preferences.set(R.string.key_tonality_reference_label, "changed@${System.currentTimeMillis()}")
        }

        /** One line describing [mode], for the settings list and the card's picker. */
        fun referenceSummary(context: android.content.Context, store: TonalityReferenceStore, mode: String, preferences: Preferences.App): String {
            val tilt = preferences.get<String>(R.string.key_tonality_tilt).toFloatOrNull() ?: TonalityReferenceFactory.DEFAULT_TILT_DB_PER_OCTAVE
            return when (mode) {
                TonalityReferenceStore.MODE_MNOISE -> context.getString(R.string.tonality_reference_mnoise_summary)
                TonalityReferenceStore.MODE_PINK -> context.getString(R.string.tonality_reference_pink_summary)
                TonalityReferenceStore.MODE_TILT -> context.getString(R.string.tonality_reference_tilt_summary,
                    String.format(java.util.Locale.ROOT, "%.1f dB/oct", tilt))
                TonalityReferenceStore.MODE_MUSIC_AVERAGE -> context.getString(R.string.tonality_reference_music_average_summary)
                TonalityReferenceStore.MODE_MUSIC_ACOUSTIC -> context.getString(R.string.tonality_reference_music_acoustic_summary)
                TonalityReferenceStore.MODE_MUSIC_PERCUSSIVE -> context.getString(R.string.tonality_reference_music_percussive_summary)
                TonalityReferenceStore.MODE_LIBRARY -> {
                    val songs = store.librarySongCount()
                    context.getString(R.string.tonality_reference_library_summary,
                        if (songs < TonalityReferenceStore.MIN_LIBRARY_SONGS)
                            context.getString(R.string.tonality_library_progress, songs, TonalityReferenceStore.MIN_LIBRARY_SONGS)
                        else context.getString(R.string.tonality_library_ready, songs))
                }
                TonalityReferenceStore.MODE_CUSTOM -> store.customName() ?: context.getString(R.string.tonality_reference_custom_none)
                else -> context.getString(R.string.tonality_reference_iec_summary)
            }
        }
    }
}
