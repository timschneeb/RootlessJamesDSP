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

package com.amrdeveloper.codeview;

import android.widget.MultiAutoCompleteTextView;

/**
 * The default tokenizer that used in CodeView auto complete feature
 */
public class KeywordTokenizer implements MultiAutoCompleteTextView.Tokenizer {

    @Override
    public int findTokenStart(CharSequence charSequence, int cursor) {
        String sequenceStr = charSequence.toString();
        sequenceStr = sequenceStr.substring(0, cursor);

        int spaceIndex = sequenceStr.lastIndexOf(" ");
        int tabIndex = sequenceStr.lastIndexOf("\t");
        int lineIndex = sequenceStr.lastIndexOf("\n");
        int bracketIndex = sequenceStr.lastIndexOf("(");

        int index = Math.max(0, Math.max(spaceIndex, Math.max(tabIndex, Math.max(lineIndex, bracketIndex))));
        if(index == 0) return 0;
        return (index + 1 < charSequence.length()) ? index + 1 : index;
    }

    @Override
    public int findTokenEnd(CharSequence charSequence, int cursor) {
        return charSequence.length();
    }

    @Override
    public CharSequence terminateToken(CharSequence charSequence) {
        return charSequence;
    }

}
