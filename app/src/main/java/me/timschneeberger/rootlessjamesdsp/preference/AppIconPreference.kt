package me.timschneeberger.rootlessjamesdsp.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.PreferenceAppiconBinding
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.getAppName

class AppIconPreference : Preference {

    constructor(
        context: Context, attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : this(context, attrs, defStyleAttr, 0)

    constructor(
        context: Context, attrs: AttributeSet?,
    ) : this(context, attrs, androidx.preference.R.attr.preferenceStyle)

    constructor(
        context: Context,
    ) : this(context, null)

    constructor(
        context: Context, attrs: AttributeSet?, defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        layoutResource = R.layout.preference_appicon
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val drawable = context.applicationInfo.loadIcon(context.packageManager)
        val binding = PreferenceAppiconBinding.bind(holder.itemView)
        binding.preferenceAppicon.setImageDrawable(drawable)
        binding.title.text = context.getAppName()
    }
}