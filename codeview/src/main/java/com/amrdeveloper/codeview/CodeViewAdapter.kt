/*
 * MIT License
 *
 * Copyright (c) 2020 AmrDeveloper (Amr Hesham)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.amrdeveloper.codeview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import java.util.Locale

/**
 * Custom base adapter that to use it in CodeView auto complete and snippets feature
 *
 * CodeViewAdapter supports to take a list of code which can include Keywords and snippets
 *
 * @since 1.1.0
 */
open class CodeViewAdapter(
    context: Context,
    private val codeViewLayoutId: Int,
    private val codeViewTextViewId: Int,
    private var codeList: MutableList<Code>
) : BaseAdapter(), Filterable {
    private var originalCodes: List<Code>? = null
    private val layoutInflater: LayoutInflater
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return (convertView ?: layoutInflater.inflate(codeViewLayoutId, parent, false)).apply {
            codeList.getOrNull(position)?.let {
                findViewById<TextView>(codeViewTextViewId)?.text = it.codeTitle
            }
        }
    }

    override fun getCount(): Int {
        return codeList.size
    }

    override fun getItem(position: Int): Any {
        return codeList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    /**
     * Update the current code list with new list
     * @param newCodeList The new code list
     */
    fun updateCodes(newCodeList: List<Code>?) {
        codeList.clear()
        codeList.addAll(newCodeList!!)
        notifyDataSetChanged()
    }

    /**
     * Clear the current code list and notify data set changed
     */
    fun clearCodes() {
        codeList.clear()
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return codeFilter
    }

    private val codeFilter: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            val suggestions: MutableList<Code> = ArrayList()
            if (originalCodes == null) {
                originalCodes = ArrayList(codeList)
            }
            if (constraint.isNullOrEmpty()) {
                results.values = originalCodes
                results.count = originalCodes!!.size
            } else {
                val filterPattern =
                    constraint.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }
                for (item in originalCodes!!) {
                    if (item.codePrefix.lowercase(Locale.getDefault()).contains(filterPattern)) {
                        suggestions.add(item)
                    }
                }
                results.values = suggestions
                results.count = suggestions.size
            }
            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            codeList = results?.values as? MutableList<Code> ?: mutableListOf()
            notifyDataSetChanged()
        }

        override fun convertResultToString(resultValue: Any): CharSequence {
            return (resultValue as Code).codeBody
        }
    }

    init {
        layoutInflater = LayoutInflater.from(context)
    }
}