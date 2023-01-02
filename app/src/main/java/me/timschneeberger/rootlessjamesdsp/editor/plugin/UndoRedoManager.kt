package me.timschneeberger.rootlessjamesdsp.editor.plugin

import android.text.Editable
import android.text.Selection
import android.text.TextUtils
import android.text.style.UnderlineSpan
import java.util.LinkedList
import android.text.TextWatcher
import android.widget.TextView

class UndoRedoManager(private val textView: TextView) {
    private val editHistory: EditHistory
    private val textChangeWatcher: TextChangeWatcher
    private var isUndoOrRedo = false

    init {
        editHistory = EditHistory()
        textChangeWatcher = TextChangeWatcher()
    }

    fun undo() {
        val edit: EditNode = editHistory.previous ?: return
        val text = textView.editableText
        val start = edit.start
        val end = start + (edit.after?.length ?: 0)
        isUndoOrRedo = true
        text.replace(start, end, edit.before)
        isUndoOrRedo = false
        val underlineSpans = text.getSpans(0, text.length, UnderlineSpan::class.java)
        for (span in underlineSpans) text.removeSpan(span)
        Selection.setSelection(text, start + (edit.before?.length ?: 0))
    }

    fun redo() {
        val edit: EditNode = editHistory.next ?: return
        val text = textView.editableText
        val start = edit.start
        val end = start + (edit.before?.length ?: 0)
        isUndoOrRedo = true
        text.replace(start, end, edit.after)
        isUndoOrRedo = false
        val underlineSpans = text.getSpans(0, text.length, UnderlineSpan::class.java)
        for (span in underlineSpans) text.removeSpan(span)
        Selection.setSelection(text, start + (edit.after?.length ?: 0))
    }

    fun connect() {
        textView.addTextChangedListener(textChangeWatcher)
    }

    fun disconnect() {
        textView.removeTextChangedListener(textChangeWatcher)
    }

    fun setMaxHistorySize(maxSize: Int) {
        editHistory.setMaxHistorySize(maxSize)
    }

    fun clearHistory() {
        editHistory.clear()
    }

    fun canUndo(): Boolean {
        return editHistory.position > 0
    }

    fun canRedo(): Boolean {
        return editHistory.position < editHistory.historyList.size
    }

    private class EditHistory {
        var position = 0
        private var maxHistorySize = -1
        val historyList = LinkedList<EditNode>()
        fun clear() {
            position = 0
            historyList.clear()
        }

        fun add(item: EditNode) {
            while (historyList.size > position) historyList.removeLast()
            historyList.add(item)
            position++
            if (maxHistorySize >= 0) trimHistory()
        }

        fun setMaxHistorySize(maxHistorySize: Int) {
            this.maxHistorySize = maxHistorySize
            if (this.maxHistorySize >= 0) trimHistory()
        }

        private fun trimHistory() {
            while (historyList.size > maxHistorySize) {
                historyList.removeFirst()
                position--
            }
            if (position < 0) position = 0
        }

        val current: EditNode?
            get() = if (position == 0) null else historyList[position - 1]
        val previous: EditNode?
            get() {
                if (position == 0) return null
                position--
                return historyList[position]
            }
        val next: EditNode?
            get() {
                if (position >= historyList.size) return null
                val item = historyList[position]
                position++
                return item
            }
    }

    private data class EditNode(var start: Int, var before: CharSequence?, var after: CharSequence?)
    private enum class ActionType {
        INSERT, DELETE, PASTE, NOT_DEF
    }

    private inner class TextChangeWatcher : TextWatcher {
        private var beforeChange: CharSequence? = null
        private var afterChange: CharSequence? = null
        private var lastActionType = ActionType.NOT_DEF
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            if (isUndoOrRedo) return
            beforeChange = s.subSequence(start, start + count)
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (isUndoOrRedo) return
            afterChange = s.subSequence(start, start + count)
            makeBatch(start)
        }

        private fun makeBatch(start: Int) {
            val action = actionType
            val currentNode = editHistory.current
            if (lastActionType != action || ActionType.PASTE == action || currentNode == null) {
                editHistory.add(EditNode(start, beforeChange, afterChange))
            } else {
                if (action == ActionType.DELETE) {
                    currentNode.start = start
                    currentNode.before = TextUtils.concat(beforeChange, currentNode.before)
                } else {
                    currentNode.after = TextUtils.concat(currentNode.after, afterChange)
                }
            }
            lastActionType = action
        }

        val actionType: ActionType
            get() = if (!TextUtils.isEmpty(beforeChange) && TextUtils.isEmpty(
                    afterChange)
            ) {
                ActionType.DELETE
            } else if (TextUtils.isEmpty(beforeChange) && !TextUtils.isEmpty(
                    afterChange)
            ) {
                ActionType.INSERT
            } else {
                ActionType.PASTE
            }

        override fun afterTextChanged(s: Editable) {}
    }
}