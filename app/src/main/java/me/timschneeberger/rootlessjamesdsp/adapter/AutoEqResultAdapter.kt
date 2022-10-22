package me.timschneeberger.rootlessjamesdsp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.model.api.AeqSearchResult

class AutoEqResultAdapter(
    var results: Array<AeqSearchResult>
) :
    RecyclerView.Adapter<AutoEqResultAdapter.AutoEqResultViewHolder>() {
    var onClickListener: ((AeqSearchResult) -> Unit)? = null

    override fun getItemCount(): Int {
        return results.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AutoEqResultViewHolder {
        val layoutView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_autoeq_profile_list, parent, false)
        return AutoEqResultViewHolder(layoutView)
    }

    override fun onBindViewHolder(
        holder: AutoEqResultViewHolder,
        position: Int
    ) {
        holder.title!!.text = results[position].name
        holder.subtitle!!.text = results[position].source
        holder.container.setOnClickListener{
            onClickListener?.invoke(results[holder.bindingAdapterPosition])
        }
    }

    inner class AutoEqResultViewHolder(
        itemView: View
    ) :
        RecyclerView.ViewHolder(itemView) {
        var container: LinearLayout = itemView as LinearLayout
        var subtitle: TextView? = itemView.findViewById<View>(R.id.subtitle) as TextView
        var title: TextView? = itemView.findViewById<View>(R.id.title) as TextView
    }
}