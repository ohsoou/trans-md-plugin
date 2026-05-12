package io.github.ohsoou.transmd.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.UIUtil
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.options.ShowSettingsUtil
import io.github.ohsoou.transmd.service.MarkdownPreprocessor
import io.github.ohsoou.transmd.service.TranslationCache
import io.github.ohsoou.transmd.service.impl.GoogleTranslateService
import io.github.ohsoou.transmd.service.impl.TranslationException
import io.github.ohsoou.transmd.settings.TransMdSettings
import io.github.ohsoou.transmd.settings.TransMdSettingsConfigurable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.awt.BorderLayout
import java.awt.Color
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTextPane

class TranslatedPreviewFileEditor(
    private val project: Project,
    private val file: VirtualFile
) : UserDataHolderBase(), FileEditor {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var translationJob: Job? = null

    private val useJcef = JBCefApp.isSupported()
    private val browser: JBCefBrowser? = if (useJcef) JBCefBrowser() else null
    private val textPane: JTextPane? = if (!useJcef) {
        JTextPane().apply { contentType = "text/html"; isEditable = false }
    } else null

    private val sourceEditor: com.intellij.openapi.editor.Editor? = run {
        val document = FileDocumentManager.getInstance().getDocument(file)
        if (document != null) EditorFactory.getInstance().createEditor(document, project) else null
    }

    val panel: JPanel = object : JPanel(BorderLayout()) {
        override fun addNotify() {
            super.addNotify()
            if (componentCount == 0) {
                val previewComponent = browser?.component ?: textPane!!
                if (sourceEditor != null) {
                    val split = JSplitPane(
                        JSplitPane.HORIZONTAL_SPLIT,
                        sourceEditor.component,
                        previewComponent
                    ).apply { resizeWeight = 0.5 }
                    add(split, BorderLayout.CENTER)
                } else {
                    add(previewComponent, BorderLayout.CENTER)
                }
            }
        }
    }

    private val mdParser: Parser
    private val mdRenderer: HtmlRenderer

    init {
        val extensions = listOf(TablesExtension.create(), StrikethroughExtension.create())
        mdParser = Parser.builder().extensions(extensions).build()
        mdRenderer = HtmlRenderer.builder().extensions(extensions).build()

        browser?.jbCefClient?.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun onBeforeBrowse(
                browser: CefBrowser, frame: CefFrame, request: CefRequest,
                userGesture: Boolean, isRedirect: Boolean
            ): Boolean {
                if (request.url == "trans-md://settings") {
                    ApplicationManager.getApplication().invokeLater {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, TransMdSettingsConfigurable::class.java)
                    }
                    return true
                }
                return request.url.startsWith("trans-md://")
            }
        }, browser.cefBrowser)
    }

    override fun selectNotify() {
        startTranslation()
    }

    fun startTranslation() {
        translationJob?.cancel()
        translationJob = scope.launch {
            runTranslationJob()
        }
    }

    private fun loadApiKey(): String? {
        val attrs = CredentialAttributes("io.github.ohsoou.transmd", "google-translate-api-key")
        return PasswordSafe.instance.getPassword(attrs)
    }

    private suspend fun runTranslationJob() {
        val apiKey = loadApiKey()
        if (apiKey.isNullOrBlank()) {
            loadHtml(errorHtml(
                "API 키가 설정되지 않았습니다.",
                "Settings → Markdown Translator에서 Google Translate API 키를 입력하세요.",
                showSettingsLink = true
            ))
            return
        }

        val rawText = runInterruptible(Dispatchers.IO) {
            String(file.contentsToByteArray(), Charsets.UTF_8)
        }

        val targetLang = TransMdSettings.getInstance().targetLang
        val cache = ApplicationManager.getApplication().getService(TranslationCache::class.java)
        val cacheKey = rawText.hashCode()

        val cached = cache?.get(cacheKey, targetLang)
        if (cached != null) {
            loadHtml(renderMarkdown(cached))
            return
        }

        val preprocessed = MarkdownPreprocessor.preprocess(rawText)
        val chunks = MarkdownPreprocessor.splitIntoChunks(preprocessed.sanitized)
        val total = chunks.size
        val translated = StringBuilder()
        val service = GoogleTranslateService(apiKey)

        for ((index, chunk) in chunks.withIndex()) {
            loadHtml(loadingHtml(index + 1, total))
            val result = service.translate(chunk, targetLang)
            result.onFailure { e ->
                val message = when {
                    e is TranslationException && e.httpCode == 403 ->
                        "API 키가 유효하지 않거나 할당량을 초과했습니다."
                    e is TranslationException && e.httpCode == 429 ->
                        "번역 할당량을 초과했습니다. 잠시 후 다시 시도하세요."
                    else -> e.message ?: "알 수 없는 오류가 발생했습니다."
                }
                loadHtml(errorHtml("번역에 실패했습니다.", message))
                return
            }
            translated.append(result.getOrThrow())
            if (index < chunks.lastIndex) translated.append("\n\n")
        }

        val restored = MarkdownPreprocessor.restore(translated.toString(), preprocessed.placeholders)
        val html = renderMarkdown(MarkdownPreprocessor.stripFrontMatter(restored))
        cache?.put(cacheKey, targetLang, html)
        loadHtml(html)
    }

    private suspend fun loadHtml(html: String) = withContext(Dispatchers.Main) {
        browser?.loadHTML(wrapHtml(html)) ?: run { textPane?.text = wrapHtml(html) }
    }

    private fun renderMarkdown(markdown: String): String {
        val document = mdParser.parse(markdown)
        return mdRenderer.render(document)
    }

    private fun Color.isDark() = (red * 0.299 + green * 0.587 + blue * 0.114) < 128
    private fun Color.toCss() = "rgb($red,$green,$blue)"

    private fun wrapHtml(body: String): String {
        val bg = UIUtil.getPanelBackground()
        val fg = UIUtil.getLabelForeground().toCss()
        val isDark = bg.isDark()
        val codeBg = if (isDark) Color(bg.red + 15, bg.green + 15, bg.blue + 15) else Color(bg.red - 15, bg.green - 15, bg.blue - 15)
        val borderColor = if (isDark) "rgb(70,70,70)" else "rgb(220,220,220)"
        val errorBg = if (isDark) "rgb(80,60,20)" else "rgb(255,243,205)"
        val errorBorder = if (isDark) "rgb(120,90,30)" else "rgb(255,193,7)"
        val linkColor = if (isDark) "rgb(88,166,255)" else "rgb(0,102,204)"

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                       padding: 16px 24px; max-width: 860px; margin: 0 auto;
                       line-height: 1.6; color: $fg; background: $bg; font-size: 14px; }
                h1 { font-size: 1.75em; } h2 { font-size: 1.4em; } h3 { font-size: 1.15em; }
                code { background: $codeBg; border-radius: 3px; padding: 2px 5px;
                       font-family: 'JetBrains Mono', monospace; font-size: 0.9em; color: $fg; }
                pre  { background: $codeBg; border-radius: 6px; padding: 12px 16px; overflow-x: auto; }
                pre code { background: none; padding: 0; }
                blockquote { border-left: 4px solid $borderColor; margin: 0;
                             padding-left: 16px; color: $fg; opacity: 0.75; }
                table { border-collapse: collapse; width: 100%; }
                th, td { border: 1px solid $borderColor; padding: 8px 12px; }
                th { background: $codeBg; }
                a { color: $linkColor; }
                hr { border: none; border-top: 1px solid $borderColor; }
                .trans-md-error { background: $errorBg; border: 1px solid $errorBorder;
                                  border-radius: 6px; padding: 12px 16px; margin: 16px 0; color: $fg; }
              </style>
            </head>
            <body>$body</body>
            </html>
        """.trimIndent()
    }

    private fun loadingHtml(current: Int, total: Int) =
        "<p>⏳ 번역 중... ($current/$total 섹션)</p>"

    private fun errorHtml(title: String, detail: String, showSettingsLink: Boolean = false): String {
        val link = if (showSettingsLink) """<br><a href="trans-md://settings">⚙ 설정 열기</a>""" else ""
        return """<div class="trans-md-error"><strong>⚠️ $title</strong><br><small>$detail$link</small></div>"""
    }

    override fun getComponent(): JComponent = panel
    override fun getPreferredFocusedComponent(): JComponent =
        sourceEditor?.contentComponent ?: browser?.component ?: textPane!!
    override fun getName(): String = "Translated Preview"
    override fun getFile(): VirtualFile = file
    override fun getState(level: com.intellij.openapi.fileEditor.FileEditorStateLevel) = FileEditorState.INSTANCE
    override fun setState(state: FileEditorState) {}
    override fun isModified() = false
    override fun isValid() = file.isValid
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun dispose() {
        translationJob?.cancel()
        sourceEditor?.let { EditorFactory.getInstance().releaseEditor(it) }
        browser?.let { Disposer.dispose(it) }
    }
}