package me.timschneeberger.rootlessjamesdsp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.model.room.BlockedApp

class AppBlocklistAdapter : ListAdapter<BlockedApp, AppBlocklistAdapter.AppBlocklistViewHolder>(BlockedAppComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppBlocklistViewHolder {
        return AppBlocklistViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_blocked_app_list, parent, false) as ViewGroup
        )
    }

    override fun onBindViewHolder(holder: AppBlocklistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun interface OnItemClickListener {
        fun onItemClick(appInfo: BlockedApp)
    }

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    inner class AppBlocklistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val titleTextView: TextView = itemView.findViewById(android.R.id.title)
        private val packageTextView: TextView = itemView.findViewById(android.R.id.summary)
        private val iconView: ImageView = itemView.findViewById(android.R.id.icon)
        private var app: BlockedApp? = null

        init {
            itemView.setOnClickListener(this)
        }

        @SuppressLint("SetTextI18n")
        fun bind(app: BlockedApp) {
            this.app = app
            this.titleTextView.text = app.appName
            this.packageTextView.text = "${app.packageName} (UID: ${app.uid})"
            this.iconView.setImageDrawable(app.appIcon)
        }

        override fun onClick(v: View) {
            app?.let {
                onItemClickListener?.onItemClick(it)
            }
        }
    }

    class BlockedAppComparator : DiffUtil.ItemCallback<BlockedApp>() {
        override fun areItemsTheSame(oldItem: BlockedApp, newItem: BlockedApp): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: BlockedApp, newItem: BlockedApp): Boolean {
            return oldItem.uid == newItem.uid && oldItem.packageName == newItem.packageName
        }
    }
}