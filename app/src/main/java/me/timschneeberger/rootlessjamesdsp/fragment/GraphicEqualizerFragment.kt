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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.GraphicEqualizerActivity
import me.timschneeberger.rootlessjamesdsp.adapter.GraphicEqNodeAdapter
import me.timschneeberger.rootlessjamesdsp.contract.AutoEqSelectorContract
import me.timschneeberger.rootlessjamesdsp.databinding.FragmentGraphicEqBinding
import me.timschneeberger.rootlessjamesdsp.model.GraphicEqNode
import me.timschneeberger.rootlessjamesdsp.model.GraphicEqNodeList
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showInputAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showYesNoAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import timber.log.Timber
import java.util.UUID

class GraphicEqualizerFragment : Fragment() {
    private lateinit var binding: FragmentGraphicEqBinding

    private val adapter: GraphicEqNodeAdapter
        get() = binding.nodeList.adapter as GraphicEqNodeAdapter

    /** editorNodeBackup contains a backup of the node loaded in the editor.
    Is null when the editor is closed or while a node is added. */
    private var editorNodeBackup: GraphicEqNode? = null
    /** editorNodeUuid contains the position of the node loaded in the editor.
    Is null when an node is first added or the editor is closed. */
    private var editorNodeUuid: UUID? = null
    private var editorActive = false
        set(value) {
            field = value
            binding.add.isEnabled = !value
            binding.reset.isEnabled = !value
            binding.autoeq.isEnabled = !value
            binding.editString.isEnabled = !value
        }

