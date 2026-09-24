package me.timschneeberger.rootlessjamesdsp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.recyclerview.widget.RecyclerView
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.model.ParametricEqBand
import me.timschneeberger.rootlessjamesdsp.model.ParametricEqBandList
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class ParametricEqBandAdapter(var bands: ParametricEqBandList) :
    RecyclerView.Adapter<ParametricEqBandAdapter.ViewHolder>() {

    private val dfFreq = DecimalFormat("0", DecimalFormatSymbols.getInstance())
    private val dfGain = DecimalFormat("0", DecimalFormatSymbols.getInstance())
    private val dfQ = DecimalFormat("0", DecimalFormatSymbols.getInstance())

    init {
        dfFreq.maximumFractionDigits = 1
        dfGain.maximumFractionDigits = 2
        dfQ.maximumFractionDigits = 2
    }

    var onItemsChanged: ((ParametricEqBandAdapter) -> Unit)? = null
    var onItemClicked: ((ParametricEqBand, Int) -> Unit)? = null

    private val callback = object : ObservableList.OnListChangedCallback<ObservableArrayList<ParametricEqBand>>() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onChanged(sender: ObservableArrayList<ParametricEqBand>?) {
            this@ParametricEqBandAdapter.notifyDataSetChanged()
            onItemsChanged()
        }

        override fun onItemRangeChanged(
            sender: ObservableArrayList<ParametricEqBand>?,
            positionStart: Int,
            itemCount: Int,
        ) {
            this@ParametricEqBandAdapter.notifyItemRangeChanged(positionStart, itemCount)
            onItemsChanged()
        }

        override fun onItemRangeInserted(
            sender: ObservableArrayList<ParametricEqBand>?,
            positionStart: Int,
            itemCount: Int,
        ) {
            this@ParametricEqBandAdapter.notifyItemRangeInserted(positionStart, itemCount)
            onItemsChanged()
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onItemRangeMoved(
            sender: ObservableArrayList<ParametricEqBand>?,
            fromPosition: Int,
            toPosition: Int,
            itemCount: Int,
        ) {
            this@ParametricEqBandAdapter.notifyDataSetChanged()
            onItemsChanged()
        }

        override fun onItemRangeRemoved(
            sender: ObservableArrayList<ParametricEqBand>?,
            positionStart: Int,
            itemCount: Int,
        ) {
            this@ParametricEqBandAdapter.notifyItemRangeRemoved(positionStart, itemCount)
            onItemsChanged()
        }
    }

    private fun onItemsChanged() {
        this.onItemsChanged?.invoke(this)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val filterType: TextView = view.findViewById(R.id.filter_type)
        val channel: TextView = view.findViewById(R.id.channel)
        val freq: TextView = view.findViewById(R.id.freq)
        val gain: TextView = view.findViewById(R.id.gain)
        val qFactor: TextView = view.findViewById(R.id.q_factor)
        val deleteButton: Button = view.findViewById(R.id.delete)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        bands.addOnListChangedCallback(callback)
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        bands.removeOnListChangedCallback(callback)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_peq_band_list, viewGroup, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.deleteButton.isEnabled = true

        val band = bands[position]
        viewHolder.filterType.text = band.filterType.displayLabel
        viewHolder.channel.text = band.channel.displayLabel
        viewHolder.freq.text = "${dfFreq.format(band.frequency)}Hz"
        viewHolder.gain.text = "${dfGain.format(band.gain)}dB"
        viewHolder.qFactor.text = "Q${dfQ.format(band.q)}"

        viewHolder.deleteButton.setOnClickListener {
            viewHolder.bindingAdapterPosition.let { pos ->
                if (pos >= 0) {
                    bands.removeAt(pos)
                }
            }
            viewHolder.deleteButton.isEnabled = false
        }

        viewHolder.itemView.setOnClickListener {
            viewHolder.bindingAdapterPosition.let { pos ->
                bands.getOrNull(pos)?.let {
                    onItemClicked?.invoke(it, pos)
                }
            }
        }
    }

    override fun getItemCount() = bands.size
}
