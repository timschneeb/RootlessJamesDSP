package me.timschneeberger.rootlessjamesdsp.editor.syntax

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.amrdeveloper.codeview.Code
import com.amrdeveloper.codeview.CodeView
import com.amrdeveloper.codeview.Keyword
import me.timschneeberger.rootlessjamesdsp.R
import java.util.ArrayList
import java.util.HashSet
import java.util.regex.Pattern

class EelLanguage(private val context: Context, private val codeView: CodeView) {

    //Language Keywords
    private val PATTERN_KEYWORDS = Pattern.compile("\\b(${getKeywords().joinToString("|")})\\b")
    private val PATTERN_BUILTINS = Pattern.compile("[,;\\[\\]{}()]")
    private val PATTERN_SINGLE_LINE_COMMENT = Pattern.compile("//[^\\n]*")
    private val PATTERN_MULTI_LINE_COMMENT = Pattern.compile("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/")
    private val PATTERN_FUNCTION = Pattern.compile("\\b\\w+(?=\\([^\\n]*\\))")
    private val PATTERN_FUNCTION_SIGNATURE = Pattern.compile("(?<=function)\\s+[^\\s\\(]+")
    private val PATTERN_CONSTANTS = Pattern.compile("(${getConstants().joinToString("|").replace("$", "\\$")})", Pattern.CASE_INSENSITIVE)
    private val PATTERN_PREFDEF_VARS = Pattern.compile("\\b(${getPredefinedVariables().joinToString("|")})\\b")
    private val PATTERN_OPERATION = Pattern.compile("\\*|=|==|>|<|!=|>=|<=|->|=|>|<|%|-|-=|%=|\\+|\\-|\\-=|\\+=|\\^|\\&|\\|\\*|\\||/|/=")
    private val PATTERN_CONDITION = Pattern.compile("\\?|:")
    private val PATTERN_ANNOTATION = Pattern.compile("(?<=\\n)[^\\S@\\n]*@.[a-zA-Z0-9]+")
    private val PATTERN_TODO_COMMENT = Pattern.compile("//\\s*TODO[^\n]*", Pattern.CASE_INSENSITIVE)
    private val PATTERN_NUMBERS = Pattern.compile("\\b(-?\\d+[.]?\\d*f?)\\b")
    private val PATTERN_CHAR = Pattern.compile("['](.*?)[']")
    private val PATTERN_STRING = Pattern.compile("[\"](.*?)[\"]")
    private val PATTERN_HEX = Pattern.compile("0x[0-9a-fA-F]+")

    private val PATTERN_PROPERTY = Pattern.compile("(?<=^|\\n)\\s*((?:[A-Za-z0-9])+:)")
    private val PATTERN_PROPERTY_RIGHT = Pattern.compile("(?<=^|\\n)\\s*((?:[A-Za-z0-9])+:)[^\\n]*")

    init {
        applyTheme()
    }

    private fun applyTheme() {
        fun col(@ColorRes resId: Int) = ContextCompat.getColor(context, resId)

        codeView.resetSyntaxPatternList()
        codeView.resetHighlighter()

        //View Background
        codeView.setBackgroundColor(col(R.color.monokia_pro_black))

        //Syntax Colors
        codeView.addSyntaxPattern(PATTERN_HEX, col(R.color.monokia_pro_purple))
        codeView.addSyntaxPattern(PATTERN_NUMBERS, col(R.color.monokia_pro_purple))
        codeView.addSyntaxPattern(PATTERN_KEYWORDS, col(R.color.monokia_pro_pink))
        codeView.addSyntaxPattern(PATTERN_BUILTINS, col(R.color.monokia_pro_white_dim))
        codeView.addSyntaxPattern(PATTERN_ANNOTATION, col(R.color.monokia_pro_pink))
        codeView.addSyntaxPattern(PATTERN_FUNCTION, col(R.color.monokia_pro_green))
        codeView.addSyntaxPattern(PATTERN_CONSTANTS, col(R.color.monokia_pro_sky))
        codeView.addSyntaxPattern(PATTERN_PREFDEF_VARS, col(R.color.monokia_pro_sky))
        codeView.addSyntaxPattern(PATTERN_OPERATION, col(R.color.monokia_pro_pink))
        codeView.addSyntaxPattern(PATTERN_CONDITION, col(R.color.monokia_pro_orange))
        codeView.addSyntaxPattern(PATTERN_FUNCTION_SIGNATURE, col(R.color.monokia_pro_green))

        codeView.addSyntaxPattern(PATTERN_CHAR, col(R.color.monokia_pro_green))
        codeView.addSyntaxPattern(PATTERN_STRING, col(R.color.monokia_pro_orange))

        codeView.addSyntaxPattern(PATTERN_PROPERTY_RIGHT, col(R.color.monokia_pro_sky_dim))
        codeView.addSyntaxPattern(PATTERN_PROPERTY, col(R.color.monokia_pro_sky))

        codeView.addSyntaxPattern(PATTERN_SINGLE_LINE_COMMENT, col(R.color.monokia_pro_grey))
        codeView.addSyntaxPattern(PATTERN_MULTI_LINE_COMMENT, col(R.color.monokia_pro_grey))

        //Default Color
        codeView.setTextColor(col(R.color.monokia_pro_white))
        codeView.addSyntaxPattern(PATTERN_TODO_COMMENT, col(R.color.gold))
        codeView.reHighlightSyntax()
    }

    private fun getKeywords(): Array<String> {
        return context.resources.getStringArray(R.array.editor_eel_keywords)
    }

    private fun getFunctions(): Array<String> {
        return context.resources.getStringArray(R.array.editor_eel_functions)
    }

    private fun getConstants(): Array<String> {
        return context.resources.getStringArray(R.array.editor_eel_constants)
    }

    private fun getPredefinedVariables(): Array<String> {
        return context.resources.getStringArray(R.array.editor_predef_vars)
    }

    fun getCodeList(): List<Code> {
        val codeList: MutableList<Code> = ArrayList()
        getKeywords().forEach { codeList.add(Keyword(it)) }
        getFunctions().forEach { codeList.add(Function(it)) }
        getConstants().forEach { codeList.add(Constant(it)) }
        getPredefinedVariables().forEach { codeList.add(Constant(it)) }
        return codeList
    }

    val indentationStarts: Set<Char>
        get() {
            val characterSet: MutableSet<Char> = HashSet()
            characterSet.add('(')
            return characterSet
        }
    val indentationEnds: Set<Char>
        get() {
            val characterSet: MutableSet<Char> = HashSet()
            characterSet.add(')')
            return characterSet
        }
}