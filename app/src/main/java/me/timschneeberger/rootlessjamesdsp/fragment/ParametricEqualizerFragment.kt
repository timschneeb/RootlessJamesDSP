package me.timschneeberger.rootlessjamesdsp.fragment

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.ParametricEqualizerActivity
import me.timschneeberger.rootlessjamesdsp.fragment.LiveEqBottomSheet
import me.timschneeberger.rootlessjamesdsp.adapter.ParametricEqBandAdapter
import me.timschneeberger.rootlessjamesdsp.databinding.FragmentParametricEqBinding
import me.timschneeberger.rootlessjamesdsp.model.ParametricEqBand
import me.timschneeberger.rootlessjamesdsp.model.ParametricEqBandList
import me.timschneeberger.rootlessjamesdsp.model.ParametricEqFilterType
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showInputAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showYesNoAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import timber.log.Timber
import java.util.UUID

class ParametricEqualizerFragment : Fragment() {
    private lateinit var binding: FragmentParametricEqBinding

    private val adapter: ParametricEqBandAdapter
        get() = binding.bandList.adapter as ParametricEqBandAdapter

    private var editorBandBackup: ParametricEqBand? = null
    private var editorBandUuid: UUID? = null
    private var editorActive = false
        set(value) {
            field = value
            binding.add.isEnabled = !value
            binding.reset.isEnabled = !value
            binding.importFile.isEnabled = !value
            binding.exportFile.isEnabled = !value
            binding.editString.isEnabled = !value
            binding.liveEq.isEnabled = !value && adapter.bands.isNotEmpty()
        }

    private val importFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@registerForActivityResult
            try {
                val text = requireContext().contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() } ?: return@registerForActivityResult
                val result = adapter.bands.fromApoString(text)

                // Apply imported preamp
                binding.preampInput.value = result.preampDb.toFloat()
                binding.equalizerSurface.setPreampDb(result.preampDb)

