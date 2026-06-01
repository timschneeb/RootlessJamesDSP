package me.timschneeberger.rootlessjamesdsp.fragment

import android.animation.LayoutTransition
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.FragmentDspBinding
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspLocalEngine
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.DspExecutionOrderStore
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
            binding.cardReverb,
            binding.cardPeq
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
        val nativeOrder = getNativeProcessingOrder()
        val orderedProcessingCards = nativeOrder
            .mapNotNull { internalName -> moduleCardForInternalName(internalName) }
            .distinct()

        syncSignalFlowCards(orderedProcessingCards + outputSafetyCard())
        bindReorderControls(nativeOrder)
    }

    private fun syncSignalFlowCards(desiredCards: List<View>) {
        val container = binding.cardContainer
        if (desiredCards.isEmpty()) return

        val currentTailStart = container.childCount - desiredCards.size
        if (currentTailStart >= 0) {
            val currentTail = (currentTailStart until container.childCount).map { container.getChildAt(it) }
            if (currentTail == desiredCards) return
        }

        val transition = container.layoutTransition
        container.layoutTransition = null
        container.suppressLayout(true)
        try {
            desiredCards.forEach { card ->
                container.removeView(card)
                container.addView(card)
            }
        } finally {
            container.suppressLayout(false)
            container.layoutTransition = transition
        }
    }

    private fun getNativeProcessingOrder(): List<String> {
        val engine = JamesDspLocalEngine.activeInstance ?: return defaultNativeOrder
        return DspExecutionOrderStore.currentOrderNames(engine.handle) ?: defaultNativeOrder
    }

    private fun moduleCardForInternalName(internalName: String): View? =
        when (internalName) {
            "tube" -> cardFor(binding.cardTube)
            "m_eq" -> cardFor(binding.cardEq)
            "arb_eq" -> cardFor(binding.cardGeq)
            "liveprog" -> cardFor(binding.cardLiveprog)
            "ster_enh" -> cardFor(binding.cardStereowide)
            else -> null
        }

    private fun defaultProcessingCards(): List<View> =
        defaultNativeOrder.mapNotNull { moduleCardForInternalName(it) }

    private fun outputSafetyCard(): View =
        cardFor(binding.cardOutputControl)

    private fun cardFor(container: View): View =
        container.parent as View

    private fun bindReorderControls(nativeOrder: List<String>) {
        reorderableModuleNames.forEach { internalName ->
            moduleCardForInternalName(internalName)?.let { card ->
                card.setOnLongClickListener(null)
                card.setOnDragListener(null)
                card.isLongClickable = false
                removeReorderControls(card)
                if (internalName in nativeOrder) {
                    addReorderControls(card, internalName, nativeOrder)
                }
            }
        }
        outputSafetyCard().setOnLongClickListener(null)
        outputSafetyCard().setOnDragListener(null)
        outputSafetyCard().isLongClickable = false
        removeReorderControls(outputSafetyCard())
    }

    private fun addReorderControls(card: View, internalName: String, nativeOrder: List<String>) {
        val headerIconFrame = card.findViewById<LinearLayout>(R.id.icon_frame)
        if (headerIconFrame == null) {
            card.post { addReorderControls(card, internalName, getNativeProcessingOrder()) }
            return
        }

        val position = nativeOrder.indexOf(internalName)
        if (position < 0) return
        removeReorderControls(card)

        val controls = LinearLayout(requireContext()).apply {
            tag = REORDER_CONTROLS_TAG
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            isBaselineAligned = false
        }
        controls.addView(createReorderButton(R.drawable.ic_baseline_keyboard_arrow_up_24dp, position > 0) {
            moveModule(internalName, -1)
        })
        controls.addView(createReorderButton(R.drawable.ic_baseline_keyboard_arrow_down_24dp, position < nativeOrder.lastIndex) {
            moveModule(internalName, 1)
        })

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = REORDER_CONTROL_MARGIN_END_DP.dp()
        }
        headerIconFrame.isVisible = true
        headerIconFrame.addView(controls, 0, params)
    }

    private fun createReorderButton(iconRes: Int, enabled: Boolean, onClick: () -> Unit): ImageButton {
        val attrs = requireContext().obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackgroundBorderless))
        val background = attrs.getResourceId(0, 0)
        attrs.recycle()
        return ImageButton(requireContext()).apply {
            setImageResource(iconRes)
            setBackgroundResource(background)
            isEnabled = true
            alpha = if (enabled) 1.0f else 0.35f
            setOnClickListener {
                if (enabled) {
                    onClick()
                }
            }
            layoutParams = LinearLayout.LayoutParams(REORDER_BUTTON_SIZE_DP.dp(), REORDER_BUTTON_SIZE_DP.dp())
        }
    }

    private fun moveModule(internalName: String, direction: Int) {
        val currentOrder = getNativeProcessingOrder().toMutableList()
        if (!isValidModuleOrder(currentOrder)) {
            Toast.makeText(requireContext(), "Invalid DSP module order", Toast.LENGTH_SHORT).show()
            return
        }

        val position = currentOrder.indexOf(internalName)
        val newPosition = position + direction
        if (position < 0 || newPosition !in currentOrder.indices) {
            return
        }

        val previousOrder = currentOrder.toList()
        java.util.Collections.swap(currentOrder, position, newPosition)
        if (!isValidModuleOrder(currentOrder)) {
            Toast.makeText(requireContext(), "Invalid DSP module order", Toast.LENGTH_SHORT).show()
            return
        }

        if (setNativeProcessingOrder(currentOrder)) {
            applySignalFlowOrder()
        } else {
            setNativeProcessingOrder(previousOrder)
            applySignalFlowOrder()
            Toast.makeText(requireContext(), "Failed to update DSP module order", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setNativeProcessingOrder(order: List<String>): Boolean {
        val engine = JamesDspLocalEngine.activeInstance ?: return false
        return DspExecutionOrderStore.setOrder(requireContext(), engine.handle, order)
    }

    private fun removeReorderControls(card: View) {
        removeTaggedChildren(card as? ViewGroup ?: return)
    }

    private fun removeTaggedChildren(container: ViewGroup) {
        for (i in container.childCount - 1 downTo 0) {
            val child = container.getChildAt(i)
            if (child.tag == REORDER_CONTROLS_TAG) {
                container.removeViewAt(i)
            } else if (child is ViewGroup) {
                removeTaggedChildren(child)
            }
        }
    }

    private fun isValidModuleOrder(order: List<String>): Boolean =
        DspExecutionOrderStore.isValidOrder(order)

    private fun Int.dp(): Int =
        (this * resources.displayMetrics.density).toInt()

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
        private val defaultNativeOrder = DspExecutionOrderStore.defaultOrder
        private val reorderableModuleNames = DspExecutionOrderStore.reorderableNames
        private const val REORDER_CONTROLS_TAG = "dsp-reorder-controls"
        private const val REORDER_BUTTON_SIZE_DP = 32
        private const val REORDER_CONTROL_MARGIN_END_DP = 8

        fun newInstance(): DspFragment {
            return DspFragment()
        }
    }
}
