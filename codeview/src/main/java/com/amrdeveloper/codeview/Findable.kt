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

/**
 * Interface used to support find and match features
 *
 * @since 1.2.1
 */
interface Findable {
    /**
     * Find all the the tokens that matches the regex string and save them on a list
     * @param regex The regex used to find tokens
     * @return List of the matches Tokens
     */
    fun findMatches(regex: String): List<Token>

    /**
     * Highlight and return the next token
     * @return The next matched token, `null` if not found
     */
    fun findNextMatch(): Token?

    /**
     * Highlight and return the previous token
     * @return The previous matched token, `null` if not found
     */
    fun findPrevMatch(): Token?

    /**
     * Clear all the matches tokens
     */
    fun clearMatches()
}