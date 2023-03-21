package me.timschneeberger.rootlessjamesdsp.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.UiContext
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.timschneeberger.rootlessjamesdsp.databinding.DialogProgressBinding
import kotlin.math.roundToInt

class ProgressDialog(
    @UiContext val context: Context,
    onCancelListener: ((DialogInterface) -> Unit)?
) {
    private val dialog: AlertDialog
    private val binding: DialogProgressBinding =
        DialogProgressBinding.inflate(context.getSystemService<LayoutInflater>()!!)

    var title: CharSequence
        set(value) {
            binding.alertTitle.text = value
        }
        get() = binding.alertTitle.text

    var unit: String = ""
        set(value) {
            field = value
            updateProgress()
        }
    var divisor: Double = 1.0
        set(value) {
            field = value
            updateProgress()
        }

    var currentProgress: Int = 0
        set(value) {
            field = value
            updateProgress()
        }
    var maxProgress: Int
        set(value) {
            binding.progress.max = value
            updateProgress()
        }
        get() = binding.progress.max

    var isIndeterminate: Boolean
        set(value) {
            binding.progress.isIndeterminate = value
            updateProgress()
        }
        get() = binding.progress.isIndeterminate

    var isCancelable: Boolean = true
        set(value) {
            field = value
            dialog.setCancelable(value)
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).isEnabled = value
        }

    init {
        dialog = MaterialAlertDialogBuilder(context)
            .setCancelable(isCancelable)
            .setOnCancelListener(onCancelListener)
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
            .setView(binding.root)
            .show()
        updateProgress()
    }

    fun dismiss() = dialog.dismiss()
    fun cancel() = dialog.cancel()

    @SuppressLint("SetTextI18n")
    private fun updateProgress() {
        binding.progressPercent.visibility = if(isIndeterminate) View.GONE else View.VISIBLE
        binding.progressNumber.visibility = if(isIndeterminate) View.GONE else View.VISIBLE

        val percent = ((currentProgress / maxProgress.toDouble()) * 100.0).roundToInt()
        binding.progressPercent.text = "$percent%"
        binding.progress.progress = currentProgress

        val current = currentProgress / divisor
        val max = maxProgress / divisor
        binding.progressNumber.text = "${String.format("%.1f", current)}/${String.format("%.1f", max)}$unit"
    }
}
