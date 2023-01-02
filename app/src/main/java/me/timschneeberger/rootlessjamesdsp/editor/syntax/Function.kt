package me.timschneeberger.rootlessjamesdsp.editor.syntax

import com.amrdeveloper.codeview.Code

class Function(private val title: String?, private val prefix: String? = title) : Code {
    override fun getCodeTitle(): String? { return title }
    override fun getCodePrefix(): String? { return prefix }
    override fun getCodeBody(): String? { return prefix }
}