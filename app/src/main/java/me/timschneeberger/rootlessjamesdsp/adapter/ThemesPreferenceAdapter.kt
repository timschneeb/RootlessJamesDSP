package me.timschneeberger.rootlessjamesdsp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.RecyclerView
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.databinding.PreferenceThemeItemBinding
import me.timschneeberger.rootlessjamesdsp.delegates.ThemingDelegate
import me.timschneeberger.rootlessjamesdsp.model.preference.AppTheme
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.getResourceColor

class ThemesPreferenceAdapter(private val context: Context,
                              private val clickListener: OnItemClickListener) :
    RecyclerView.Adapter<ThemesPreferenceAdapter.ThemeViewHolder>() {

    private var themes = emptyList<AppTheme>()
    private val preferences by lazy { context.getSharedPreferences(Constants.PREF_APP, Context.MODE_PRIVATE) }

    private lateinit var binding: PreferenceThemeItemBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val isAmoled = preferences.getBoolean(context.getString(R.string.key_appearance_pure_black), false)
        val themeResIds = ThemingDelegate.getThemeResIds(themes[viewType], isAmoled)
        val themedContext = themeResIds.fold(parent.context) {
                context, themeResId ->
            ContextThemeWrapper(context, themeResId)
        }

        binding = PreferenceThemeItemBinding.inflate(LayoutInflater.from(themedContext), parent, false)
        return ThemeViewHolder(binding.root)
    }

    override fun getItemViewType(position: Int): Int = position

    override fun getItemCount(): Int = themes.size

    override fun onBindViewHolder(holder: ThemesPreferenceAdapter.ThemeViewHolder, position: Int) {
        holder.bind(themes[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(themes: List<AppTheme>) {
        this.themes = themes
        notifyDataSetChanged()
    }

    inner class ThemeViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

        private val selectedColor = view.context.getResourceColor(com.google.android.material.R.attr.colorPrimary)
        private val unselectedColor = view.context.getResourceColor(android.R.attr.divider)

        fun bind(appTheme: AppTheme) {
            binding.name.text = view.context.getString(appTheme.titleResId!!)

            // For rounded corners
            binding.badges.clipToOutline = true

            val storedAppTheme = AppTheme.valueOf(preferences.getString(context.getString(R.string.key_appearance_app_theme), AppTheme.DEFAULT.name) ?: AppTheme.DEFAULT.name)
            val isSelected = storedAppTheme == appTheme
            binding.themeCard.isChecked = isSelected
            binding.themeCard.strokeColor = if (isSelected) selectedColor else unselectedColor

            listOf(binding.root, binding.themeCard).forEach {
                it.setOnClickListener {
                    clickListener.onItemClick(bindingAdapterPosition)
                }
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
}
