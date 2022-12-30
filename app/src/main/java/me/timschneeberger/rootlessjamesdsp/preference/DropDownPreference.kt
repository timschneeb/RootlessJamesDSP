package me.timschneeberger.rootlessjamesdsp.preference

import android.content.Context
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import me.timschneeberger.rootlessjamesdsp.R


/**
 * A [ListPreference] that presents the options in a drop down menu rather than a dialog.
 */
class DropDownPreference(
    private val mContext: Context, attrs: AttributeSet?,
    defStyleAttr: Int, defStyleRes: Int,
) :
    ListPreference(mContext, attrs, defStyleAttr, defStyleRes) {

    private var mRoot: View? = null
    private var mPopup: PopupMenu? = null

    @JvmOverloads
    constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyle: Int = androidx.preference.R.attr.dropdownPreferenceStyle,
    ) : this(context, attrs, defStyle, 0) {
        layoutResource = R.layout.preference_alt
    }

    override fun onClick() {
        mPopup?.show()
    }

    override fun setEntries(entries: Array<CharSequence?>) {
        super.setEntries(entries)
        createMenu()
    }

    private fun createMenu() {
        mPopup = mRoot?.let { PopupMenu(mContext, it) }

        entries.forEachIndexed { index, s ->
            mPopup?.menu?.add(Menu.NONE, index, Menu.CATEGORY_CONTAINER, s)
        }

        mPopup?.setOnMenuItemClickListener { menuItem: MenuItem ->
            if (menuItem.itemId >= 0) {
                val value: String = entryValues[menuItem.itemId].toString()
                if (value != getValue() && callChangeListener(value)) {
                    setValue(value)
                    notifyChanged()
                }
            }
            true
        }
    }

    override fun setValue(value: String) {
        val index = entryValues.indexOf(value)
        summary = if(index < 0) mContext.getString(R.string.value_not_set) else entries[index]
        super.setValue(value)
    }

    override fun setValueIndex(index: Int) {
        value = entryValues[index].toString()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        mRoot = holder.itemView
        createMenu()

        super.onBindViewHolder(holder)
    }
}
