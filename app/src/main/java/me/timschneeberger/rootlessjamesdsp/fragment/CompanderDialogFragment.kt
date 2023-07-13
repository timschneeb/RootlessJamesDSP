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
import me.timschneeberger.rootlessjamesdsp.databinding.PreferenceCompanderDialogBinding
import me.timschneeberger.rootlessjamesdsp.preference.CompanderPreference
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.view.CompanderSurface

class CompanderDialogFragment : PreferenceDialogFragmentCompat() {

    private var compander: CompanderSurface? = null
    private lateinit var mLevels: DoubleArray
    private lateinit var mOldSetting: String

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                Constants.ACTION_PRESET_LOADED -> dismiss()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mOldSetting = savedInstanceState?.getString("oldSetting")
            ?: preference.sharedPreferences
            ?.getString(preference.key, (preference as CompanderPreference).initialValue) ?: ""

        mLevels = savedInstanceState?.getDoubleArray("levels") ?: mOldSetting
            .split(";")
            .drop(7)
            .dropLastWhile(String::isEmpty)
            .map { it.toDoubleOrNull() ?: 0.0 }
            .toDoubleArray()
            .copyOf(7)

        requireContext().registerLocalReceiver(broadcastReceiver, IntentFilter(Constants.ACTION_PRESET_LOADED))
    }


    // FIXME Equalizer is impossible to use with accessibility tools
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        val binding = PreferenceCompanderDialogBinding.bind(view)
        compander = binding.companderSurface
        compander!!.areKnobsVisible = true
        compander!!.setOnTouchListener { v, event ->
            val minDb = compander!!.minDb
            val maxDb = compander!!.maxDb
            val band = compander!!.findClosest(event.x)
            var level = event.y / v.height * (minDb - maxDb) - minDb
            if (level > -0.02 && level < 0.0) {
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

        mLevels.forEachIndexed(compander!!::setBand)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState.apply {
            putDoubleArray("levels", mLevels)
            putString("oldSetting", mOldSetting)
        })
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val array = CompanderSurface.SCALE + mLevels
            val value = array.joinToString(";")
            if (preference.callChangeListener(value)) {
                applySetting(value)
            }
        } else {
            applySetting(mOldSetting)
        }
    }

    private fun applyCurrentSetting() {
        applySetting((CompanderSurface.SCALE + mLevels).joinToString(";"))
    }

    private fun applySetting(value: String) {
        preference.preferenceManager.sharedPreferences?.edit(commit = true) {
            putString(preference.key, value)
        }
        (preference as CompanderPreference).updateFromPreferences()
    }

    private fun updateBand(i: Int, gain: Double) {
        mLevels[i] = gain
        compander!!.setBand(i, gain)
    }

    override fun onDestroy() {
        requireContext().unregisterLocalReceiver(broadcastReceiver)
        super.onDestroy()
        compander = null
    }

    companion object {
        private const val BUNDLE_KEY = "key"

        fun newInstance(key: String): CompanderDialogFragment {
            val fragment = CompanderDialogFragment()
            fragment.arguments = Bundle().apply {
                putString(BUNDLE_KEY, key)
            }
            return fragment
        }
    }
}