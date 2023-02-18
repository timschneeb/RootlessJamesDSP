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
import me.timschneeberger.rootlessjamesdsp.model.GraphicEqNode
import me.timschneeberger.rootlessjamesdsp.model.GraphicEqNodeList
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class GraphicEqNodeAdapter(var nodes: GraphicEqNodeList) :
    RecyclerView.Adapter<GraphicEqNodeAdapter.ViewHolder>() {

    private val df = DecimalFormat("0", DecimalFormatSymbols.getInstance())

    var onItemsChanged: ((GraphicEqNodeAdapter) -> Unit)? = null
    var onItemClicked: ((GraphicEqNode, Int) -> Unit)? = null

    private val callback = object : ObservableList.OnListChangedCallback<ObservableArrayList<GraphicEqNode>>() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onChanged(sender: ObservableArrayList<GraphicEqNode>?) {
            this@GraphicEqNodeAdapter.notifyDataSetChanged()
            onItemsChanged()
        }

        override fun onItemRangeChanged(
            sender: ObservableArrayList<GraphicEqNode>?,
            positionStart: Int,
            itemCount: Int,
        ) {
            this@GraphicEqNodeAdapter.notifyItemRangeChanged(positionStart, itemCount)
            onItemsChanged()
        }

        override fun onItemRangeInserted(
            sender: ObservableArrayList<GraphicEqNode>?,
            positionStart: Int,
            itemCount: Int,
        ) {
            this@GraphicEqNodeAdapter.notifyItemRangeInserted(positionStart, itemCount)
            onItemsChanged()
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onItemRangeMoved(
            sender: ObservableArrayList<GraphicEqNode>?,
            fromPosition: Int,
            toPosition: Int,
            itemCount: Int,
        ) {
            this@GraphicEqNodeAdapter.notifyDataSetChanged()
            onItemsChanged()
        }

        override fun onItemRangeRemoved(
            sender: ObservableArrayList<GraphicEqNode>?,
            positionStart: Int,
            itemCount: Int,
        ) {
            this@GraphicEqNodeAdapter.notifyItemRangeRemoved(positionStart, itemCount)
            onItemsChanged()
        }
    }

    private fun onItemsChanged() {
        this.onItemsChanged?.invoke(this)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val freq: TextView
        val gain: TextView
        val deleteButton: Button

        init {
            freq = view.findViewById(R.id.freq)
            gain = view.findViewById(R.id.gain)
            deleteButton = view.findViewById(R.id.delete)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        nodes.addOnListChangedCallback(callback)
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        nodes.removeOnListChangedCallback(callback)
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_geq_node_list, viewGroup, false)

        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Reset view
        viewHolder.deleteButton.isEnabled = true

        // Set content
        df.maximumFractionDigits = 1
        viewHolder.freq.text = "${df.format(nodes[position].freq)}Hz"
        df.maximumFractionDigits = 4
        viewHolder.gain.text = "${df.format(nodes[position].gain)}dB"

        // Set click listeners
        viewHolder.deleteButton.setOnClickListener {
            viewHolder.bindingAdapterPosition.let { pos ->
                if(pos >= 0) {
                    nodes.removeAt(pos)
                }
            }
            viewHolder.deleteButton.isEnabled = false
        }

        viewHolder.itemView.setOnClickListener {
            viewHolder.bindingAdapterPosition.let { pos ->
                nodes.getOrNull(pos)?.let {
                    onItemClicked?.invoke(it, pos)
                }
            }
        }
    }

    override fun getItemCount() = nodes.size
}
