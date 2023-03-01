package me.timschneeberger.rootlessjamesdsp.editor.plugin

import android.widget.EditText
import android.view.View
import android.view.accessibility.AccessibilityEvent

class SourcePositionListener(private val editText: EditText) {
    fun interface OnPositionChanged {
        fun onPositionChange(line: Int, column: Int)
    }

    private var onPositionChanged: OnPositionChanged? = null
    fun setOnPositionChanged(listener: OnPositionChanged?) {
        onPositionChanged = listener
    }

    private val viewAccessibility: View.AccessibilityDelegate =
        object : View.AccessibilityDelegate() {
            override fun sendAccessibilityEvent(host: View, eventType: Int) {
                super.sendAccessibilityEvent(host, eventType)
                if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED && onPositionChanged != null) {
                    val selectionStart = editText.selectionStart
                    val line = editText.layout?.getLineForOffset(selectionStart) ?: 0
                    val column = selectionStart - (editText.layout?.getLineStart(line) ?: 0)
                    onPositionChanged?.onPositionChange(line + 1, column + 1)
                }
            }
        }

    init {
        editText.accessibilityDelegate = viewAccessibility
    }
}