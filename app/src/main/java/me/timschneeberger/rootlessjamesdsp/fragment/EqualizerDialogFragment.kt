package me.timschneeberger.rootlessjamesdsp.fragment

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.preference.PreferenceDialogFragmentCompat
import com.google.android.material.chip.Chip
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.PreferenceEqualizerDialogBinding
import me.timschneeberger.rootlessjamesdsp.interop.PreferenceCache
import me.timschneeberger.rootlessjamesdsp.preference.EqualizerPreference
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.view.EqualizerSurface
import timber.log.Timber

class EqualizerDialogFragment : PreferenceDialogFragmentCompat() {

    private var equalizer: EqualizerSurface? = null
    private lateinit var mLevels: DoubleArray
    private lateinit var mOldSetting: String

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                when (intent?.action) {
                    Constants.ACTION_PRESET_LOADED -> dismiss()
                }
            }
            catch(ex: IllegalStateException) {
                // Catch illegal state exception when dismissing after onSaveInstanceState
                Timber.w(ex)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mOldSetting = savedInstanceState?.getString("oldSetting")
            ?: preference.sharedPreferences
            ?.getString(preference.key, (preference as EqualizerPreference).initialValue) ?: ""

        mLevels = savedInstanceState?.getDoubleArray("levels") ?: mOldSetting
            .split(";")
            .drop(15)
            .dropLastWhile(String::isEmpty)
            .map { it.toDoubleOrNull() ?: 0.0 }
            .toDoubleArray()
            .copyOf(15)

        requireContext().registerLocalReceiver(broadcastReceiver, IntentFilter(Constants.ACTION_PRESET_LOADED))
    }


    // FIXME Equalizer is impossible to use with accessibility tools
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        val binding = PreferenceEqualizerDialogBinding.bind(view)
        equalizer = binding.equalizerSurface
        equalizer!!.areKnobsVisible = true
        equalizer!!.setOnTouchListener { v, event ->
            val minDb = equalizer!!.minDb
            val maxDb = equalizer!!.maxDb
            val band = equalizer!!.findClosest(event.x)
            var level = event.y / v.height * (minDb - maxDb) - minDb
            if (level > -0.05 && level < 0.0) {
                level = 0.0
            } else if (level > maxDb) {
                level = maxDb
            } else if (level < minDb) {
                level = minDb
            }

            updateBand(band, level)
            applyCurrentSetting()
            true
        }

        val type = PreferenceCache.uncachedGet(requireContext(), Constants.PREF_EQ, R.string.key_eq_filter_type, "0").toIntOrNull() ?: 0
        equalizer!!.mode = if(type == 0) EqualizerSurface.Mode.Fir else EqualizerSurface.Mode.Iir
        equalizer!!.iirOrder = when(type) {
            1 -> 4
            2 -> 6
            3 -> 8
            4 -> 10
            else -> 12
        }

        mLevels.forEachIndexed(equalizer!!::setBand)

        (preference as EqualizerPreference).entries.forEachIndexed { index, charSequence ->
            val chip =
                Chip(requireContext(), null,
                    com.google.android.material.R.style.Widget_Material3_Chip_Assist_Elevated)
                    .apply {
                    text = charSequence
                    setOnClickListener {
                        (preference as EqualizerPreference).entryValues[index]
                            .split(";")
                            .dropLastWhile(String::isEmpty)
                            .map { it.toDoubleOrNull() ?: 0.0 }
                            .forEachIndexed(::updateBand)
                            .also { applyCurrentSetting() }
                    }
                }
        binding.equalizerPresets.addView(chip, index)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState.apply {
            putDoubleArray("levels", mLevels)
            putString("oldSetting", mOldSetting)
        })
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val array = EqualizerSurface.SCALE + mLevels
            val value = array.joinToString(";")
            if (preference.callChangeListener(value)) {
                applySetting(value)
            }
        } else {
            applySetting(mOldSetting)
        }
    }

    private fun applyCurrentSetting() {
        applySetting((EqualizerSurface.SCALE + mLevels).joinToString(";"))
    }

    private fun applySetting(value: String) {
        preference.preferenceManager.sharedPreferences?.edit(commit = true) {
            putString(preference.key, value)
        }
        (preference as EqualizerPreference).updateFromPreferences()
    }

    private fun updateBand(i: Int, gain: Double) {
        mLevels[i] = gain
        equalizer!!.setBand(i, gain)
    }

    override fun onDestroy() {
        requireContext().unregisterLocalReceiver(broadcastReceiver)
        super.onDestroy()
        equalizer = null
    }

    companion object {
        private const val BUNDLE_KEY = "key"

        fun newInstance(key: String): EqualizerDialogFragment {
            val fragment = EqualizerDialogFragment()
            fragment.arguments = Bundle().apply {
                putString(BUNDLE_KEY, key)
            }
            return fragment
        }
    }
}