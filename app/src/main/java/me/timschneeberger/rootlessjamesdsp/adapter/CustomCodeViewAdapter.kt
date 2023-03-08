package me.timschneeberger.rootlessjamesdsp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.amrdeveloper.codeview.Code
import com.amrdeveloper.codeview.CodeViewAdapter
import com.amrdeveloper.codeview.Keyword
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.editor.syntax.Function

class CustomCodeViewAdapter(
    context: Context,
    codes: List<Code>,
) : CodeViewAdapter(context, R.layout.item_editor_autocomplete, 0, codes.toMutableList()) {

    private val layoutInflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: layoutInflater.inflate(R.layout.item_editor_autocomplete, parent, false)

        val codeType = view!!.findViewById<ImageView>(R.id.code_type)
        val codeTitle = view.findViewById<TextView>(R.id.code_title)
        val currentCode = getItem(position) as Code?
        if (currentCode != null) {
            codeTitle.text = currentCode.codeTitle
            when (currentCode) {
                is Function -> codeType.setImageResource(R.drawable.ic_function_variant)
                is Keyword -> codeType.setImageResource(R.drawable.ic_key)
                else -> codeType.setImageResource(R.drawable.ic_code_json)
            }
        }
        return view
    }
}