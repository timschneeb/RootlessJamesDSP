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

    private var dialogEqualizerView: EqualizerSurface? = null
    private lateinit var mLevels: DoubleArray
    private var shouldReset = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mLevels = savedInstanceState?.getDoubleArray("levels") ?: preference.sharedPreferences
            ?.getString(preference.key, (preference as EqualizerPreference).initialValue)!!
            .split(";")
            .drop(15)
            .dropLastWhile(String::isEmpty)
            .map(String::toDouble)
            .toDoubleArray()
            .copyOf(15)
    }

    // FIXME Equalizer is impossible to use with accessibility tools
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        val binding = PreferenceEqualizerDialogBinding.bind(view)
        dialogEqualizerView = binding.equalizerDialog
        dialogEqualizerView!!.areKnobsVisible = true
        dialogEqualizerView!!.setOnTouchListener { v, event ->
            val band = dialogEqualizerView!!.findClosest(event.x)
            var level = event.y / v.height * (MIN_DB - MAX_DB) - MIN_DB
            if (level > -0.05 && level < 0.0) {
                level = 0f
            } else if (level > MAX_DB) {
                level = MAX_DB.toFloat()
            } else if (level < MIN_DB) {
                level = MIN_DB.toFloat()
            }

            updateBand(band, level.toDouble())
            true
        }

        mLevels.forEachIndexed(dialogEqualizerView!!::setBand)

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
                            .map(String::toDouble)
                            .forEachIndexed(::updateBand)
                    }
                }
        binding.equalizerPresets.addView(chip, index)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        shouldReset = false
        super.onSaveInstanceState(outState.apply { putDoubleArray("levels", mLevels) })
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val array = EqualizerSurface.FreqScale + mLevels
            val value = array.joinToString(";")
            if (preference.callChangeListener(value)) {
                preference.preferenceManager.sharedPreferences?.edit(commit = true) {
                    putString(preference.key, value)
                }
                (preference as EqualizerPreference).updateFromPreferences()
            }
        } else if (shouldReset) {
            preference.sharedPreferences?.getString(
                preference.key, (preference as EqualizerPreference).initialValue
            )!!.split(";")
                .drop(15)
                .dropLastWhile(String::isEmpty)
                .map(String::toDouble)
                .forEachIndexed(::updateBand)
        }
    }

    private fun updateBand(i: Int, gain: Double) {
        mLevels[i] = gain
        dialogEqualizerView!!.setBand(i, gain)
    }

    override fun onDestroy() {
        super.onDestroy()
        dialogEqualizerView = null
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