    private val autoEqSelectorLauncher =
        registerForActivityResult(AutoEqSelectorContract()) { result ->
            result?.let {
                adapter.nodes.deserialize(it)
                save()
            }
        }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                Constants.ACTION_PRESET_LOADED -> {
                    activity?.finish()
                    startActivity(Intent(requireContext(), GraphicEqualizerActivity::class.java).apply {
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
        binding = FragmentGraphicEqBinding.inflate(layoutInflater, container, false)

        binding.previewCard.setOnClickListener {
            if(resources.configuration.orientation != ORIENTATION_LANDSCAPE) {
                val newState = !binding.equalizerSurface.isVisible
                collapsePreview(newState)
            }
        }

        binding.reset.setOnClickListener {
            requireContext().showYesNoAlert(
                R.string.geq_reset_confirm_title,
                R.string.geq_reset_confirm,
            ) {
                if(it) {
                    adapter.nodes.deserialize(Constants.DEFAULT_GEQ)
                    updateViewState()
                    editorDiscard()
                    save()
                }
            }
        }

        binding.editString.setOnClickListener {
            requireContext().showInputAlert(
                layoutInflater,
                R.string.geq_edit_as_string,
                R.string.geq_edit_hint,
                adapter.nodes.serialize(),
                false,
            null
            ) {
                it?.let {
                    adapter.nodes.deserialize(it)
                }
                save()
            }
        }

        binding.add.setOnClickListener {
            if(editorActive) return@setOnClickListener

            editorNodeBackup = null
            editorNodeUuid = null
            editorActive = true

            binding.freqInput.value = 100f
            binding.gainInput.value = 0f
            updateViewState()
        }

        binding.freqInput.setOnValueChangedListener { editorApply() }
        binding.gainInput.setOnValueChangedListener { editorApply() }

        binding.freqInput.customStepScale = { value: Float, _: Boolean ->
            when(value) {
                in 0f..400f -> 10f
                in 400f..600f -> 20f
                in 600f..1000f -> 50f
                in 1000f..5000f -> 100f
                in 5000f..Float.MAX_VALUE -> 500f
                else -> 10f
            }
        }

        binding.confirm.setOnClickListener {
            editorSave()
        }

        binding.cancel.setOnClickListener {
            editorDiscard()
        }

        binding.autoeq.setOnClickListener {
            editorDiscard()
            autoEqSelectorLauncher.launch(0)
        }

        // Load node data
        binding.nodeList.layoutManager = LinearLayoutManager(requireContext())
        loadNodes(savedInstanceState)

        // TODO fix
        /*if(savedInstanceState != null) {
            editorNodeUuid = savedInstanceState.getSerializableAs(STATE_EDITOR_NODE_UUID, UUID::class.java)
            editorNodeBackup = savedInstanceState.getSerializableAs(STATE_EDITOR_NODE_BACKUP, GraphicEqNode::class.java)
            editorActive = savedInstanceState.getBoolean(STATE_EDITOR_ACTIVE)
            binding.freqInput.value = savedInstanceState.getFloat(STATE_EDITOR_UI_FREQ_INPUT)
            binding.gainInput.value = savedInstanceState.getFloat(STATE_EDITOR_UI_GAIN_INPUT)
        }*/

        updateViewState()
        return binding.root
    }

    private fun loadNodes(savedInstanceState: Bundle?) {
        val nodes = GraphicEqNodeList()
        val dataSaved = savedInstanceState?.getBundle(STATE_NODES)
        if(dataSaved != null) {
            nodes.fromBundle(dataSaved)
        }
        else {
            val nodeString = requireContext().getSharedPreferences(Constants.PREF_GEQ, Context.MODE_PRIVATE)
                ?.getString(getString(R.string.key_geq_nodes), Constants.DEFAULT_GEQ)!!
            nodes.deserialize(nodeString)
        }
        nodes.sortBy { it.freq }
        binding.equalizerSurface.setNodes(nodes)

        binding.nodeList.adapter = GraphicEqNodeAdapter(nodes).apply {
            onItemsChanged = {
                binding.equalizerSurface.setNodes(it.nodes)

                updateViewState()
                save()
            }

            onItemClicked = { node: GraphicEqNode, _: Int ->
                editorNodeBackup = node
                editorNodeUuid = node.uuid
                editorActive = true

                binding.freqInput.value = node.freq.toFloat()
                binding.gainInput.value = node.gain.toFloat()
                updateViewState()
            }
        }
    }

    private fun updateViewState() {
        val empty = adapter.nodes.isEmpty()
        binding.emptyView.isVisible = empty
        binding.nodeList.isVisible = !empty && !editorActive
        binding.nodeEdit.isVisible = editorActive
        binding.nodeDetailContextButtons.visibility = if(editorActive) View.VISIBLE else View.INVISIBLE
        binding.editCardTitle.text = getString(if(editorActive) R.string.geq_node_editor else R.string.geq_node_list)
    }

    override fun onStop() {
        if(editorActive) {
            Timber.d("onStop: discarding unsaved changes")
            editorDiscard()
        }

        super.onStop()
    }

    private fun editorCanSave(): Boolean {
        // Allow save when all values are valid
        val freqValid = binding.freqInput.isCurrentValueValid()
        val gainValid = binding.gainInput.isCurrentValueValid()
        return freqValid && gainValid
    }

    private fun editorApply() {
        if(editorCanSave()) {
            val uuid = editorNodeUuid
            val freq = binding.freqInput.value.toDouble()
            val gain = binding.gainInput.value.toDouble()

            if(uuid == null) {
                val node = GraphicEqNode(freq, gain)
                adapter.nodes.add(node)
                editorNodeUuid = node.uuid
                Timber.d("editorApply: tracking new added mode $editorNodeUuid for $freq Hz with $gain dB (source: editorApply/add)")
            }
            else {
                Timber.d("editorApply: modifying node $editorNodeUuid")
                val index = adapter.nodes.indexOfFirst { it.uuid == uuid }
                if(index < 0)
                    Timber.e("editorApply: failed to find matching node UUID")
                else {
                    Timber.d("tracking node UUID $uuid (unchanged) for $freq Hz with $gain dB (source: editorApply/modify)")
                    adapter.nodes[index] = GraphicEqNode(freq, gain, uuid)
                }
            }

        }
    }

    private fun editorDiscard() {
        val uuid = editorNodeUuid
        if(editorNodeBackup != null && uuid != null) {
            /* Revert edits to node */
            Timber.d("editorDiscard: reverting modifications to node $uuid")
            val index = adapter.nodes.indexOfFirst { it.uuid == uuid }
            if(index < 0)
                Timber.e("editorDiscard: failed to find matching node UUID")
            else
                adapter.nodes[index] = editorNodeBackup
        }
        else if(uuid != null) {
            /* Revert added node */
            Timber.d("editorDiscard: reverting addition of node $uuid")
            adapter.nodes.removeAll { it.uuid == uuid }
        }

        editorNodeBackup = null
        editorNodeUuid = null
        editorActive = false
        updateViewState()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun editorSave() {
        if(!editorCanSave()) {
            requireContext().showYesNoAlert(
                R.string.geq_discard_changes_title,
                R.string.geq_discard_changes) {
                if(it) {
                    editorDiscard()
                }
            }
            return
        }

        Timber.d("editorSave: confirming changes to node $editorNodeUuid")
        editorNodeBackup = null
        editorNodeUuid = null
        editorActive = false

        adapter.nodes.sortBy { it.freq }
        adapter.notifyDataSetChanged()

        updateViewState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if(newConfig.orientation == ORIENTATION_LANDSCAPE) {
            collapsePreview(false)
        }
        super.onConfigurationChanged(newConfig)
    }

    private fun collapsePreview(collapsed: Boolean) {
        binding.equalizerSurface.isVisible = collapsed
        binding.previewTitle.text =
            getString(if(collapsed) R.string.geq_preview else R.string.geq_preview_collapsed)
    }

    @SuppressLint("ApplySharedPref")
    private fun save() {
        requireContext().getSharedPreferences(Constants.PREF_GEQ, Context.MODE_PRIVATE)
            .edit()
            .putString(getString(R.string.key_geq_nodes), adapter.nodes.serialize())
            .commit()
        requireContext().sendLocalBroadcast(Intent(Constants.ACTION_GRAPHIC_EQ_CHANGED))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // TODO workaround: discard changes
        if(editorActive)
            editorDiscard()

        /*super.onSaveInstanceState(outState.apply {
            putBundle(STATE_NODES, adapter.nodes.toBundle())
            putSerializable(STATE_EDITOR_NODE_UUID, editorNodeUuid)
            putSerializable(STATE_EDITOR_NODE_BACKUP, editorNodeBackup)
            putBoolean(STATE_EDITOR_ACTIVE, editorActive)
            putFloat(STATE_EDITOR_UI_FREQ_INPUT, binding.freqInput.value)
            putFloat(STATE_EDITOR_UI_GAIN_INPUT, binding.gainInput.value)
        })*/
    }

    companion object {
        const val STATE_NODES = "nodes"
        const val STATE_EDITOR_NODE_UUID = "editorNodeUuid"
        const val STATE_EDITOR_NODE_BACKUP = "editorNodeBackup"
        const val STATE_EDITOR_ACTIVE = "editorActive"
        const val STATE_EDITOR_UI_FREQ_INPUT = "editorUiFreqInput"
        const val STATE_EDITOR_UI_GAIN_INPUT = "editorUiGainInput"

        fun newInstance(): GraphicEqualizerFragment {
            return GraphicEqualizerFragment()
        }
    }
}