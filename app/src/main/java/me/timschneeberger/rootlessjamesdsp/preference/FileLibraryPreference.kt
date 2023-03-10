package me.timschneeberger.rootlessjamesdsp.preference

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.ListPreference
import androidx.preference.Preference.SummaryProvider
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.model.Preset
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
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
        // Convert old full path convention to new relative paths
        val init = getPersistedString((defaultValue as? String) ?: "")
        value = if(init.startsWith("/"))
            File(init).toRelativeString(context.getExternalFilesDir(null)!!)
        else
            init
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
            val name = if(fullPath) (File(directory!!, it).toRelativeString(context.getExternalFilesDir(null)!!)) else it.substringBeforeLast('.')
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
        val types = mapOf(
            "Convolver" to listOf(".flac", ".wav", ".irs"),
            "Liveprog" to listOf(".eel"),
            "DDC" to listOf(".vdc"),
            "Presets" to listOf(".tar")
        )

        fun hasIrsExtension(it: String): Boolean {
            return types["Convolver"]!!.any { ext -> it.endsWith(ext) }
        }
        fun hasLiveprogExtension(it: String): Boolean {
            return types["Liveprog"]!!.any { ext -> it.endsWith(ext) }
        }
        fun hasVdcExtension(it: String): Boolean {
            return types["DDC"]!!.any { ext -> it.endsWith(ext) }
        }
        fun hasPresetExtension(it: String): Boolean {
            return types["Presets"]!!.any { ext -> it.endsWith(ext) }
        }


        /**
         * If path starts at root, it is already a full path.
         * If path is relative to the external files dir, it needs to be changed to use a full path.
         */
        fun createFullPathCompat(context: Context, path: String): String {
            return if(path.startsWith("/"))
                path
            else
                context.getExternalFilesDir(null)!!.absolutePath + "/" + path
        }

    }
}