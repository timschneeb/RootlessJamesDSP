package me.timschneeberger.rootlessjamesdsp.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.amrdeveloper.codeview.CodeView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import me.timschneeberger.rootlessjamesdsp.BuildConfig
import me.timschneeberger.rootlessjamesdsp.R
import me.timschneeberger.rootlessjamesdsp.adapter.CustomCodeViewAdapter
import me.timschneeberger.rootlessjamesdsp.databinding.ActivityLiveprogEditorBinding
import me.timschneeberger.rootlessjamesdsp.databinding.DialogEditorSearchReplaceBinding
import me.timschneeberger.rootlessjamesdsp.editor.plugin.SourcePositionListener
import me.timschneeberger.rootlessjamesdsp.editor.plugin.UndoRedoManager
import me.timschneeberger.rootlessjamesdsp.editor.syntax.EelLanguage
import me.timschneeberger.rootlessjamesdsp.editor.widget.SymbolInputView
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspWrapper
import me.timschneeberger.rootlessjamesdsp.liveprog.EelParser
import me.timschneeberger.rootlessjamesdsp.model.ProcessorMessage
import me.timschneeberger.rootlessjamesdsp.service.BaseAudioProcessorService
import me.timschneeberger.rootlessjamesdsp.utils.Constants
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.registerLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.resolveColorAttribute
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showInputAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.showYesNoAlert
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.toast
import me.timschneeberger.rootlessjamesdsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import me.timschneeberger.rootlessjamesdsp.utils.isPlugin
import me.timschneeberger.rootlessjamesdsp.utils.isRoot
import me.timschneeberger.rootlessjamesdsp.utils.isRootless
import timber.log.Timber
import java.util.Locale
import java.util.regex.Pattern


class LiveprogEditorActivity : BaseActivity() {

    private lateinit var binding: ActivityLiveprogEditorBinding

    private lateinit var codeView: CodeView
    private lateinit var language: EelLanguage
    private lateinit var undoRedoManager: UndoRedoManager

    private val parser = EelParser()
    private var isDirty = false
        set(value) {
            field = value
            updateName()
        }

