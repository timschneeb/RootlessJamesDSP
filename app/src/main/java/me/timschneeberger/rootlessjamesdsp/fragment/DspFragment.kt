package me.timschneeberger.rootlessjamesdsp.fragment

import android.animation.LayoutTransition
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.FragmentDspBinding
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspLocalEngine
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspWrapper
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.preferences.Preferences
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.Locale

class DspFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val prefsApp: Preferences.App by inject()
    private val prefsVar: Preferences.Var by inject()

    private lateinit var binding: FragmentDspBinding
    private var updateNoticeOnClick: (() -> Unit)? = null
    private var updateNoticeOnCloseClick: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        prefsApp.registerOnSharedPreferenceChangeListener(this)
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        prefsApp.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            applySignalFlowOrder()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDspBinding.inflate(layoutInflater, container, false)

        binding.translationNotice.setOnCloseClickListener(::hideTranslationNotice)
        binding.translationNotice.setOnRootClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://crowdin.com/project/rootlessjamesdsp")))
            hideTranslationNotice()
        }

        binding.updateNotice.setOnCloseClickListener {
            updateNoticeOnCloseClick?.invoke()
        }
        binding.updateNotice.setOnRootClickListener {
            updateNoticeOnClick?.invoke()
        }

        // Should show notice?
        Timber.e(Locale.getDefault().language.toString())
        binding.translationNotice.isVisible =
           prefsVar.get<Long>(R.string.key_snooze_translation_notice) < (System.currentTimeMillis() / 1000L) &&
                    !Locale.getDefault().language.equals("en")
        binding.updateNotice.isVisible = false

        val transition = LayoutTransition()
        transition.enableTransitionType(LayoutTransition.CHANGING)
        binding.cardContainer.layoutTransition = transition

        childFragmentManager.beginTransaction()
            .replace(R.id.card_device_profiles, DeviceProfilesCardFragment.newInstance())
            .replace(
                R.id.card_output_control, PreferenceGroupFragment.newInstance(Constants.PREF_OUTPUT,
                    R.xml.dsp_output_control_preferences
                ))
            .replace(
                R.id.card_eq, PreferenceGroupFragment.newInstance(Constants.PREF_EQ,
                    R.xml.dsp_equalizer_preferences
                ))
            .replace(
                R.id.card_peq, PreferenceGroupFragment.newInstance(Constants.PREF_PEQ,
                    R.xml.dsp_parametriceq_preferences
                ))
            .replace(
                R.id.card_geq, PreferenceGroupFragment.newInstance(Constants.PREF_GEQ,
                    R.xml.dsp_graphiceq_preferences
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
            .commit()

        // Hide disabled modules
        listOf(
            binding.cardCompressor,
            binding.cardBass,
            binding.cardDdc,
            binding.cardConvolver,
            binding.cardCrossfeed,
            binding.cardReverb
        ).forEach {
            (it.parent as? View)?.isVisible = false
        }

        applySignalFlowOrder()

        // Load initial preferences
        arrayOf(R.string.key_device_profiles_enable).forEach {
            onSharedPreferenceChanged(null, getString(it))
        }

        return binding.root
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when(key) {
            getString(R.string.key_device_profiles_enable) -> {
                (binding.cardDeviceProfiles.parent as ViewGroup).isVisible =
                    prefsApp.get<Boolean>(R.string.key_device_profiles_enable)
            }
        }
    }

    private fun hideTranslationNotice() {
        binding.translationNotice.isVisible = false
        // Set timer +1y
        prefsVar.set<Long>(R.string.key_snooze_translation_notice, (System.currentTimeMillis() / 1000L) + 31536000L)
    }

    private fun applySignalFlowOrder() {
        val orderedProcessingCards = getNativeProcessingOrder()
            .flatMap { internalName -> moduleCardsForInternalName(internalName) }
            .distinct()

        val allRuntimeCards = (defaultProcessingCards() + outputSafetyCard()).distinct()
        allRuntimeCards.forEach { binding.cardContainer.removeView(it) }

        orderedProcessingCards.forEach { binding.cardContainer.addView(it) }
        binding.cardContainer.addView(outputSafetyCard())
    }

    private fun getNativeProcessingOrder(): List<String> {
        val engine = JamesDspLocalEngine.activeInstance ?: return defaultNativeOrder
        val modules = JamesDspWrapper.getModules(engine.handle).associateBy { it.index }
        val order = JamesDspWrapper.getExecutionOrder(engine.handle)
        if (order.isEmpty() || order.size != modules.size || order.toSet().size != order.size) return defaultNativeOrder
        if (order.any { it !in modules }) return defaultNativeOrder
        return order.mapNotNull { modules[it]?.internalName }
    }

    private fun moduleCardsForInternalName(internalName: String): List<View> =
        when (internalName) {
            "tube" -> listOf(cardFor(binding.cardTube))
            "m_eq" -> listOf(cardFor(binding.cardEq))
            "arb_eq" -> listOf(cardFor(binding.cardPeq), cardFor(binding.cardGeq))
            "liveprog" -> listOf(cardFor(binding.cardLiveprog))
            "ster_enh" -> listOf(cardFor(binding.cardStereowide))
            else -> emptyList()
        }

    private fun defaultProcessingCards(): List<View> =
        defaultNativeOrder.flatMap { moduleCardsForInternalName(it) }

    private fun outputSafetyCard(): View =
        cardFor(binding.cardOutputControl)

    private fun cardFor(container: View): View =
        container.parent as View

    fun setUpdateCardVisible(visible: Boolean) {
        binding.updateNotice.isVisible = visible
    }

    fun setUpdateCardTitle(title: String) {
        binding.updateNotice.titleText = title
    }

    fun setUpdateCardOnClick(onClick: () -> Unit) {
        updateNoticeOnClick = onClick
    }

    fun setUpdateCardOnCloseClick(onClick: () -> Unit) {
        updateNoticeOnCloseClick = onClick
    }

    fun restartFragment(id: Int, newFragment: Fragment) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                childFragmentManager.beginTransaction()
                    .replace(id, newFragment)
                    .commitAllowingStateLoss()
            }
            catch(ex: IllegalStateException) {
                Timber.e("Failed to restart fragment")
                Timber.i(ex)
            }
        }
    }

    companion object {
        private val defaultNativeOrder = listOf("tube", "m_eq", "arb_eq", "liveprog", "ster_enh")

        fun newInstance(): DspFragment {
            return DspFragment()
        }
    }
}
