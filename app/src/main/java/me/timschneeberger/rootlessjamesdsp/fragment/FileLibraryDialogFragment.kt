package me.timschneeberger.rootlessjamesdsp.fragment

import android.R.attr.button
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.preference.ListPreferenceDialogFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.preference.FileLibraryPreference
import me.timschneeberger.rootlessjamesdsp.utils.ContextExtensions.showAlert
import me.timschneeberger.rootlessjamesdsp.utils.StorageUtils
import timber.log.Timber
import java.io.File


class FileLibraryDialogFragment : ListPreferenceDialogFragmentCompat() {

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
            val name = getFileLibraryPreference().entries[position]
            val path = getFileLibraryPreference().entryValues[position]

            val popupMenu = PopupMenu(requireContext(), view)
            popupMenu.menuInflater.inflate(R.menu.menu_filelibrary_context, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                val selectedFile = File(path.toString())
                when (menuItem.itemId) {
                    R.id.delete_selection -> {
                        selectedFile.delete()
                        // Refresh by re-opening alert dialog
                        this.dismiss()
                        getFileLibraryPreference().showDialog()
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

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        builder.setView(R.layout.filelibrary_dialog)
        builder.setNeutralButton("Import") { _, _ -> }
    }

    private fun getFileLibraryPreference(): FileLibraryPreference {
        return preference as FileLibraryPreference
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

                val correctType = getFileLibraryPreference().hasCorrectExtension(
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
                    getFileLibraryPreference().directory?.absolutePath ?: "", uri)
                if(file == null)
                {
                    Timber.e("Failed to import file")
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    getFileLibraryPreference().showDialog()
                }, 150)
            }
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