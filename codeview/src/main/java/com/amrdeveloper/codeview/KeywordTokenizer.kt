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

import android.widget.MultiAutoCompleteTextView
import kotlin.math.max

/**
 * The default tokenizer that used in CodeView auto complete feature
 */
class KeywordTokenizer : MultiAutoCompleteTextView.Tokenizer {
    override fun findTokenStart(charSequence: CharSequence, cursor: Int): Int {
        var sequenceStr = charSequence.toString()
        sequenceStr = sequenceStr.substring(0, cursor)
        val spaceIndex = sequenceStr.lastIndexOf(" ")
        val tabIndex = sequenceStr.lastIndexOf("\t")
        val lineIndex = sequenceStr.lastIndexOf("\n")
        val bracketIndex = sequenceStr.lastIndexOf("(")
        val index = max(0, max(spaceIndex, max(tabIndex, max(lineIndex, bracketIndex))))
        if (index == 0) return 0
        return if (index + 1 < charSequence.length) index + 1 else index
    }

    override fun findTokenEnd(charSequence: CharSequence, cursor: Int): Int {
        return charSequence.length
    }

    override fun terminateToken(charSequence: CharSequence): CharSequence {
        return charSequence
    }
}