                save()
                updateViewState()
                val msg = getString(R.string.peq_import_success, adapter.bands.size)
                if (result.skippedFilters > 0) {
                    requireContext().toast("$msg (${result.skippedFilters} unsupported filters skipped)")
                } else {
                    requireContext().toast(msg)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to import PEQ file")
                requireContext().toast(R.string.peq_import_error)
            }
        }

    private val exportFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            uri ?: return@registerForActivityResult
            try {
                val apoString = adapter.bands.toApoString(binding.preampInput.value.toDouble())
                requireContext().contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(apoString)
                }
                requireContext().toast(R.string.peq_export_success)
            } catch (e: Exception) {
                Timber.e(e, "Failed to export PEQ file")
                requireContext().toast("Export failed: ${e.message}")
            }
        }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constants.ACTION_PRESET_LOADED -> {
                    activity?.finish()
                    startActivity(Intent(requireContext(), ParametricEqualizerActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requireContext().registerLocalReceiver(broadcastReceiver, IntentFilter(Constants.ACTION_PRESET_LOADED))
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        requireContext().unregisterLocalReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentParametricEqBinding.inflate(layoutInflater, container, false)

        binding.previewCard.setOnClickListener {
            if (resources.configuration.orientation != ORIENTATION_LANDSCAPE) {
                val newState = !binding.equalizerSurface.isVisible
                collapsePreview(newState)
            }
        }

        binding.reset.setOnClickListener {
            requireContext().showYesNoAlert(
                R.string.peq_reset_confirm_title,
                R.string.peq_reset_confirm,
            ) {
                if (it) {
                    adapter.bands.deserialize(Constants.DEFAULT_PEQ)
                    binding.preampInput.value = 0f
                    binding.equalizerSurface.setPreampDb(0.0)
                    updateViewState()
                    editorDiscard()
                    save()
                }
            }
        }

        binding.editString.setOnClickListener {
            requireContext().showInputAlert(
                layoutInflater,
                R.string.peq_edit_as_string,
                R.string.peq_edit_hint,
                adapter.bands.toApoString(binding.preampInput.value.toDouble()),
                false,
                null
            ) {
                it?.let { text ->
                    val result = adapter.bands.fromApoString(text)
                    // Apply parsed preamp
                    binding.preampInput.value = result.preampDb.toFloat()
                    binding.equalizerSurface.setPreampDb(result.preampDb)
                }
                save()
            }
        }

        binding.add.setOnClickListener {
            if (editorActive) return@setOnClickListener

            editorBandBackup = null
            editorBandUuid = null
            editorActive = true

            binding.freqInput.value = 1000f
            binding.gainInput.value = 0f
            binding.qInput.value = 1.41f
            setFilterTypeSelection(ParametricEqFilterType.PEAKING)
            updateViewState()
        }

        binding.importFile.setOnClickListener {
            importFileLauncher.launch(arrayOf("text/plain", "text/*"))
        }

        binding.exportFile.setOnClickListener {
            exportFileLauncher.launch("parametric_eq.txt")
        }

        binding.liveEq.setOnClickListener {
            LiveEqBottomSheet.newInstance(
                bands = adapter.bands,
                preampDb = binding.preampInput.value.toDouble(),
                onLiveUpdate = { bands ->
                    binding.equalizerSurface.setBands(bands, binding.preampInput.value.toDouble())
                },
                onCommit = {
                    // Writing the bands back already saved them through the adapter callback
                    if (isAdded && view != null)
                        updateViewState()
                }
            ).show(parentFragmentManager, "live_eq")
        }

        binding.freqInput.setOnValueChangedListener { editorApply() }
        binding.gainInput.setOnValueChangedListener { editorApply() }
        binding.qInput.setOnValueChangedListener { editorApply() }

        binding.filterTypeGroup.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) editorApply()
        }

        binding.freqInput.customStepScale = { value: Float, _: Boolean ->
            when (value) {
                in 0f..400f -> 10f
                in 400f..600f -> 20f
                in 600f..1000f -> 50f
                in 1000f..5000f -> 100f
                in 5000f..Float.MAX_VALUE -> 500f
                else -> 10f
            }
        }

        binding.qInput.min = 0.1f

        binding.qInput.customStepScale = { value: Float, _: Boolean ->
            when (value) {
                in 0f..1f -> 0.05f
                in 1f..5f -> 0.1f
                in 5f..10f -> 0.5f
                in 10f..Float.MAX_VALUE -> 1f
                else -> 0.1f
            }
        }

        binding.confirm.setOnClickListener {
            editorSave()
        }

        binding.cancel.setOnClickListener {
            editorDiscard()
        }

        // Preamp listener
        binding.preampInput.setOnValueChangedListener {
            binding.equalizerSurface.setPreampDb(binding.preampInput.value.toDouble())
            savePreamp()
        }

        // Load band data
        binding.bandList.layoutManager = LinearLayoutManager(requireContext())
        loadBands(savedInstanceState)

        updateViewState()
        return binding.root
    }

    private fun loadBands(savedInstanceState: Bundle?) {
        val bands = ParametricEqBandList()
        val prefs = requireContext().getSharedPreferences(Constants.PREF_PEQ, Context.MODE_PRIVATE)
        val dataSaved = savedInstanceState?.getBundle(STATE_BANDS)
        if (dataSaved != null) {
            bands.fromBundle(dataSaved)
        } else {
            val bandString = prefs?.getString(getString(R.string.key_peq_bands), Constants.DEFAULT_PEQ)!!
            bands.deserialize(bandString)
        }

        // Load preamp
        val preampDb = prefs?.getFloat(getString(R.string.key_peq_preamp), 0f) ?: 0f
        binding.preampInput.value = preampDb
        binding.equalizerSurface.setBands(bands, preampDb.toDouble())

        binding.bandList.adapter = ParametricEqBandAdapter(bands).apply {
            onItemsChanged = {
                binding.equalizerSurface.setBands(it.bands, binding.preampInput.value.toDouble())
                updateViewState()
                save()
            }

            onItemClicked = { band: ParametricEqBand, _: Int ->
                editorBandBackup = band
                editorBandUuid = band.uuid
                editorActive = true

                binding.freqInput.value = band.frequency.toFloat()
                binding.gainInput.value = band.gain.toFloat()
                binding.qInput.value = band.q.toFloat()
                setFilterTypeSelection(band.filterType)
                updateViewState()
            }
        }
    }

    private fun getSelectedFilterType(): ParametricEqFilterType {
        return when (binding.filterTypeGroup.checkedButtonId) {
            R.id.filter_low_shelf -> ParametricEqFilterType.LOW_SHELF
            R.id.filter_high_shelf -> ParametricEqFilterType.HIGH_SHELF
            else -> ParametricEqFilterType.PEAKING
        }
    }

    private fun setFilterTypeSelection(type: ParametricEqFilterType) {
        val buttonId = when (type) {
            ParametricEqFilterType.PEAKING -> R.id.filter_peaking
            ParametricEqFilterType.LOW_SHELF -> R.id.filter_low_shelf
            ParametricEqFilterType.HIGH_SHELF -> R.id.filter_high_shelf
        }
        binding.filterTypeGroup.check(buttonId)
    }

    private fun updateViewState() {
        val empty = adapter.bands.isEmpty()
        binding.emptyView.isVisible = empty && !editorActive
        binding.bandList.isVisible = !empty && !editorActive
        binding.bandEdit.isVisible = editorActive
        binding.bandDetailContextButtons.visibility = if (editorActive) View.VISIBLE else View.INVISIBLE
        binding.editCardTitle.text = getString(if (editorActive) R.string.peq_band_editor else R.string.peq_band_list)
        binding.liveEq.isEnabled = !empty && !editorActive
    }

    override fun onStop() {
        if (editorActive) {
            Timber.d("onStop: discarding unsaved changes")
            editorDiscard()
        }
        super.onStop()
    }

    private fun editorCanSave(): Boolean {
        val freqValid = binding.freqInput.isCurrentValueValid()
        val gainValid = binding.gainInput.isCurrentValueValid()
        val qValid = binding.qInput.isCurrentValueValid()
        return freqValid && gainValid && qValid
    }

    private fun editorApply() {
        if (editorCanSave()) {
            val uuid = editorBandUuid
            val freq = binding.freqInput.value.toDouble()
            val gain = binding.gainInput.value.toDouble()
            val q = binding.qInput.value.toDouble()
            val filterType = getSelectedFilterType()

            if (uuid == null) {
                val band = ParametricEqBand(freq, gain, q, filterType)
                adapter.bands.add(band)
                editorBandUuid = band.uuid
                Timber.d("editorApply: tracking new band $editorBandUuid for $freq Hz $gain dB Q$q $filterType")
            } else {
                Timber.d("editorApply: modifying band $editorBandUuid")
                val index = adapter.bands.indexOfFirst { it.uuid == uuid }
                if (index < 0)
                    Timber.e("editorApply: failed to find matching band UUID")
                else
                    adapter.bands[index] = ParametricEqBand(freq, gain, q, filterType, uuid)
            }
        }
    }

    private fun editorDiscard() {
        val uuid = editorBandUuid
        if (editorBandBackup != null && uuid != null) {
            Timber.d("editorDiscard: reverting modifications to band $uuid")
            val index = adapter.bands.indexOfFirst { it.uuid == uuid }
            if (index < 0)
                Timber.e("editorDiscard: failed to find matching band UUID")
            else
                adapter.bands[index] = editorBandBackup
        } else if (uuid != null) {
            Timber.d("editorDiscard: reverting addition of band $uuid")
            adapter.bands.removeAll { it.uuid == uuid }
        }

        editorBandBackup = null
        editorBandUuid = null
        editorActive = false
        updateViewState()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun editorSave() {
        if (!editorCanSave()) {
            requireContext().showYesNoAlert(
                R.string.peq_discard_changes_title,
                R.string.peq_discard_changes
            ) {
                if (it) {
                    editorDiscard()
                }
            }
            return
        }

        Timber.d("editorSave: confirming changes to band $editorBandUuid")
        editorBandBackup = null
        editorBandUuid = null
        editorActive = false

        adapter.notifyDataSetChanged()
        updateViewState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (newConfig.orientation == ORIENTATION_LANDSCAPE) {
            collapsePreview(false)
        }
        super.onConfigurationChanged(newConfig)
    }

    private fun collapsePreview(collapsed: Boolean) {
        binding.equalizerSurface.isVisible = collapsed
        binding.previewTitle.text =
            getString(if (collapsed) R.string.peq_preview else R.string.peq_preview_collapsed)
    }

    @SuppressLint("ApplySharedPref")
    private fun save() {
        requireContext().getSharedPreferences(Constants.PREF_PEQ, Context.MODE_PRIVATE)
            .edit()
            .putString(getString(R.string.key_peq_bands), adapter.bands.serialize())
            .putFloat(getString(R.string.key_peq_preamp), binding.preampInput.value)
            .commit()
        requireContext().sendLocalBroadcast(Intent(Constants.ACTION_PARAMETRIC_EQ_CHANGED))
    }

    @SuppressLint("ApplySharedPref")
    private fun savePreamp() {
        requireContext().getSharedPreferences(Constants.PREF_PEQ, Context.MODE_PRIVATE)
            .edit()
            .putFloat(getString(R.string.key_peq_preamp), binding.preampInput.value)
            .commit()
        requireContext().sendLocalBroadcast(Intent(Constants.ACTION_PARAMETRIC_EQ_CHANGED))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (editorActive)
            editorDiscard()

        super.onSaveInstanceState(outState)
    }

    companion object {
        const val STATE_BANDS = "bands"

        fun newInstance(): ParametricEqualizerFragment {
            return ParametricEqualizerFragment()
        }
    }
}
