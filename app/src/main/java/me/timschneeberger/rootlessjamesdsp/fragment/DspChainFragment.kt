package me.timschneeberger.rootlessjamesdsp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.FragmentDspChainBinding
import me.timschneeberger.rootlessjamesdsp.databinding.ItemDspModuleBinding
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspLocalEngine
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspWrapper
import me.timschneeberger.rootlessjamesdsp.interop.structure.DspModule
import me.timschneeberger.rootlessjamesdsp.utils.DspExecutionOrderStore
import java.util.Collections

class DspChainFragment : Fragment() {

    private lateinit var binding: FragmentDspChainBinding
    private lateinit var adapter: DspModuleAdapter
    private var currentOrder = mutableListOf<Int>()
    private var allModules = listOf<DspModule>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDspChainBinding.inflate(inflater, container, false)
        setupRecyclerView()
        loadData()

        binding.btnReset.setOnClickListener {
            JamesDspLocalEngine.activeInstance?.let { engine ->
                JamesDspWrapper.resetExecutionOrder(engine.handle)
                DspExecutionOrderStore.saveCurrentOrder(requireContext(), engine.handle)
                loadData()
                Toast.makeText(requireContext(), "Chain order reset", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = DspModuleAdapter(
            onMoveUp = { position -> moveModule(position, -1) },
            onMoveDown = { position -> moveModule(position, 1) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun loadData() {
        val engine = JamesDspLocalEngine.activeInstance
        if (engine == null) {
            Toast.makeText(requireContext(), "No active DSP engine found", Toast.LENGTH_LONG).show()
            return
        }

        allModules = JamesDspWrapper.getModules(engine.handle)
        currentOrder = JamesDspWrapper.getExecutionOrder(engine.handle).toMutableList()
        if (!isValidOrder(currentOrder)) {
            Toast.makeText(requireContext(), "Invalid DSP chain order reported by engine", Toast.LENGTH_LONG).show()
            currentOrder = allModules.map { it.index }.toMutableList()
        }
        updateList()
    }

    private fun updateList() {
        val displayList = currentOrder.mapNotNull { idx ->
            allModules.find { it.index == idx }
        }
        adapter.submitList(displayList)
    }

    private fun moveModule(position: Int, direction: Int) {
        val newPosition = position + direction
        if (newPosition in 0 until currentOrder.size) {
            val previousOrder = currentOrder.toList()
            Collections.swap(currentOrder, position, newPosition)
            if (!isValidOrder(currentOrder)) {
                currentOrder = previousOrder.toMutableList()
                Toast.makeText(requireContext(), "Invalid DSP chain order", Toast.LENGTH_SHORT).show()
                return
            }
            
            JamesDspLocalEngine.activeInstance?.let { engine ->
                if (JamesDspWrapper.setExecutionOrder(engine.handle, currentOrder.toIntArray())) {
                    DspExecutionOrderStore.saveCurrentOrder(requireContext(), engine.handle)
                    loadData()
                } else {
                    currentOrder = previousOrder.toMutableList()
                    updateList()
                    Toast.makeText(requireContext(), "Failed to update execution order", Toast.LENGTH_SHORT).show()
                    loadData()
                }
            }
        }
    }

    private fun isValidOrder(order: List<Int>): Boolean {
        val validIndices = allModules.map { it.index }.toSet()
        return order.size == validIndices.size &&
            order.toSet().size == order.size &&
            order.all { it in validIndices }
    }

    private class DspModuleAdapter(
        private val onMoveUp: (Int) -> Unit,
        private val onMoveDown: (Int) -> Unit
    ) : RecyclerView.Adapter<DspModuleAdapter.ViewHolder>() {

        private var items = listOf<DspModule>()

        fun submitList(newList: List<DspModule>) {
            items = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDspModuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], position, items.size)
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(private val binding: ItemDspModuleBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(module: DspModule, position: Int, total: Int) {
                binding.tvDisplayName.text = module.displayName
                binding.tvInternalName.text = "Internal: ${module.internalName} (Index: ${module.index})"
                
                val statusParts = mutableListOf<String>()
                statusParts.add(if (module.enabled) "Enabled" else "Disabled")
                if (module.requiresLock) statusParts.add("Requires Lock")
                binding.tvStatus.text = statusParts.joinToString(" | ")
                binding.tvStatus.setTextColor(if (module.enabled) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())

                binding.btnUp.isEnabled = position > 0
                binding.btnDown.isEnabled = position < total - 1

                binding.btnUp.setOnClickListener { onMoveUp(position) }
                binding.btnDown.setOnClickListener { onMoveDown(position) }
            }
        }
    }

    companion object {
        fun newInstance() = DspChainFragment()
    }
}
