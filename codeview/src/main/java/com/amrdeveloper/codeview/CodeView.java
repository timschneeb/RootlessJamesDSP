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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Rect;
import android.os.Handler;
import android.text.Editable;

import android.text.InputFilter;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.ReplacementSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.MultiAutoCompleteTextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CodeView is a CustomView to provide a lot of features that you need to highlights
 * and creating an editor for your custom programming language or data format
 */
public class CodeView extends AppCompatMultiAutoCompleteTextView implements Findable, Replaceable {

    private int tabWidth = 0;
    private int tabLength = 0;
    private int tabWidthInCharacters;
    private int mUpdateDelayTime = 500;

    private boolean modified = true;
    private boolean highlightWhileTextChanging = true;

    private boolean hasErrors = false;
    private boolean mRemoveErrorsWhenTextChanged = true;

    // Line number options
    private Rect lineNumberRect;
    private Paint lineNumberPaint;
    private boolean enableLineNumber = false;
    private boolean enableRelativeLineNumber = false;

    // Highlighting current line options
    private Rect lineBounds;
    private Paint highlightLinePaint;
    private boolean enableHighlightCurrentLine = false;
    private final static int LINE_HIGHLIGHT_DEFAULT_COLOR = Color.DKGRAY;

    // Indentations options
    private int currentIndentation = 0;
    private boolean enableAutoIndentation = false;
    private final Set<Character> indentationStarts = new HashSet<>();
    private final Set<Character> indentationEnds = new HashSet<>();

    // Matches and tokens
    private int currentMatchedIndex = -1;
    private int matchingColor = Color.YELLOW;
    private CharacterStyle currentMatchedToken;
    private final List<Token> matchedTokens = new ArrayList<>();

    // Auto complete and Suggestions
    private int maxNumberOfSuggestions = Integer.MAX_VALUE;
    private int autoCompleteItemHeightInDp = (int) (50 * Resources.getSystem().getDisplayMetrics().density);

    // Auto pair complete
    private boolean enablePairComplete = false;
    private boolean enablePairCompleteCenterCursor = false;
    private final Map<Character, Character> mPairCompleteMap = new HashMap<>();

    private final Handler mUpdateHandler = new Handler();
    private MultiAutoCompleteTextView.Tokenizer mAutoCompleteTokenizer;

    private static final Pattern PATTERN_LINE = Pattern.compile("(^(.*)$)+", Pattern.MULTILINE);
    private static final Pattern PATTERN_TRAILING_WHITE_SPACE = Pattern.compile("[\\t ]+$", Pattern.MULTILINE);

    private final SortedMap<Integer, Integer> mErrorHashSet = new TreeMap<>();
    private final Map<Pattern, Integer> mSyntaxPatternMap = new LinkedHashMap<>();

    public CodeView(Context context) {
        super(context);
        initEditorView();
    }

