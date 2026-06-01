package me.timschneeberger.rootlessjamesdsp.utils

import android.content.Context
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspHandle
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspWrapper

object DspExecutionOrderStore {
    val defaultOrder = listOf("tube", "m_eq", "arb_eq", "liveprog", "ster_enh")
    val reorderableNames = defaultOrder.toSet()

    fun currentOrderNames(handle: JamesDspHandle): List<String>? {
        val modulesByIndex = JamesDspWrapper.getModules(handle).associateBy { it.index }
        val order = JamesDspWrapper.getExecutionOrder(handle).toList()
        val names = mutableListOf<String>()
        for (index in order) {
            val name = modulesByIndex[index]?.internalName ?: return null
            names.add(name)
        }
        return names.takeIf { isValidOrder(it) }
    }

    fun applySavedOrder(context: Context, handle: JamesDspHandle): Boolean {
        val savedOrder = readSavedOrder(context)
        if (savedOrder == null) {
            saveCurrentOrder(context, handle)
            return true
        }

        if (setOrder(context, handle, savedOrder, persist = false)) {
            saveOrder(context, savedOrder)
            return true
        }

        JamesDspWrapper.resetExecutionOrder(handle)
        currentOrderNames(handle)?.let { saveOrder(context, it) }
        return false
    }

    fun saveCurrentOrder(context: Context, handle: JamesDspHandle) {
        currentOrderNames(handle)?.let { saveOrder(context, it) }
    }

    fun setOrder(context: Context, handle: JamesDspHandle, order: List<String>, persist: Boolean = true): Boolean {
        if (!isValidOrder(order)) return false

        val modulesByName = JamesDspWrapper.getModules(handle).associateBy { it.internalName }
        val nativeOrder = IntArray(order.size)
        for ((position, name) in order.withIndex()) {
            nativeOrder[position] = modulesByName[name]?.index ?: return false
        }
        if (nativeOrder.size != modulesByName.size || nativeOrder.toList().toSet().size != nativeOrder.size) return false

        val applied = JamesDspWrapper.setExecutionOrder(handle, nativeOrder)
        if (applied && persist) saveOrder(context, order)
        return applied
    }

    fun isValidOrder(order: List<String>): Boolean =
        order.size == reorderableNames.size &&
            order.toSet().size == order.size &&
            order.all { it in reorderableNames }

    private fun readSavedOrder(context: Context): List<String>? {
        val value = prefs(context).getString(context.getString(R.string.key_dsp_execution_order), null)
            ?: return null
        val order = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return order.takeIf { isValidOrder(it) }
    }

    private fun saveOrder(context: Context, order: List<String>) {
        prefs(context)
            .edit()
            .putString(context.getString(R.string.key_dsp_execution_order), order.joinToString(","))
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE)
}
