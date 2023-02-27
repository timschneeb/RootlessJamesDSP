package me.timschneeberger.rootlessjamesdsp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.model.AppInfo

class AppsListAdapter: RecyclerView.Adapter<AppsListAdapter.ViewHolder>(), Filterable {
    var dataList: List<AppInfo> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            filteredDataList = value
            notifyDataSetChanged()
        }

    private var filteredDataList: List<AppInfo> = emptyList()

    inner class ViewHolder(rootView: ViewGroup): RecyclerView.ViewHolder(rootView), View.OnClickListener {

        init { rootView.setOnClickListener(this) }

        private val titleView = rootView.findViewById<TextView>(android.R.id.title)
        private val summaryView = rootView.findViewById<TextView>(android.R.id.summary)
        private val iconView = rootView.findViewById<ImageView>(android.R.id.icon)

        var data: AppInfo? = null
            set(value) {
                field = value
                value ?: return
                titleView.text = value.appName
                summaryView.text = value.packageName
                iconView.setImageDrawable(value.icon)
            }

        override fun onClick(v: View) {
            data?.let {
                onItemClickListener?.onItemClick(it)
            }
        }
    }

    fun interface OnItemClickListener {
        fun onItemClick(appInfo: AppInfo)
    }

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app_list, parent, false) as ViewGroup
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(position >= itemCount)
            return
        holder.data = filteredDataList[position]
    }

    override fun getItemCount(): Int = filteredDataList.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString() ?: ""
                filteredDataList =
                    if (charString.isEmpty())
                        dataList
                    else {
                        val filteredList = arrayListOf<AppInfo>()
                        dataList
                            .filter {
                                it.appName.contains(constraint!!, true)
                            }
                            .forEach { filteredList.add(it) }
                        filteredList
                    }
                return FilterResults().apply { values = filteredDataList }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {

                filteredDataList = if (results?.values == null)
                    emptyList()
                else
                    results.values as List<AppInfo>
                notifyDataSetChanged()
            }
        }
    }
}