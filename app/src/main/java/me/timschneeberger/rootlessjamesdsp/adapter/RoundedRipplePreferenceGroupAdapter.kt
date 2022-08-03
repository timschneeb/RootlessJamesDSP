package me.timschneeberger.rootlessjamesdsp.adapter

import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceViewHolder
import me.timschneeberger.rootlessjamesdsp.R

@SuppressLint("RestrictedApi")
class RoundedRipplePreferenceGroupAdapter(preferenceGroup: PreferenceGroup) : PreferenceGroupAdapter(preferenceGroup) {
    override fun onBindViewHolder(
        holder: PreferenceViewHolder,
        position: Int
    ) {
        super.onBindViewHolder(holder, position)
        val preference = getItem(position)
        preference ?: return

        if(preference !is PreferenceGroup) {
            holder.itemView.background = ContextCompat.getDrawable(
                preference.context,
                R.drawable.ripple_rounded
            )
        }
    }
}