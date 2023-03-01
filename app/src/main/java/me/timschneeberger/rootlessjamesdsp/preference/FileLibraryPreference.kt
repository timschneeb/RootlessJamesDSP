package me.timschneeberger.rootlessjamesdsp.preference

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference.SummaryProvider
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.model.Preset
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.toast
import java.io.File
import java.io.InputStream


class FileLibraryPreference(context: Context, attrs: AttributeSet?) :
    ListPreference(context, attrs,
        androidx.preference.R.attr.dialogPreferenceStyle,
        androidx.preference.R.attr.preferenceFragmentListStyle
    ) {

    var directory: File? = null
    var type: String = "unknown"
        set(value) {
            field = value

            summaryProvider = SummaryProvider<ListPreference> {
                if(it.entry.isNullOrBlank())
                    if(isLiveprog()) context.getString(R.string.liveprog_no_script_selected) else context.getString(
                        R.string.filelibrary_no_file_selected)
                else
                    it.entry
            }

            directory = File(context.getExternalFilesDir(null), type)
            if(type.lowercase() != "unknown")
                directory?.mkdir()
            refresh()
        }

    init {
        with(context.obtainStyledAttributes(attrs, R.styleable.FileLibraryPreference)) {
            type = getString(R.styleable.FileLibraryPreference_type) ?: "unknown"
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

    fun refresh() {
        if(directory == null)
        {
            context.toast(context.getString(R.string.filelibrary_access_fail), false)
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
                (isLiveprog() && hasLiveprogExtension(it)) ||
                (isPreset() && hasPresetExtension(it))
    }

    fun hasValidContent(stream: InputStream): Boolean {
        return if (isPreset())
            Preset.validate(stream)
        else
            true
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
    fun isPreset(): Boolean {
        return type.lowercase() == "presets"
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
        fun hasPresetExtension(it: String): Boolean {
            return it.endsWith(".tar")
        }
    }
}