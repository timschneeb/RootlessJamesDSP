package me.timschneeberger.rootlessjamesdsp.fragment

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.ListAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.preference.DialogPreference.TargetFragment
import androidx.preference.ListPreferenceDialogFragmentCompat
import androidx.preference.Preference
import com.google.android.material.chip.Chip
import kotlinx.coroutines.*
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.MainApplication
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.activity.LiveprogEditorActivity
import me.timschneeberger.rootlessjamesdsp.databinding.DialogFilelibraryBinding
import me.timschneeberger.rootlessjamesdsp.interop.JdspImpResToolbox
import me.timschneeberger.rootlessjamesdsp.liveprog.EelParser
import me.timschneeberger.rootlessjamesdsp.model.preset.Preset
import me.timschneeberger.rootlessjamesdsp.preference.FileLibraryPreference
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showInputAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import me.timschneeberger.rootlessjamesdsp.utils.storage.StorageUtils
import timber.log.Timber
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt


class FileLibraryDialogFragment : ListPreferenceDialogFragmentCompat(), TargetFragment {

    private val fileLibPreference by lazy {
        preference as FileLibraryPreference
    }

    private var clickedEntryValue: CharSequence? = null
    private lateinit var dialog: AlertDialog
    private lateinit var importLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: DialogFilelibraryBinding

