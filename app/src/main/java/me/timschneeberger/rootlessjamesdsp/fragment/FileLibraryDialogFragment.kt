package me.timschneeberger.rootlessjamesdsp.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.preference.ListPreferenceDialogFragmentCompat
import kotlinx.coroutines.*
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.MainApplication
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.interop.JdspImpResToolbox
import me.timschneeberger.rootlessjamesdsp.model.Preset
import me.timschneeberger.rootlessjamesdsp.preference.FileLibraryPreference
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.showAlert
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.showInputAlert
import me.timschneeberger.rootlessjamesdsp.utils.StorageUtils
import me.timschneeberger.rootlessjamesdsp.utils.SystemServices
import timber.log.Timber
import java.io.File
import kotlin.math.roundToInt


class FileLibraryDialogFragment : ListPreferenceDialogFragmentCompat() {

    private val fileLibPreference by lazy {
        preference as FileLibraryPreference
    }

    private lateinit var dialog: AlertDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog = super.onCreateDialog(savedInstanceState) as AlertDialog
        // Workaround to prevent the button from closing the dialog
        dialog.setOnShowListener {
            if(fileLibPreference.isPreset() && dialog.listView.adapter.isEmpty) {
                showMessage(getString(R.string.filelibrary_no_presets))
            }

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
                if(fileLibPreference.isPreset()) {
                    val popupMenu = PopupMenu(requireContext(), it)
                    popupMenu.menuInflater.inflate(R.menu.menu_filelibrary_add_context, popupMenu.menu)

                    popupMenu.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.preset_import -> { import() }
                            R.id.preset_new -> {
                                showFileNamePrompt(
                                    R.string.filelibrary_context_new_preset_long,
                                    Preset("Untitled.tar").file(),
                                    autofill = false,
                                    allowOverwrite = true
                                ) { file ->
                                    if(file.exists())
                                        showMessage(getString(R.string.filelibrary_preset_overwritten, file.nameWithoutExtension))
                                    else
                                        showMessage(getString(R.string.filelibrary_preset_created, file.nameWithoutExtension))

                                    Preset(file.name).save()
                                    refresh()
                                }
                            }
                        }
                        true
                    }
                    popupMenu.show()
                }
                else
                    import()
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isVisible = false
        }

        dialog.listView.setOnItemLongClickListener {
                _, view, position, _ ->
            val name = fileLibPreference.entries[position]
            val path = fileLibPreference.entryValues[position]

            val popupMenu = PopupMenu(requireContext(), view)
            popupMenu.menuInflater.inflate(R.menu.menu_filelibrary_context, popupMenu.menu)
            popupMenu.menu.findItem(R.id.duplicate_selection).isVisible =
                fileLibPreference.isLiveprog() || fileLibPreference.isPreset()
            popupMenu.menu.findItem(R.id.overwrite_selection).isVisible = fileLibPreference.isPreset()
            popupMenu.menu.findItem(R.id.resample_selection).isVisible = fileLibPreference.isIrs()

            popupMenu.setOnMenuItemClickListener { menuItem ->
                val selectedFile = File(path.toString())
                when (menuItem.itemId) {
                    R.id.resample_selection -> {
                        if(fileLibPreference.isIrs()) {
                            var targetRate = (requireActivity().application as MainApplication).engineSampleRate.roundToInt()
                            if (targetRate <= 0) {
                                targetRate = SystemServices.get(requireContext(), AudioManager::class.java)
                                    .getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
                                    ?.let { str -> Integer.parseInt(str).takeUnless { it == 0 } } ?: 48000
                                Timber.w("resample: engine sample rate is zero, using HAL rate instead")
                            }

                            Timber.d("resample: Resampling ${selectedFile.name} to ${targetRate}Hz")

                            CoroutineScope(Dispatchers.IO).launch {
                                val newName = JdspImpResToolbox.OfflineAudioResample(
                                    (selectedFile.absoluteFile.parentFile?.absolutePath + "/") ?: "",
                                    selectedFile.name,
                                    targetRate
                                )

                                withContext(Dispatchers.Main) {
                                    try {
                                        if (newName == "Invalid")
                                            showMessage(getString(R.string.filelibrary_resample_failed))
                                        else
                                            showMessage(getString(R.string.filelibrary_resample_complete,
                                                targetRate))
                                        refresh()
                                    }
                                    catch (_: IllegalStateException) {
                                        // Context may not be attached to fragment at this point
                                    }
                                }
                            }
                        }
                        refresh()
                    }
                    R.id.overwrite_selection -> {
                        if(fileLibPreference.isPreset()) {
                            Preset(selectedFile.name).save()
                            showMessage(getString(R.string.filelibrary_preset_overwritten, name))
                        }
                        refresh()
                    }
                    R.id.rename_selection -> {
                        showFileNamePrompt(
                            R.string.filelibrary_context_rename,
                            selectedFile,
                            autofill = true,
                            allowOverwrite = false
                        ) {
                            selectedFile.renameTo(it)
                            showMessage(getString(R.string.filelibrary_renamed, it.nameWithoutExtension))
                            refresh()
                        }
                    }
                    R.id.delete_selection -> {
                        selectedFile.delete()
                        showMessage(getString(R.string.filelibrary_deleted, name))
                        refresh()
                    }
                    R.id.duplicate_selection -> {
                        showFileNamePrompt(
                            R.string.filelibrary_context_duplicate,
                            selectedFile,
                            autofill = true,
                            allowOverwrite = false
                        ) {
                            selectedFile.copyTo(it)
                            refresh()
                        }
                    }
                    R.id.share_selection -> {
                        val uri = FileProvider.getUriForFile(
                            requireContext(),
                            BuildConfig.APPLICATION_ID + ".file_library_provider",
                            selectedFile
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                        shareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        shareIntent.type = "application/octet-stream"
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.filelibrary_context_share)))
                    }
                }
                true
            }
            popupMenu.show()
            true
        }

        return dialog
    }

    private fun showMessage(text: String) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    private fun showFileNamePrompt(
        @StringRes title: Int,
        selectedFile: File,
        autofill: Boolean,
        allowOverwrite: Boolean,
        callback: (File) -> Unit
    ) {
        requireContext().showInputAlert(
            layoutInflater,
            title,
            R.string.filelibrary_new_file_name,
            if (autofill) selectedFile.nameWithoutExtension else "",
            false,
            null
        ) {
            if (it != null) {
                val newFile =
                    File(selectedFile.absoluteFile.parentFile!!.absolutePath + File.separator + it + "." + selectedFile.extension)
                if (newFile.exists() && !allowOverwrite) {
                    showMessage(getString(R.string.filelibrary_file_exists))
                    return@showInputAlert
                }
                callback.invoke(newFile)
            }
        }
    }

    private fun refresh() {
        if(fileLibPreference.isPreset()) {
            fileLibPreference.refresh()
            dialog.listView.adapter = createPresetAdapter()
        }
        else {
            // TODO refresh without closing
            this.dismiss()
            CoroutineScope(Dispatchers.Main).launch {
                delay(50L)
                fileLibPreference.showDialog()
            }
        }
    }

    private fun createPresetAdapter(): ListAdapter {
        return ListItemAdapter(
            requireContext(),
            R.layout.item_preset_list,
            android.R.id.text1,
            fileLibPreference.entries
        )
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        builder.setView(R.layout.dialog_filelibrary)
        builder.setNeutralButton(getString(R.string.action_import)) { _, _ -> }

        if(fileLibPreference.isPreset()) {
            builder.setNeutralButton(getString(R.string.add)) { _, _ -> }
            builder.setNegativeButton(getString(R.string.close)) { _, _ -> }
            builder.setTitle(getString(R.string.action_presets))
            builder.setAdapter(createPresetAdapter()) { _, position ->
                val name = fileLibPreference.entries[position]
                val path = fileLibPreference.entryValues[position]
                val result = Preset(File(path.toString()).name).load() != null
                if(result)
                    showMessage(getString(R.string.filelibrary_preset_loaded, name))
                else
                    showMessage(getString(R.string.filelibrary_preset_load_failed, name))
                this.dismiss()
            }
        }
    }

    private fun import() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(intent, IMPORT_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == IMPORT_FILE && resultCode == Activity.RESULT_OK)
        {
            data?.data?.also { uri ->

                val correctType = fileLibPreference.hasCorrectExtension(
                    StorageUtils.queryName(
                        requireContext(),
                        uri
                    )
                )
                if(!correctType)
                {
                    requireContext().showAlert(R.string.filelibrary_unsupported_format_title,
                        R.string.filelibrary_unsupported_format)
                    return
                }

                StorageUtils.openInputStreamSafe(requireContext(), uri)?.use {
                    if(!fileLibPreference.hasValidContent(it)) {
                        Timber.e("File rejected due to invalid content")
                        requireContext().showAlert(R.string.filelibrary_corrupted_title,
                            R.string.filelibrary_corrupted)
                        return@also
                    }
                }

                val file = StorageUtils.importFile(requireContext(),
                    fileLibPreference.directory?.absolutePath ?: "", uri)
                if(file == null)
                {
                    Timber.e("Failed to import file")
                    return
                }

                CoroutineScope(Dispatchers.Main).launch {
                    delay(150L)
                    refresh()
                }
            }
        }
    }

    private inner class ListItemAdapter(
        context: Context, resource: Int, textViewResourceId: Int,
        objects: Array<CharSequence?>?,
    ) :
        ArrayAdapter<CharSequence?>(context, resource, textViewResourceId, objects!!) {
        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }
    }

    companion object {
        private const val IMPORT_FILE = 0x200
        private const val BUNDLE_KEY = "key"

        fun newInstance(key: String): FileLibraryDialogFragment {
            val fragment = FileLibraryDialogFragment()

            val args = Bundle()
            args.putString(BUNDLE_KEY, key)
            fragment.arguments = args
            return fragment
        }
    }
}