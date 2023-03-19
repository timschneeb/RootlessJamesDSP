package me.timschneeberger.rootlessjamesdsp.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.preference.PreferenceDialogFragmentCompat
import com.google.android.material.chip.Chip
import me.timschneeberger.rootlessjamesdsp.databinding.PreferenceEqualizerDialogBinding
import me.timschneeberger.rootlessjamesdsp.preference.EqualizerPreference
import me.timschneeberger.rootlessjamesdsp.view.EqualizerSurface
import me.timschneeberger.rootlessjamesdsp.view.EqualizerSurface.Companion.MAX_DB
import me.timschneeberger.rootlessjamesdsp.view.EqualizerSurface.Companion.MIN_DB

class EqualizerDialogFragment : PreferenceDialogFragmentCompat() {

    private var equalizer: EqualizerSurface? = null
    private lateinit var mLevels: DoubleArray
    private lateinit var mOldSetting: String

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
    }

    // FIXME Equalizer is impossible to use with accessibility tools
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        val binding = PreferenceEqualizerDialogBinding.bind(view)
        equalizer = binding.equalizerSurface
        equalizer!!.areKnobsVisible = true
        equalizer!!.setOnTouchListener { v, event ->
            val band = equalizer!!.findClosest(event.x)
            var level = event.y / v.height * (MIN_DB - MAX_DB) - MIN_DB
            if (level > -0.05 && level < 0.0) {
                level = 0f
            } else if (level > MAX_DB) {
                level = MAX_DB.toFloat()
            } else if (level < MIN_DB) {
                level = MIN_DB.toFloat()
            }

            updateBand(band, level.toDouble())
            applyCurrentSetting()
            true
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
            val array = EqualizerSurface.FreqScale + mLevels
            val value = array.joinToString(";")
            if (preference.callChangeListener(value)) {
                applySetting(value)
            }
        } else {
            applySetting(mOldSetting)
        }
    }

    private fun applyCurrentSetting() {
        applySetting((EqualizerSurface.FreqScale + mLevels).joinToString(";"))
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
        super.onDestroy()
        equalizer = null
    }

    companion object {
        private const val BUNDLE_KEY = "key"

        fun newInstance(key: String): EqualizerDialogFragment {
            val fragment = EqualizerDialogFragment()

            val args = Bundle()
            args.putString(BUNDLE_KEY, key)
            fragment.arguments = args
            return fragment
        }
    }
}