    private val eelParser = EelParser()
    private val scriptScannerScope = CoroutineScope(Dispatchers.IO)
    private var currentTag: String? = null
    private var currentTagScripts: List<String>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog = super.onCreateDialog(savedInstanceState) as AlertDialog
        // Workaround to prevent the button from closing the dialog
        dialog.setOnShowListener {
            if(fileLibPreference.isPreset() && dialog.listView.adapter.isEmpty) {
                requireContext().toast(getString(R.string.filelibrary_no_presets))
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
                                    val overwritten = file.exists()
                                    val success = Preset(file.name).save()
                                    if(overwritten && success)
                                        requireContext().toast(getString(R.string.filelibrary_preset_overwritten, file.nameWithoutExtension))
                                    else if(!overwritten && success)
                                        requireContext().toast(getString(R.string.filelibrary_preset_created, file.nameWithoutExtension))
                                    else
                                        requireContext().toast(getString(R.string.filelibrary_preset_save_failed))

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
            val item = dialog.listView.adapter.getItem(position) as Entry
            val name = item.name
            val path = FileLibraryPreference.createFullPathCompat(requireContext(), item.value.toString())

            val popupMenu = PopupMenu(requireContext(), view)
            popupMenu.menuInflater.inflate(R.menu.menu_filelibrary_context, popupMenu.menu)
            popupMenu.menu.findItem(R.id.duplicate_selection).isVisible =
                fileLibPreference.isLiveprog() || fileLibPreference.isPreset()
            popupMenu.menu.findItem(R.id.edit_selection).isVisible = fileLibPreference.isLiveprog()
            popupMenu.menu.findItem(R.id.overwrite_selection).isVisible = fileLibPreference.isPreset()
            popupMenu.menu.findItem(R.id.resample_selection).isVisible = fileLibPreference.isIrs()

            popupMenu.setOnMenuItemClickListener { menuItem ->
                val selectedFile = File(path.toString())
                when (menuItem.itemId) {
                    R.id.resample_selection -> {
                        if(fileLibPreference.isIrs()) {
                            var targetRate = (requireActivity().application as MainApplication).engineSampleRate.roundToInt()
                            if (targetRate <= 0) {
                                targetRate = requireContext().getSystemService<AudioManager>()
                                    ?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
                                    ?.let { str -> Integer.parseInt(str).takeUnless { it == 0 } } ?: 48000
                                Timber.w("resample: engine sample rate is zero, using HAL rate instead")
                            }

                            Timber.d("resample: Resampling ${selectedFile.name} to ${targetRate}Hz")

                            CoroutineScope(Dispatchers.IO).launch {
                                val newName = JdspImpResToolbox.OfflineAudioResample(
                                    (selectedFile.absoluteFile.parentFile?.absolutePath + "/"),
                                    selectedFile.name,
                                    targetRate
                                )

                                withContext(Dispatchers.Main) {
                                    try {
                                        if (newName == "Invalid")
                                            requireContext().toast(getString(R.string.filelibrary_resample_failed))
                                        else
                                            requireContext().toast(getString(R.string.filelibrary_resample_complete,
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
                            if(Preset(selectedFile.name).save())
                                requireContext().toast(getString(R.string.filelibrary_preset_overwritten, name))
                            else
                                requireContext().toast(getString(R.string.filelibrary_preset_save_failed))
                        }
                        refresh()
                    }
                    R.id.edit_selection -> {
                        if(fileLibPreference.isLiveprog()) {
                            val intent = Intent(requireContext(), LiveprogEditorActivity::class.java)
                            intent.putExtra(LiveprogEditorActivity.EXTRA_TARGET_FILE, selectedFile.absolutePath)
                            startActivity(intent)
                        }
                        dismiss()
                    }
                    R.id.rename_selection -> {
                        showFileNamePrompt(
                            R.string.filelibrary_context_rename,
                            selectedFile,
                            autofill = true,
                            allowOverwrite = false
                        ) {
                            selectedFile.renameTo(it)
                            requireContext().toast(getString(R.string.filelibrary_renamed, it.nameWithoutExtension))
                            refresh()
                        }
                    }
                    R.id.delete_selection -> {
                        selectedFile.delete()
                        requireContext().toast(getString(R.string.filelibrary_deleted, name))
                        refresh()

                        // If this file was active, we need to reset the selection to null
                        if (fileLibPreference.callChangeListener("")) {
                            fileLibPreference.value = ""
                        }
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

        refreshSelection()

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK)
                return@registerForActivityResult

            result?.data?.data?.let { uri ->
                val correctType = fileLibPreference.hasCorrectExtension(
                    StorageUtils.queryName(
                        requireContext(),
                        uri
                    ) ?: "INVALID"
                )
                if(!correctType)
                {
                    requireContext().showAlert(R.string.filelibrary_unsupported_format_title,
                        R.string.filelibrary_unsupported_format)
                    return@let
                }

                StorageUtils.openInputStreamSafe(requireContext(), uri)?.use {
                    if(!fileLibPreference.hasValidContent(it)) {
                        Timber.e("File rejected due to invalid content")
                        requireContext().showAlert(R.string.filelibrary_corrupted_title,
                            R.string.filelibrary_corrupted)
                        return@let
                    }
                }

                val file = StorageUtils.importFile(requireContext(),
                    fileLibPreference.directory?.absolutePath ?: "", uri)
                if(file == null)
                {
                    Timber.e("Failed to import file")
                    return@let
                }

                CoroutineScope(Dispatchers.Main).launch {
                    delay(150L)
                    refresh()
                }
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        scriptScannerScope.cancel()

        if (positiveResult && !clickedEntryValue.isNullOrEmpty()) {
            val value = clickedEntryValue.toString()

            if (fileLibPreference.callChangeListener(value)) {
                fileLibPreference.value = value
            }
        }
    }

    private fun showFileNamePrompt(
        @StringRes title: Int,
        selectedFile: File,
        autofill: Boolean,
        allowOverwrite: Boolean,
        callback: (File) -> Unit,
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
                    requireContext().toast(getString(R.string.filelibrary_file_exists))
                    return@showInputAlert
                }
                callback.invoke(newFile)
            }
        }
    }

    private fun refresh() {
        fileLibPreference.refresh()
        dialog.listView.adapter = createAdapter()

        onTagClicked(currentTag, currentTagScripts)
        refreshSelection()
    }

    private fun refreshSelection() {
        if (fileLibPreference.isPreset())
            return

        val selectedIndex = (dialog.listView.adapter as? ListItemAdapter)?.indexOf(fileLibPreference.value) ?: -1
        if (selectedIndex >= 0) {
            dialog.listView.setItemChecked(selectedIndex, true)
            dialog.listView.setSelection(selectedIndex)
        }
        else {
            dialog.listView.setItemChecked(-1, true)
        }
    }

    private fun onTagClicked(tag: String?, scripts: List<String>?) {
        currentTag = tag
        currentTagScripts = scripts
        Timber.e(tag)
        Timber.e(scripts?.joinToString(";"))

        (dialog.listView.adapter as Filterable).filter.filter(scripts?.joinToString(";"))
    }

    private fun scanScriptMetadata() {
        if(!fileLibPreference.isLiveprog())
            return

        scriptScannerScope.launch {
            binding.tags.removeAllViews()

            val untaggedScripts = mutableListOf<String>()
            val foundTags = mutableMapOf<String /* tag */, MutableList<String> /* scripts */>()

            fileLibPreference.entryValues.forEach { path ->
                context?.let {
                    eelParser.load(
                        FileLibraryPreference.createFullPathCompat(it, path.toString()),
                        skipProperties = true
                    )
                } ?: return@forEach

                if(eelParser.tags.isEmpty())
                    eelParser.fileName?.let(untaggedScripts::add)
                eelParser.tags.forEach { tag ->
                    val prettyfied = tag.lowercase()
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        .run { if(length <= 3) uppercase() else this }


                    eelParser.fileName?.let {
                        if (foundTags.containsKey(prettyfied))
                            foundTags[prettyfied]?.add(it)
                        else
                            foundTags[prettyfied] = mutableListOf(it)
                    }
                }
            }

            if(untaggedScripts.isNotEmpty())
                foundTags["Untagged"] = ((foundTags["Untagged"] ?: listOf()) + untaggedScripts).toMutableList()

            withContext(Dispatchers.Main) {
                val sorted = foundTags.entries
                    .sortedWith(compareByDescending<Map.Entry<String, List<String>>> { it.value.size }
                        .thenBy { it.key })

                for((tag, scripts) in sorted) {
                    binding.tags.addView(Chip(dialog.context, null,
                        com.google.android.material.R.style.Widget_Material3_Chip_Assist_Elevated)
                        .apply {
                            text = tag
                            isCheckable = true
                            setOnClickListener { if(isChecked) onTagClicked(tag, scripts) }
                        })
                }
                binding.tags.setOnCheckedStateChangeListener { _, checkedIds ->
                    if(checkedIds.isEmpty())
                        onTagClicked(null, null)
                }
            }
        }
    }

    @SuppressLint("PrivateResource")
    private fun createAdapter(): ListAdapter {
        return ListItemAdapter(
            requireContext(),
            if (fileLibPreference.isPreset()) R.layout.item_preset_list
            else com.google.android.material.R.layout.select_dialog_singlechoice_material,
            android.R.id.text1,
            fileLibPreference.entries.zip(fileLibPreference.entryValues){
                    a, b -> Entry(a, b)
            }.toTypedArray(),
            fileLibPreference.isLiveprog()
        ) {
            refreshSelection()
        }
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        binding = DialogFilelibraryBinding.inflate(layoutInflater)
        binding.tags.isSingleSelection = true
        binding.tags.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }
        binding.root.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }

        builder.setView(binding.root)
        builder.setNeutralButton(getString(R.string.action_import)) { _, _ -> }

        if(fileLibPreference.isPreset()) {
            builder.setNeutralButton(getString(R.string.add)) { _, _ -> }
            builder.setNegativeButton(getString(R.string.close)) { _, _ -> }
            builder.setTitle(getString(R.string.action_presets))
        }

        builder.setAdapter(createAdapter()) { _, position ->
            val item = dialog.listView.adapter.getItem(position) as Entry
            val name = item.name
            val value = item.value

            if(fileLibPreference.isPreset()) {
                try {
                    Preset(File(value.toString()).name).load()
                    requireContext().toast(getString(R.string.filelibrary_preset_loaded, name))
                }
                catch (ex: Exception) {
                    requireContext().showAlert(getString(R.string.filelibrary_corrupted_title),
                        ex.localizedMessage ?: getString(R.string.filelibrary_preset_load_failed, name))
                }
            }

            clickedEntryValue = value

            // Simulate positive button press and dismiss
            this.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
            dialog.dismiss()
        }

        scanScriptMetadata()
    }

    private fun import() {
        try {
            importLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            })
        }
        catch(ex: Exception) {
            Timber.e("No activity found")
            Timber.i(ex)
            requireContext().toast(R.string.no_activity_found)
        }
    }

    data class Entry(val name: CharSequence, val value: CharSequence) {
        override fun toString() = name.toString()
    }

    private inner class ListItemAdapter(
        context: Context, resource: Int, textViewResourceId: Int, val allItems: Array<Entry>,
        val allowFilter: Boolean = false, val onFiltered: () -> Unit
    ) : ArrayAdapter<Entry>(context, resource, textViewResourceId, allItems), Filterable {
        private var items: Array<Entry> = allItems

        fun indexOf(value: String): Int {
            return items.map { it.value }.indexOf(value)
        }
        override fun hasStableIds(): Boolean = true
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Entry = items[position]
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getFilter(): Filter {
            return object : Filter() {
                @Suppress("UNCHECKED_CAST")
                override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults) {
                    items = filterResults.values as Array<Entry>
                    notifyDataSetChanged()
                    onFiltered.invoke()
                }

                override fun performFiltering(charSequence: CharSequence?): FilterResults {
                    if(!allowFilter)
                        return FilterResults().apply { values = allItems }

                    val query = charSequence?.toString()?.split(";")
                    return FilterResults().apply {
                        values = if (query.isNullOrEmpty())
                            allItems
                        else
                            allItems.filter { query.contains(it.name) }.toTypedArray()
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Preference?> findPreference(key: CharSequence): T? {
        return if(key == arguments?.getString(BUNDLE_KEY))
            fileLibPreference as? T
        else
            null
    }

    companion object {
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