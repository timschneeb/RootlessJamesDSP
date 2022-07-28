package me.timschneeberger.rootlessjamesdsp.preference

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference.SummaryProvider
import me.timschneeberger.rootlessjamesdsp.R
import java.io.File


class FileLibraryPreference(context: Context, attrs: AttributeSet) :
    ListPreference(context, attrs,
        androidx.preference.R.attr.dialogPreferenceStyle,
        androidx.preference.R.attr.preferenceFragmentListStyle
    ) {

    var type: String
    var directory: File?

    init {
        with(context.obtainStyledAttributes(attrs, R.styleable.FileLibraryPreference)) {
            type = getString(R.styleable.FileLibraryPreference_type) ?: "unknown"
        }
        directory = context.getExternalFilesDir(type)
        refresh()

        if(type != "unknown")
            directory?.mkdir()

        summaryProvider = SummaryProvider<ListPreference> {
            if(it.value == null || it.value.isBlank())
                if(isLiveprog()) context.getString(R.string.liveprog_no_script_selected) else context.getString(
                                    R.string.filelibrary_no_file_selected)
            else
                it.entry
        }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getString(index) as String
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        value = getPersistedString((defaultValue as? String) ?: "")
    }

    override fun onClick() {
        refresh()
        super.onClick()
    }

    fun showDialog() {
        refresh()
        preferenceManager.showDialog(this)
    }

    private fun refresh() {
        if(directory == null)
        {
            Toast.makeText(context, context.getString(R.string.filelibrary_access_fail), Toast.LENGTH_SHORT).show()
            return
        }

        entries = createFileStringArray(false)
        entryValues = createFileStringArray(true)
    }

    private fun createFileStringArray(fullPath: Boolean): Array<String> {
        val result = arrayListOf<String>()
        directory?.list()?.forEach {
            val name = if(fullPath) (directory!!.absolutePath + "/" + it) else it.substringBeforeLast('.')
            if(hasCorrectExtension(it))
            {
                result.add(name)
            }
        }

        result.sort()
        return result.toTypedArray()
    }

    fun hasCorrectExtension(it: String): Boolean {
       return (isIrs() && hasIrsExtension(it)) ||
               (isVdc() && hasVdcExtension(it)) ||
               (isLiveprog() && hasLiveprogExtension(it))
    }

    fun isLiveprog(): Boolean {
        return type.lowercase() == "liveprog"
    }
    fun isVdc(): Boolean {
        return type.lowercase() == "ddc"
    }
    fun isIrs(): Boolean {
        return type.lowercase() == "convolver"
    }

    companion object {
        fun hasIrsExtension(it: String): Boolean {
            return it.endsWith(".flac") || it.endsWith(".wav") ||  it.endsWith(".irs")
        }
        fun hasLiveprogExtension(it: String): Boolean {
            return it.endsWith(".eel")
        }
        fun hasVdcExtension(it: String): Boolean {
            return it.endsWith(".vdc")
        }
    }
}