    private var processorMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handleProcessorMessage(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveprogEditorBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        registerLocalReceiver(processorMessageReceiver, IntentFilter(Constants.ACTION_PROCESSOR_MESSAGE))

        configCodeView()
        configCodeViewPlugins()

        val savedPath = savedInstanceState?.getString(EXTRA_TARGET_FILE)

        if(!intent.hasExtra(EXTRA_TARGET_FILE)) {
            finish()
        }

        // Only load if path not is cached
        savedPath ?: intent.getStringExtra(EXTRA_TARGET_FILE)?.let { load(it) }

        onBackPressedDispatcher.addCallback(this /* lifecycle owner */, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(isDirty) {
                    this@LiveprogEditorActivity.showYesNoAlert(R.string.editor_save_prompt_title, R.string.editor_save_prompt) {
                        if(it)
                            save()
                        sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_RELOAD_LIVEPROG))
                        finish()
                        return@showYesNoAlert
                    }
                }
                else {
                    sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_RELOAD_LIVEPROG))
                    finish()
                }
            }
        })

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.getString(EXTRA_TARGET_FILE)?.let { path ->
            Timber.d("Restoring parser state")
            parser.load(path, skipParse = true)
            parser.contents = codeView.text.toString()
            parser.parse()
            isDirty = savedInstanceState.getBoolean(STATE_IS_DIRTY)
            undoRedoManager.clearHistory() // TODO save history instance state correctly
        }

        super.onPostCreate(savedInstanceState)
    }

    override fun onDestroy() {
        unregisterLocalReceiver(processorMessageReceiver)
        super.onDestroy()
    }

    private fun handleProcessorMessage(intent: Intent) {
        when (ProcessorMessage.Type.fromInt(intent.getIntExtra(ProcessorMessage.TYPE, 0))) {
            ProcessorMessage.Type.LiveprogResult -> {
                codeView.removeAllErrorLines()

                val ret = intent.getIntExtra(ProcessorMessage.Param.LiveprogResultCode.name, 1)
                // Handle compiler error
                if(ret <= 0)
                {
                    var msg = JamesDspWrapper.eelErrorCodeToString(ret)
                    val msgDetail = intent.getStringExtra(ProcessorMessage.Param.LiveprogErrorMessage.name)

                    // Mark line in editor red
                    val result = """\b(\d+):""".toRegex().find(msgDetail ?: "")
                    val matchRange = result?.range
                    val message = msgDetail?.removeRange(matchRange?.start ?: 0, (matchRange?.endInclusive ?: -1) + 1)

                    var relativeLine = result?.groups?.get(1)?.value?.toIntOrNull() ?: -1

                    // Calculate actual line number
                    val initLine = parser.findAnnotationLine("@init")
                    val sampleLine = parser.findAnnotationLine("@sample")
                    if (ret == -1) // error in @init
                    {
                        if (relativeLine >= 0 && initLine >= 0)
                            relativeLine += initLine
                        else
                            relativeLine = -1
                    }
                    else if (ret == -3) // error in @sample
                    {
                        if (relativeLine >= 0 && sampleLine >= 0)
                            relativeLine += sampleLine
                        else
                            relativeLine = -1
                    }

                    if(msgDetail?.isNotBlank() == true) {
                        msg += if(relativeLine >= 0)
                            "; Line $relativeLine: $message"
                        else
                            "; $msgDetail"
                    }

                    if(relativeLine >= 0)
                        codeView.addErrorLine(relativeLine, ContextCompat.getColor(this, R.color.monokia_pro_error))
                    codeView.reHighlightErrors()

                    Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG)
                        .apply {
                            setAction(getString(R.string.close)) {}
                            setTextMaxLines(5)
                            setBackgroundTint(resolveColorAttribute(com.google.android.material.R.attr.colorErrorContainer))
                            setTextColor(resolveColorAttribute(com.google.android.material.R.attr.colorOnErrorContainer))
                            setActionTextColor(resolveColorAttribute(com.google.android.material.R.attr.colorError))
                            show()
                        }
                }
            }
            else -> {}
        }
    }

    private fun load(path: String) {
        parser.load(path)
        val content = parser.contents
        if(content == null) {
            toast(getString(R.string.editor_open_fail))
            finish()
            return
        }

        updateName()

        codeView.setText(content)
        codeView.removeAllErrorLines()
        codeView.reHighlightSyntax()
        undoRedoManager.clearHistory()
        isDirty = false
    }

    private fun save() {
        parser.contents = codeView.text.toString()
        parser.save()
        isDirty = false
    }

    @SuppressLint("SetTextI18n")
    private fun updateName() {
        binding.fileNameText.text = parser.fileName + if(isDirty) "*" else ""
    }

    @SuppressLint("ApplySharedPref", "CommitPrefEdits")
    private fun run() {
        save()

        val path = parser.path
        path ?: return

        @Suppress("DEPRECATION")
        val liveprog = getSharedPreferences(Constants.PREF_LIVEPROG, Context.MODE_MULTI_PROCESS)
        var key = getString(R.string.key_liveprog_enable)
        // Make sure liveprog is enabled
        if(!liveprog.getBoolean(key, false)) {
            toast(getString(R.string.editor_liveprog_enabled), false)
            liveprog.edit().putBoolean(key, true).commit()
        }

        // Make sure the current file is selected
        key = getString(R.string.key_liveprog_file)
        if(liveprog.getString(key, "") != path) {
            liveprog.edit().putString(key, path).commit()
        }

        // If service down or in case of root, if engine disabled, show warning message
        if(
            // Root/Rootless: if no services are active
            (!isPlugin() && BaseAudioProcessorService.activeServices <= 0) ||
            // Root/Plugin: if power button off
            (!isRootless() && !prefsApp.get<Boolean>(R.string.key_powered_on))
        ) {
            this.showAlert(R.string.editor_engine_down_title, R.string.editor_engine_down)
        }
        else {
            toast(getString(R.string.editor_script_launched), false)
        }

        sendLocalBroadcast(Intent(Constants.ACTION_SERVICE_RELOAD_LIVEPROG))
    }

    private fun configCodeView() {
        binding.codeViewScroller.isSmoothScrollingEnabled = true
        binding.codeViewHorizScroller.isSmoothScrollingEnabled = true

        codeView = binding.codeView

        // Completion threshold
        codeView.threshold = 2

        // Change default font to JetBrains Mono font
        val jetBrainsMono = ResourcesCompat.getFont(this, R.font.jetbrainsmono)
        codeView.typeface = jetBrainsMono
        codeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, prefsApp.get(R.string.key_editor_font_size))

        // Add input view
        binding.symbolInput.bindEditor(codeView)
        binding.symbolInput.addSymbols(
            arrayOf("TAB", "(", ")", "[", "]", ",", ".", ";", "?", ":", "+", "-", "*", "/", "@", "\""),
            arrayOf("\t",  "(", ")", "[", "]", ",", ".", ";", "?", ":", "+", "-", "*", "/", "@", "\"")
        )
        binding.symbolInput.forEachButton(object: SymbolInputView.ButtonConsumer {
            override fun accept(btn: Button) {
                btn.typeface = jetBrainsMono
            }
        })

        // Setup Line number feature
        codeView.setEnableLineNumber(true)
        codeView.setLineNumberTextColor(Color.GRAY)
        codeView.setLineNumberTextSize(25f)

        // Setup highlighting current line
        codeView.setEnableHighlightCurrentLine(true)
        codeView.setHighlightCurrentLineColor(ContextCompat.getColor(this, R.color.monokia_pro_gray))

        // Setup Auto indenting feature
        codeView.setTabLength(4)
        codeView.setEnableAutoIndentation(false)

        codeView.setTabWidth(4)

        codeView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                isDirty = true
            }
            override fun afterTextChanged(editable: Editable) {}
        })

        // Setup the language
        language = EelLanguage(this, codeView)

        // Setup auto pair complete
        val pairCompleteMap: MutableMap<Char, Char> = HashMap()
        pairCompleteMap['{'] = '}'
        pairCompleteMap['['] = ']'
        pairCompleteMap['('] = ')'
        pairCompleteMap['"'] = '"'
        pairCompleteMap['\''] = '\''
        codeView.setPairCompleteMap(pairCompleteMap)
        codeView.enablePairComplete(true)
        codeView.enablePairCompleteCenterCursor(false)

        // Setup the auto complete and auto indenting for the current language
        configLanguageAutoComplete()
        configLanguageAutoIndentation()
    }

    private fun configLanguageAutoComplete() {
        // Load the code list (keywords and snippets) for the current language
        val codeList = language.getCodeList()

        // Use CodeViewAdapter or custom one
        val adapter = CustomCodeViewAdapter(this, codeList)

        // Add the odeViewAdapter to the CodeView
        codeView.setAdapter(adapter)
    }

    private fun configLanguageAutoIndentation() {
        codeView.setIndentationStarts(language.indentationStarts)
        codeView.setIndentationEnds(language.indentationEnds)
    }

    private fun configCodeViewPlugins() {
        undoRedoManager = UndoRedoManager(codeView)
        undoRedoManager.connect()

        binding.sourcePositionTxt.text = getString(R.string.editor_source_position, 0, 0)
        val sourcePositionListener = SourcePositionListener(codeView)
        sourcePositionListener.setOnPositionChanged { line, column ->
            binding.sourcePositionTxt.text = getString(R.string.editor_source_position, line, column)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState.apply {
            putString(EXTRA_TARGET_FILE, parser.path)
            putBoolean(STATE_IS_DIRTY, isDirty)
        })
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_liveprog_editor, menu)
        if (menu is MenuBuilder)
            menu.setOptionalIconsVisible(true)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.findMenu -> launchEditorButtonSheet()
            R.id.text_undo -> {
                if(undoRedoManager.canUndo())
                    undoRedoManager.undo()
                else
                    toast(getString(R.string.editor_cannot_undo))
            }
            R.id.text_redo -> {
                if(undoRedoManager.canRedo())
                    undoRedoManager.redo()
                else
                    toast(getString(R.string.editor_cannot_redo))
            }
            R.id.text_run -> run()
            R.id.text_save -> save()
            R.id.fontSize -> changeFontSize()
            R.id.docs -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/james34602/EEL_VM#readme")))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun changeFontSize() {
        showInputAlert(
            LayoutInflater.from(this),
            R.string.editor_text_size,
            R.string.editor_text_size,
            "%.1f".format(Locale.ROOT, prefsApp.get<Float>(R.string.key_editor_font_size)),
            true,
            "dp"
        ) {
            it ?: return@showInputAlert
            try {
                val value = it.toFloat()
                if(value < 1)
                    throw NumberFormatException()

                codeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, value)
                prefsApp.set(R.string.key_editor_font_size, value)
            }
            catch (ex: Exception) {
                Timber.e("Failed to parse number input")
                Timber.d(ex)
                toast(getString(R.string.slider_dialog_format_error), false)
            }
        }
    }

    private fun launchEditorButtonSheet() {
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(R.layout.dialog_editor_search_replace)
        dialog.window!!.setDimAmount(0f)

        val editor = DialogEditorSearchReplaceBinding.inflate(layoutInflater, null, false)
        dialog.setContentView(editor.root)

        val searchEdit = editor.searchEdit
        val replacementEdit = editor.replacementEdit
        val findPrevAction = editor.findPrevAction
        val findNextAction = editor.findNextAction
        val replacementAction = editor.replaceAction
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                val text = editable.toString().trim { it <= ' ' }
                if (text.isEmpty()) codeView.clearMatches()
                codeView.findMatches(Pattern.quote(text))
            }
        })
        findPrevAction.setOnClickListener { codeView.findPrevMatch() }
        findNextAction.setOnClickListener { codeView.findNextMatch() }
        replacementAction.setOnClickListener {
            val regex = searchEdit.text.toString()
            val replacement = replacementEdit.text.toString()
            codeView.replaceAllMatches(regex, replacement)
        }
        dialog.setOnDismissListener { codeView.clearMatches() }
        dialog.show()
    }

    companion object {
        const val EXTRA_TARGET_FILE = "TargetFile"
        const val STATE_IS_DIRTY = "IsDirty"
    }
}