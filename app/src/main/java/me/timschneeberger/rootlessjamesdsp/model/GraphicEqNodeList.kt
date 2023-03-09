package me.timschneeberger.rootlessjamesdsp.model

import android.os.Bundle
import androidx.databinding.ObservableArrayList
import me.timschneeberger.rootlessjamesdsp.utils.extensions.CompatExtensions.getSerializableAs
import timber.log.Timber
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class GraphicEqNodeList : ObservableArrayList<GraphicEqNode>() {
    private val dfFreq = DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
    private val dfGain = DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH))

    init {
        dfFreq.maximumFractionDigits = 2
        dfGain.maximumFractionDigits = 6
    }

    fun serialize(): String {
        var str = "GraphicEQ: "
        for (i in 0 until this.size) {
            str += "${dfFreq.format(this[i].freq)} ${dfGain.format(this[i].gain)}; "
        }

        return str
    }


    fun deserialize(str: String) {
        this.clear()

        str.replace("GraphicEQ:", "")
            .replace("\n", " ")
            .split(";")
            .map { it.trim() }
            .filter(String::isNotBlank)
            .forEach { s ->
                val pair = s.split(" ").filter(String::isNotBlank)
                val freq = pair.getOrNull(0)?.toDoubleOrNull()
                val gain = pair.getOrNull(1)?.toDoubleOrNull()

                if (freq != null && gain != null) {
                    val node = GraphicEqNode(freq, gain)
                    this.add(node)
                }
            }
    }

    fun fromBundle(bundle: Bundle) {
        this.clear()

        val freq = bundle.getDoubleArray(STATE_FREQ) ?: return
        val gain = bundle.getDoubleArray(STATE_GAIN) ?: return
        val uuids = bundle.getSerializableAs<Array<UUID>>(STATE_UUID)

        val count = Integer.min(freq.size, gain.size)
        for (i in 0 until count) {
            val node = GraphicEqNode(freq[i], gain[i], uuids?.get(i) ?: UUID.randomUUID())
            Timber.d("tracking restored node UUID ${node.uuid} for ${freq[i]} Hz with ${gain[i]} dB (source: fromBundle)")
            this.add(node)
        }
    }

    fun toBundle(): Bundle {
        val bundle = Bundle()
        val arrays = toArrays()
        bundle.putDoubleArray(STATE_FREQ, arrays.first)
        bundle.putDoubleArray(STATE_GAIN, arrays.second)
        bundle.putSerializable(STATE_UUID, arrays.third)
        return bundle
    }

    fun toArrays(): Triple<DoubleArray, DoubleArray, Array<UUID>> {
        val freq = DoubleArray(this.size)
        val gain = DoubleArray(this.size)
        val uuids = arrayListOf<UUID>()
        for((i, node) in this.withIndex()) {
            freq[i] = node.freq
            gain[i] = node.gain
            uuids.add(node.uuid)
        }
        return Triple(freq, gain, uuids.toTypedArray())
    }

    companion object {
        private const val STATE_FREQ = "freq"
        private const val STATE_GAIN = "gain"
        private const val STATE_UUID = "uuid"
    }
}