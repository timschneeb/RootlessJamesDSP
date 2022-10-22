package me.timschneeberger.rootlessjamesdsp.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.preference.ListPreferenceDialogFragmentCompat
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.preference.FileLibraryPreference
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.showAlert
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.showInputAlert
import me.timschneeberger.rootlessjamesdsp.utils.StorageUtils
import timber.log.Timber
import java.io.File


class FileLibraryDialogFragment : ListPreferenceDialogFragmentCompat() {

    private val fileLibPreference by lazy {
        if(requireArguments().getBoolean(BUNDLE_PRESET_TYPE))
            FileLibraryPreference(requireContext(), null).apply {
                this.type = "presets"
            }
        else
            preference as FileLibraryPreference
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as AlertDialog
        // Workaround to prevent the button from closing the dialog
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
                import()
            }
        }
        dialog.listView.setOnItemLongClickListener {
                _, view, position, _ ->
            val name = fileLibPreference.entries[position]
            val path = fileLibPreference.entryValues[position]

            val popupMenu = PopupMenu(requireContext(), view)
            popupMenu.menuInflater.inflate(R.menu.menu_filelibrary_context, popupMenu.menu)
            popupMenu.menu.findItem(R.id.duplicate_selection).isVisible = fileLibPreference.isLiveprog()

            popupMenu.setOnMenuItemClickListener { menuItem ->
                val selectedFile = File(path.toString())
                when (menuItem.itemId) {
                    R.id.delete_selection -> {
                        selectedFile.delete()
                        // Refresh by re-opening alert dialog
                        refresh()
                    }
                    R.id.duplicate_selection -> {
                        requireContext().showInputAlert(
                            layoutInflater,
                            R.string.filelibrary_context_duplicate,
                            R.string.filelibrary_new_file_name,
                            selectedFile.nameWithoutExtension,
                            false,
                            null
                        ) {
                            if (it != null) {
                                val newFile =
                                    File(selectedFile.parentFile!!.absolutePath + File.separator + it + "." + selectedFile.extension)
                                if (newFile.exists()) {
                                    Toast.makeText(
                                        context,
                                        getString(R.string.filelibrary_file_exists),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@showInputAlert
                                }
                                selectedFile.copyTo(newFile)
                                refresh()
                            }
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

    private fun refresh() {
        if(requireArguments().getBoolean(BUNDLE_PRESET_TYPE))
            fileLibPreference.refresh()
        else {
            this.dismiss()
            fileLibPreference.showDialog()
        }
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        builder.setView(R.layout.dialog_filelibrary)
        builder.setNeutralButton(getString(R.string.action_import)) { _, _ -> }
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
                this.dismiss()

                val file = StorageUtils.importFile(requireContext(),
                    fileLibPreference.directory?.absolutePath ?: "", uri)
                if(file == null)
                {
                    Timber.e("Failed to import file")
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    refresh()
                }, 150)
            }
        }
    }

    companion object {
        private const val IMPORT_FILE = 0x200
        private const val BUNDLE_KEY = "key"
        private const val BUNDLE_PRESET_TYPE = "isPresetType"

        fun newInstance(key: String): FileLibraryDialogFragment {
            val fragment = FileLibraryDialogFragment()

            val args = Bundle()
            args.putString(BUNDLE_KEY, key)
            args.putBoolean(BUNDLE_PRESET_TYPE, false)
            fragment.arguments = args
            return fragment
        }

        fun newPresetDialogInstance(): FileLibraryDialogFragment {
            val fragment = FileLibraryDialogFragment()

            val args = Bundle()
            args.putBoolean(BUNDLE_PRESET_TYPE, true)
            fragment.arguments = args
            return fragment
        }
    }
}