    public CodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initEditorView();
    }

    public CodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initEditorView();
    }

    private void initEditorView() {
        if(mAutoCompleteTokenizer == null)
            mAutoCompleteTokenizer = new KeywordTokenizer();

        setTokenizer(mAutoCompleteTokenizer);
        setHorizontallyScrolling(true);
        setFilters(new InputFilter[]{mInputFilter});
        addTextChangedListener(mEditorTextWatcher);
        setOnKeyListener(mOnKeyListener);

        lineNumberRect = new Rect();
        lineNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lineNumberPaint.setStyle(Paint.Style.FILL);

        lineBounds = new Rect();
        highlightLinePaint = new Paint();
        highlightLinePaint.setColor(LINE_HIGHLIGHT_DEFAULT_COLOR);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (enableLineNumber  || enableHighlightCurrentLine) {
            final Editable fullText = getText();
            final Layout layout = getLayout();
            final int lineCount = getLineCount();
            final int selectionStart = Selection.getSelectionStart(fullText);
            final int cursorLine = layout.getLineForOffset(selectionStart);

            // Highlight the current line with custom color by drawing rectangle on this line
            if (enableHighlightCurrentLine) {
                getLineBounds(cursorLine, lineBounds);
                canvas.drawRect(lineBounds, highlightLinePaint);
            }

            // Draw line number or relative line number
            if (enableLineNumber) {
                for (int i = 0; i < lineCount; ++i) {
                    final int baseline = getLineBounds(i, null);
                    if (i == 0 || fullText.charAt(layout.getLineStart(i) - 1) == '\n') {
                        // If relative line number is enabled the number should be the absolute value of cursorLine - i)
                        // if not it should be just current line number
                        // Add 1 to the current line number to make it start from 1 not 0
                        int lineNumber = (i == cursorLine || !enableRelativeLineNumber) ? i + 1 : Math.abs(cursorLine - i);
                        canvas.drawText(" " + lineNumber, lineNumberRect.left, baseline, lineNumberPaint);
                    }
                }

                // Calculate padding depending on current line number
                final int paddingLeft = 50 + (int) Math.log10(lineCount) * 10;
                setPadding(paddingLeft, getPaddingTop(), getPaddingRight(), getPaddingBottom());
            }
        }
        super.onDraw(canvas);
    }

    @Override
    public List<Token> findMatches(String regex) {
        matchedTokens.clear();
        if (regex.isEmpty()) return matchedTokens;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(getText());
        while (matcher.find()) matchedTokens.add(new Token(matcher.start(), matcher.end()));
        return matchedTokens;
    }

    @Override
    public Token findNextMatch() {
        if (matchedTokens.isEmpty()) return null;
        currentMatchedIndex++;
        if (currentMatchedIndex >= matchedTokens.size()) currentMatchedIndex = 0;
        Token currentMatch = matchedTokens.get(currentMatchedIndex);
        clearHighlightingMatchingToken();
        highlightMatchingToken(currentMatch);
        return currentMatch;
    }

    @Override
    public Token findPrevMatch() {
        if (matchedTokens.isEmpty()) return null;
        currentMatchedIndex--;
        if (currentMatchedIndex < 0) currentMatchedIndex = 0;
        Token currentMatch = matchedTokens.get(currentMatchedIndex);
        clearHighlightingMatchingToken();
        highlightMatchingToken(currentMatch);
        return currentMatch;
    }

    @Override
    public void clearMatches() {
        clearHighlightingMatchingToken();
        currentMatchedToken = null;
        currentMatchedIndex = -1;
        matchedTokens.clear();
    }

    @Override
    public void replaceFirstMatch(String regex, String replacement) {
        Pattern pattern = Pattern.compile(regex);
        String text = pattern.matcher(getText().toString()).replaceFirst(replacement);
        setTextHighlighted(text);
    }

    @Override
    public void replaceAllMatches(String regex, String replacement) {
        Pattern pattern = Pattern.compile(regex);
        String text = pattern.matcher(getText().toString()).replaceAll(replacement);
        setTextHighlighted(text);
    }

    private void highlightSyntax(Editable editable) {
        if(mSyntaxPatternMap.isEmpty()) return;

        Set<Map.Entry<Pattern, Integer>> syntaxSet = mSyntaxPatternMap.entrySet();
        for (Map.Entry<Pattern, Integer> syntax : syntaxSet) {
            Matcher matcher = syntax.getKey().matcher(editable);
            while (matcher.find()) {
                createForegroundColorSpan(editable, matcher, syntax.getValue());
            }
        }
    }

    private void highlightErrorLines(Editable editable) {
        if(mErrorHashSet.isEmpty()) return;
        int maxErrorLineValue = mErrorHashSet.lastKey();

        int lineNumber = 1;
        Matcher matcher = PATTERN_LINE.matcher(editable);

        while (matcher.find()) {
            if(mErrorHashSet.containsKey(lineNumber)) {
                int color = mErrorHashSet.get(lineNumber);
                createBackgroundColorSpan(editable, matcher, color);
            }
            lineNumber = lineNumber + 1;
            if(lineNumber > maxErrorLineValue) break;
        }
    }

    private void createForegroundColorSpan(Editable editable, Matcher matcher, @ColorInt int color) {
        editable.setSpan(new ForegroundColorSpan(color),
                matcher.start(), matcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void createBackgroundColorSpan(Editable editable, Matcher matcher, @ColorInt int color) {
        editable.setSpan(new BackgroundColorSpan(color),
                matcher.start(), matcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void createBackgroundColorSpan(Editable editable, int start, int end, @ColorInt int color) {
        editable.setSpan(new BackgroundColorSpan(color),
                start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void highlightMatchingToken(Token token) {
        currentMatchedToken = new BackgroundColorSpan(matchingColor);
        getEditableText().setSpan(currentMatchedToken,
                token.getStart(), token.getEnd(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void clearHighlightingMatchingToken() {
        if (currentMatchedToken == null) return;
        getEditableText().removeSpan(currentMatchedToken);
    }

    private Editable highlight(Editable editable) {
        if(editable.length() == 0) return editable;
        try {
            clearSpans(editable);
            highlightErrorLines(editable);
            highlightSyntax(editable);
        }
        catch (IllegalStateException e) {
            e.printStackTrace();
        }
        return editable;
    }

    private void highlightWithoutChange(Editable editable) {
        modified = false;
        highlight(editable);
        modified = true;
    }

    /**
     * Replace the current text with new highlighted text
     * @param text The new Text
     */
    public void setTextHighlighted(CharSequence text) {
        if (text == null || text.length() == 0) return;

        cancelHighlighterRender();

        removeAllErrorLines();

        modified = false;
        setText(highlight(new SpannableStringBuilder(text)));
        modified = true;
    }

    /**
     * Modify the tab length to use it in auto indenting feature
     * @param length The new tab length value
     */
    public void setTabLength(int length) {
        tabLength = length;
    }

    /**
     * Modify the current tab with
     * @param characters to use it to calculate the tab width
     */
    public void setTabWidth(int characters) {
        if (tabWidthInCharacters == characters) return;
        tabWidthInCharacters = characters;
        tabWidth = Math.round(getPaint().measureText("m") * characters);
    }

    private void clearSpans(Editable editable) {
        int length = editable.length();
        ForegroundColorSpan[] foregroundSpans = editable.getSpans(
                0,length, ForegroundColorSpan.class);

        for (int i = foregroundSpans.length; i-- > 0;)
            editable.removeSpan(foregroundSpans[i]);

        BackgroundColorSpan[] backgroundSpans = editable.getSpans(
                0, length, BackgroundColorSpan.class);

        for (int i = backgroundSpans.length; i-- > 0;)
            editable.removeSpan(backgroundSpans[i]);
    }

    /**
     * Stop the highlighter task
     */
    public void cancelHighlighterRender() {
        mUpdateHandler.removeCallbacks(mUpdateRunnable);
    }

    private void convertTabs(Editable editable, int start, int count) {
        if (tabWidth < 1) return;

        String s = editable.toString();

        for (int stop = start + count;
             (start = s.indexOf("\t", start)) > -1 && start < stop;
             ++start) {
            editable.setSpan(new CodeView.TabWidthSpan(),
                    start,
                    start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * Setup the syntax of your data as a map of patterns with their colors
     * @param syntaxPatterns Map of Patterns and Colors
     */
    public void setSyntaxPatternsMap(Map<Pattern, Integer> syntaxPatterns) {
        if(!mSyntaxPatternMap.isEmpty()) mSyntaxPatternMap.clear();
        mSyntaxPatternMap.putAll(syntaxPatterns);
    }

    /**
     * Add Single syntax as a Pattern with one Color
     * @param pattern Syntax feature pattern
     * @param Color Colors used when highlighting the pattern
     */
    public void addSyntaxPattern(Pattern pattern, @ColorInt int Color) {
        mSyntaxPatternMap.put(pattern, Color);
    }

    /**
     * Remove one pattern from the Syntax patterns
     * @param pattern Pattern object to remove it
     */
    public void removeSyntaxPattern(Pattern pattern) {
        mSyntaxPatternMap.remove(pattern);
    }

    /**
     * @return The current number of patterns in the syntax map
     */
    public int getSyntaxPatternsSize() {
        return mSyntaxPatternMap.size();
    }

    /**
     * Remove all syntax patterns
     */
    public void resetSyntaxPatternList() {
        mSyntaxPatternMap.clear();
    }

    /**
     * Un highlight all keywords by removing all spans
     */
    public void resetHighlighter() {
        clearSpans(getText());
    }

    /**
     * Enable or disable auto indenting feature
     * @param enableAutoIndentation Flag to enable or disable auto indenting
     */
    public void setEnableAutoIndentation(boolean enableAutoIndentation) {
        this.enableAutoIndentation = enableAutoIndentation;
    }

    /**
     * Set the indenting starts set of characters
     * @param characters Set of characters to use them as indenting starts
     * @since 1.2.1
     */
    public void setIndentationStarts(Set<Character> characters) {
        indentationStarts.clear();
        indentationStarts.addAll(characters);
    }

    /**
     * Set the indenting ends set of characters
     * @param characters Set of characters to use them as indenting ends
     * @since 1.2.1
     */
    public void setIndentationEnds(Set<Character> characters) {
        indentationEnds.clear();
        indentationEnds.addAll(characters);
    }

    /**
     * Add New Error to the current set of errors to highlight it
     * @param lineNum The error line number
     * @param color The color to highlight this error
     */
    public void addErrorLine(int lineNum, int color) {
        mErrorHashSet.put(lineNum, color);
        hasErrors = true;
    }

    /**
     * Remove one error by the line number from the error set
     * @param lineNum The error line number to remove it
     */
    public void removeErrorLine(int lineNum) {
        mErrorHashSet.remove(lineNum);
        hasErrors = mErrorHashSet.size() > 0;
    }

    /**
     * Remove all the errors from the errors set and change {@link #hasErrors} to false
     */
    public void removeAllErrorLines() {
        mErrorHashSet.clear();
        hasErrors = false;
    }

    /**
     * @return The current number of errors in the Errors set
     */
    public int getErrorsSize() {
        return mErrorHashSet.size();
    }

    /**
     * @return The current text but without any Trailing Space
     */
    public String getTextWithoutTrailingSpace() {
        return PATTERN_TRAILING_WHITE_SPACE
                .matcher(getText())
                .replaceAll("");
    }

    /**
     * Replace the current Auto Complete default tokenizer by custom one
     * @param tokenizer The new custom Tokenizer
     */
    public void setAutoCompleteTokenizer(MultiAutoCompleteTextView.Tokenizer tokenizer) {
        mAutoCompleteTokenizer = tokenizer;
    }

    /**
     * Enable or disable remove all the current errors when text is changed
     * @param removeErrors True to enable remove current error
     */
    public void setRemoveErrorsWhenTextChanged(boolean removeErrors) {
        mRemoveErrorsWhenTextChanged = removeErrors;
    }

    /**
     * Re Highlight the syntax patterns
     */
    public void reHighlightSyntax() {
        highlightSyntax(getEditableText());
    }

    /**
     * Re Highlight the current errors
     */
    public void reHighlightErrors() {
        highlightErrorLines(getEditableText());
    }

    /**
     * @return {@code true} if the errors lists is not empty
     */
    public boolean isHasError() {
        return hasErrors;
    }

    /**
     * Modify the highlighting delay time
     * @param time The new delay time
     */
    public void setUpdateDelayTime(int time) {
        mUpdateDelayTime = time;
    }

    /**
     * @return The current highlighting delay time
     */
    public int getUpdateDelayTime() {
        return mUpdateDelayTime;
    }

    /**
     * Enable or disable highlighting while text is changing
     * @param updateWhileTextChanging True to enable highlighting while text is changing
     */
    public void setHighlightWhileTextChanging(boolean updateWhileTextChanging) {
        this.highlightWhileTextChanging = updateWhileTextChanging;
    }

    /**
     * Enable or disable the line number feature
     * @param enableLineNumber Flag to enable or disable line number
     * @since 1.1.0
     */
    public void setEnableLineNumber(boolean enableLineNumber) {
        this.enableLineNumber = enableLineNumber;
    }

    /**
     * @return {@code true} if the line number is enabled
     * @since 1.1.0
     */
    public boolean isLineNumberEnabled() {
        return enableLineNumber;
    }

    /**
     * Enable or disable the relative line number feature
     * @param enableRelativeLineNumber  Flag to enable or disable line relative number
     * @since 1.3.6
     */
    public void setEnableRelativeLineNumber(boolean enableRelativeLineNumber) {
        this.enableRelativeLineNumber = enableRelativeLineNumber;
    }

    /**
     * @return (@code true) if relative line number is enabled
     * @since 1.3.6
     */
    public boolean isLineRelativeNumberEnabled() {
        return enableRelativeLineNumber;
    }

    /**
     * Enable or disable the highlighting current line feature
     * @param enableHighlightCurrentLine  Flag to enable or disable highlighting current line
     * @since 1.3.6
     */
    public void setEnableHighlightCurrentLine(boolean enableHighlightCurrentLine) {
        this.enableHighlightCurrentLine = enableHighlightCurrentLine;
    }

    /**
     * @return (@code true) if highlighting current line feature is enabled
     * @since 1.3.6
     */
    public boolean isHighlightCurrentLineEnabled() {
        return enableHighlightCurrentLine;
    }

    /**
     * Modify the highlight current line  color
     * @param color The new color value
     * @since 1.1.0
     */
    public void setHighlightCurrentLineColor(int color) {
        highlightLinePaint.setColor(color);
    }

    /**
     * Modify the line number text color
     * @param color The new color value
     * @since 1.1.0
     */
    public void setLineNumberTextColor(int color) {
        lineNumberPaint.setColor(color);
    }

    /**
     * Modify the line number text size
     * @param size The new size value
     * @since 1.1.0
     */
    public void setLineNumberTextSize(float size) {
        lineNumberPaint.setTextSize(size);
    }

    /**
     * Modify the matches tokens highlighting color
     * @param color The new color value
     * @since 1.2.1
     */
    public void setMatchingHighlightColor(int color) {
        matchingColor = color;
    }
    
     /**
     * Modify the typeface of line number
     * @param typeface The typeface to be set
     * @since 1.3.4 
     */
    public void setLineNumberTypeface(Typeface typeface){
        lineNumberPaint.setTypeface(typeface);
    }

    /**
     * Modify the maximum number of suggestions to show, default is Integer.MAX_VALUE
     * @param maxSuggestions the maximum number of suggestions
     * @since 1.3.0
     */
    public void setMaxSuggestionsSize(int maxSuggestions) {
        maxNumberOfSuggestions = maxSuggestions;
    }

    /**
     * Modify the auto complete item height
     * @param height auto complete item height in dp
     * @since 1.3.0
     */
    public void setAutoCompleteItemHeightInDp(int height) {
        autoCompleteItemHeightInDp = (int) (height * Resources.getSystem().getDisplayMetrics().density);
    }

    /**
     * Enable or disable the auto pairs complete feature
     * @param enable Flag to enable or disable auto pair complete
     * @since 1.3.0
     */
    public void enablePairComplete(boolean enable) {
        enablePairComplete = enable;
    }

    /**
     * Enable or disable moving the cursor to the center after insert pair complete
     * @param enable Flag to enable or disable pair complete center cursor
     * @since 1.3.4
     */
    public void enablePairCompleteCenterCursor(boolean enable) {
        enablePairCompleteCenterCursor = enable;
    }

    /**
     * Set the pairs for auto pairs complete feature
     * @param map Map of pairs of characters
     * @since 1.3.0
     */
    public void setPairCompleteMap(Map<Character, Character> map) {
        mPairCompleteMap.clear();
        mPairCompleteMap.putAll(map);
    }

    /**
     * Add new pair complete item using key and value
     * @param key the pair complete item key
     * @param value the pair complete item value
     * @since 1.3.0
     */
    public void addPairCompleteItem(char key, char value) {
        mPairCompleteMap.put(key, value);
    }

    /**
     * Remove single pair complete item by key
     * @param key the pair complete item key
     * @since 1.3.0
     */
    public void removePairCompleteItem(char key) {
        mPairCompleteMap.remove(key);
    }

    /**
     * Clear all of pairs
     * @since 1.3.0
     */
    public void clearPairCompleteMap() {
        mPairCompleteMap.clear();
    }

    @Override
    public void showDropDown() {
        final Layout layout = getLayout();
        final int position = getSelectionStart();
        final int line = layout.getLineForOffset(position);
        final int lineButton = layout.getLineBottom(line);

        int numberOfMatchedItems = getAdapter().getCount();
        if (numberOfMatchedItems > maxNumberOfSuggestions) {
            numberOfMatchedItems = maxNumberOfSuggestions;
        }

        int dropDownHeight = getDropDownHeight();
        int modifiedDropDownHeight = numberOfMatchedItems * autoCompleteItemHeightInDp;
        if (dropDownHeight != modifiedDropDownHeight) {
            dropDownHeight = modifiedDropDownHeight;
        }

        final Rect displayFrame = new Rect();
        getGlobalVisibleRect(displayFrame);

        int displayFrameHeight = displayFrame.height();

        int verticalOffset = lineButton + dropDownHeight;
        if (verticalOffset > displayFrameHeight) {
            verticalOffset = displayFrameHeight - autoCompleteItemHeightInDp;
        }

        setDropDownHeight(dropDownHeight);
        setDropDownVerticalOffset(verticalOffset - displayFrameHeight - dropDownHeight);
        setDropDownHorizontalOffset((int) layout.getPrimaryHorizontal(position));

        super.showDropDown();
    }

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            Editable source = getText();
            highlightWithoutChange(source);
        }
    };

    private final OnKeyListener mOnKeyListener = new OnKeyListener() {

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (!enableAutoIndentation) return false;
            switch (keyCode) {
                case KeyEvent.KEYCODE_SPACE:
                    currentIndentation++;
                    break;
                case KeyEvent.KEYCODE_DEL:
                    if (currentIndentation > 0)
                        currentIndentation--;
                    break;
            }
            return false;
        }
    };

    private final TextWatcher mEditorTextWatcher = new TextWatcher() {

        private int start;
        private int count;

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {
            this.start = start;
            this.count = count;
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            if (!modified) return;

            if(highlightWhileTextChanging) {
                if (mSyntaxPatternMap.size() > 0) {
                    convertTabs(getEditableText(), start, count);
                    mUpdateHandler.postDelayed(mUpdateRunnable, mUpdateDelayTime);
                }
            }

            if (mRemoveErrorsWhenTextChanged) removeAllErrorLines();

            if (count == 1 && (enableAutoIndentation || enablePairComplete)) {
                char currentChar = charSequence.charAt(start);

                if (enableAutoIndentation) {
                    if (indentationStarts.contains(currentChar))
                        currentIndentation += tabLength;
                    else if (indentationEnds.contains(currentChar))
                        currentIndentation -= tabLength;
                }

                if (enablePairComplete) {
                    Character pairValue = mPairCompleteMap.get(currentChar);
                    if (pairValue != null) {
                        modified = false;
                        int selectionEnd = getSelectionEnd();
                        getText().insert(selectionEnd, pairValue.toString());
                        if (enablePairCompleteCenterCursor) setSelection(selectionEnd);
                        if (enableAutoIndentation) {
                            if (indentationStarts.contains(pairValue))
                                currentIndentation += tabLength;
                            else if (indentationEnds.contains(pairValue))
                                currentIndentation -= tabLength;
                        }
                        modified = true;
                    }
                }
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if(!highlightWhileTextChanging) {
                if (!modified) return;

                cancelHighlighterRender();

                if (mSyntaxPatternMap.size() > 0) {
                    convertTabs(getEditableText(), start, count);
                    mUpdateHandler.postDelayed(mUpdateRunnable, mUpdateDelayTime);
                }
            }
        }
    };

    private final class TabWidthSpan extends ReplacementSpan {

        @Override
        public int getSize(@NonNull Paint paint, CharSequence text,
                           int start, int end, Paint.FontMetricsInt fm) {
            return tabWidth;
        }

        @Override
        public void draw(@NonNull Canvas canvas, CharSequence text,
                         int start, int end, float x,
                         int top, int y, int bottom, @NonNull Paint paint) {
        }
    }

    private final InputFilter mInputFilter = new InputFilter() {

        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dStart, int dEnd) {
            if (modified && enableAutoIndentation && start < source.length()) {
                if (source.charAt(start) == '\n') {
                    // Apply the current indentation if it inserted at the end
                    if (dest.length() == dEnd) return applyIndentation(source, currentIndentation);

                    // reCalculate the current indentation
                    int indentation = calculateSourceIndentation(dest.subSequence(0, dStart));

                    // Decrement the indentation if the next char is on indentationEnds set
                    if (indentationEnds.contains(dest.charAt(dEnd))) indentation -= tabLength;

                    // Apply the new indentation to the source code
                    return applyIndentation(source, indentation);
                }
            }
            return source;
        }
    };

    private CharSequence applyIndentation(CharSequence source, int indentation) {
        StringBuilder sourceCode = new StringBuilder();
        sourceCode.append(source);
        for (int i = 0; i < indentation; i++) sourceCode.append(" ");
        return sourceCode.toString();
    }

    private int calculateSourceIndentation(CharSequence source) {
        int indentation = 0;
        String[] lines = source.toString().split("\n");
        for (String line : lines) {
            indentation += calculateExtraIndentation(line);
        }
        return indentation;
    }

    private int calculateExtraIndentation(String line) {
        if (line.isEmpty()) return 0;
        char firstChar = line.charAt(line.length() - 1);
        if (indentationStarts.contains(firstChar)) return tabLength;
        else if (indentationEnds.contains(firstChar)) return -tabLength;
        return 0;
    }
}
