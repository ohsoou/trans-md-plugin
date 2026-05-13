package io.github.ohsoou.transmd.editor

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.ui.UIUtil
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import io.github.ohsoou.transmd.service.MarkdownPreprocessor
import io.github.ohsoou.transmd.service.TranslationJob
import io.github.ohsoou.transmd.service.TranslationJobFailure
import io.github.ohsoou.transmd.service.TranslationJobState
import io.github.ohsoou.transmd.render.CommonmarkRenderer
import io.github.ohsoou.transmd.render.FallbackMarkdownRenderer
import io.github.ohsoou.transmd.render.JetBrainsPreviewRenderer
import io.github.ohsoou.transmd.settings.TransMdSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel
import org.intellij.plugins.markdown.ui.preview.jcef.MarkdownJCEFHtmlPanel
import java.awt.CardLayout
import java.awt.BorderLayout
import java.awt.Color
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTextPane

internal object SwingHtmlFallbackDocument {
    fun wrap(body: String, backgroundHex: String, foregroundHex: String): String = """
        <html>
        <body bgcolor="$backgroundHex" text="$foregroundHex">
        $body
        </body>
        </html>
    """.trimIndent()
}

class TranslatedPreviewFileEditor(
    private val project: Project,
    private val file: VirtualFile,
    private val translationJob: TranslationJob
) : UserDataHolderBase(), FileEditor {
    companion object {
        private val LOG = Logger.getInstance(TranslatedPreviewFileEditor::class.java)
        private const val JETBRAINS_PREVIEW_CARD = "jetbrains-preview"
        private const val HTML_FALLBACK_CARD = "html-fallback"
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var activeJob: Job? = null

    private val textPane = JTextPane().apply {
        contentType = "text/html"
        isEditable = false
    }
    private val previewStack = JPanel(CardLayout())
    private var markdownPanel: MarkdownHtmlPanel? = null
    private val renderer = FallbackMarkdownRenderer(
        primary = JetBrainsPreviewRenderer(file, project),
        fallback = CommonmarkRenderer()
    )

    private var sourceEditor: com.intellij.openapi.editor.Editor? = null

    init {
        previewStack.add(textPane, HTML_FALLBACK_CARD)
        showPreviewCard(HTML_FALLBACK_CARD)
    }

    val panel: JPanel = object : JPanel(BorderLayout()) {
        override fun addNotify() {
            super.addNotify()
            if (componentCount == 0) {
                val doc = FileDocumentManager.getInstance().getDocument(file)
                sourceEditor = if (doc != null) EditorFactory.getInstance().createEditor(doc, project) else null

                val primaryPanel = createPrimaryPreviewPanel()
                markdownPanel = primaryPanel
                if (primaryPanel != null) {
                    previewStack.add(primaryPanel.component, JETBRAINS_PREVIEW_CARD)
                    showPreviewCard(JETBRAINS_PREVIEW_CARD)
                } else {
                    renderer.forceFallback()
                }

                val previewComponent = previewStack
                if (sourceEditor != null) {
                    val split = JSplitPane(
                        JSplitPane.HORIZONTAL_SPLIT,
                        sourceEditor!!.component,
                        previewComponent
                    ).apply { resizeWeight = 0.5 }
                    add(split, BorderLayout.CENTER)
                } else {
                    add(previewComponent, BorderLayout.CENTER)
                }
            }
        }
    }

    override fun selectNotify() {
        startTranslation()
    }

    fun startTranslation() {
        activeJob?.cancel()
        activeJob = scope.launch {
            runTranslationJob()
        }
    }

    private suspend fun loadApiKey(): String? = runInterruptible(Dispatchers.IO) {
        val attrs = CredentialAttributes("io.github.ohsoou.transmd", "google-translate-api-key")
        PasswordSafe.instance.getPassword(attrs)
    }

    private suspend fun runTranslationJob() {
        val apiKey = loadApiKey()
        val rawText = runInterruptible(Dispatchers.IO) {
            String(file.contentsToByteArray(), Charsets.UTF_8)
        }
        val targetLang = TransMdSettings.getInstance().targetLang

        translationJob.run(rawText, targetLang, apiKey).collect { state ->
            when (state) {
                is TranslationJobState.Translating -> showStatus(loadingHtml(state.currentChunk, state.totalChunks))
                is TranslationJobState.Succeeded -> displayMarkdown(state.translatedMarkdown)
                is TranslationJobState.Failed -> showStatus(errorHtml(state.reason))
            }
        }
    }

    private suspend fun showStatus(html: String) = withContext(Dispatchers.Main) {
        try {
            textPane.contentType = "text/html"
            textPane.text = wrapFallbackHtml(html)
            textPane.caretPosition = 0
        } catch (t: Throwable) {
            LOG.warn("Status HTML preview failed, using plain text fallback.", t)
            textPane.contentType = "text/plain"
            textPane.text = html
        }
        showPreviewCard(HTML_FALLBACK_CARD)
    }

    private suspend fun loadHtml(html: String): Boolean = withContext(Dispatchers.Main) {
        val panel = markdownPanel
        if (!renderer.isUsingFallback && panel != null) {
            try {
                panel.setHtml(html, 0, file)
                showPreviewCard(JETBRAINS_PREVIEW_CARD)
                return@withContext true
            } catch (t: Throwable) {
                if (t !is Exception && t !is LinkageError) throw t
                forceFallback("JetBrains Markdown preview panel failed, switching to CommonMark fallback.", t)
            }
        }

        try {
            textPane.contentType = "text/html"
            textPane.text = wrapFallbackHtml(html)
            textPane.caretPosition = 0
        } catch (t: Throwable) {
            LOG.warn("Fallback HTML preview failed, using plain text fallback.", t)
            textPane.contentType = "text/plain"
            textPane.text = html
        }
        showPreviewCard(HTML_FALLBACK_CARD)
        false
    }

    private suspend fun displayMarkdown(markdown: String) {
        val stripped = MarkdownPreprocessor.stripFrontMatter(markdown)
        val html = renderMarkdown(stripped)
        val renderedWithFallback = renderer.isUsingFallback
        val shownInPrimary = loadHtml(html)
        if (!shownInPrimary && !renderedWithFallback && renderer.isUsingFallback) {
            loadHtml(renderer.render(stripped))
        }
    }

    private suspend fun renderMarkdown(markdown: String): String {
        val wasUsingFallback = renderer.isUsingFallback
        val rendered = try {
            renderer.render(markdown)
        } catch (t: Throwable) {
            if (t !is Exception && t !is LinkageError) throw t
            forceFallback("JetBrains Markdown renderer failed, switching to CommonMark fallback.", t)
            renderer.render(markdown)
        }
        if (!wasUsingFallback && renderer.isUsingFallback) {
            forceFallback(
                "JetBrains Markdown renderer failed, switching to CommonMark fallback.",
                renderer.lastFailure
            )
        }
        return rendered
    }

    private fun createPrimaryPreviewPanel(): MarkdownHtmlPanel? {
        if (!JBCefApp.isSupported()) {
            return null
        }
        return try {
            MarkdownJCEFHtmlPanel(project, file)
        } catch (t: Throwable) {
            if (t !is Exception && t !is LinkageError) throw t
            forceFallback("JetBrains Markdown preview is unavailable, using CommonMark fallback.", t)
            null
        }
    }

    private fun showPreviewCard(card: String) {
        (previewStack.layout as CardLayout).show(previewStack, card)
    }

    private fun forceFallback(message: String, t: Throwable? = null) {
        if (!renderer.isUsingFallback) {
            renderer.forceFallback()
        }
        if (t != null) {
            LOG.warn(message, t)
        } else {
            LOG.warn(message)
        }
    }

    private fun wrapFallbackHtml(body: String): String {
        val bgColor = toHex(UIUtil.getPanelBackground())
        val fgColor = toHex(UIUtil.getLabelForeground())
        return SwingHtmlFallbackDocument.wrap(body, bgColor, fgColor)
    }

    private fun toHex(color: Color): String = "#%02x%02x%02x".format(color.red, color.green, color.blue)

    private fun loadingHtml(current: Int, total: Int) =
        "<p>⏳ 번역 중... ($current/$total 섹션)</p>"

    private fun errorHtml(failure: TranslationJobFailure): String = when (failure) {
        TranslationJobFailure.MissingApiKey -> errorHtml(
            "API 키가 설정되지 않았습니다.",
            "Settings → Trans Md에서 Google Translate API 키를 입력하세요.",
            showSettingsLink = true
        )
        TranslationJobFailure.PermissionDenied -> errorHtml(
            "번역에 실패했습니다.",
            "API 키가 유효하지 않거나 할당량을 초과했습니다."
        )
        TranslationJobFailure.QuotaExceeded -> errorHtml(
            "번역에 실패했습니다.",
            "번역 할당량을 초과했습니다. 잠시 후 다시 시도하세요."
        )
        is TranslationJobFailure.ProviderFailure -> errorHtml("번역에 실패했습니다.", failure.message)
        is TranslationJobFailure.UnexpectedFailure -> errorHtml(
            "번역에 실패했습니다.",
            failure.cause.message ?: "알 수 없는 오류가 발생했습니다."
        )
    }

    private fun errorHtml(title: String, detail: String, showSettingsLink: Boolean = false): String {
        val link = if (showSettingsLink) "<br><small>Settings → Trans Md에서 설정을 열 수 있습니다.</small>" else ""
        return """<div class="trans-md-error"><strong>⚠️ $title</strong><br><small>$detail$link</small></div>"""
    }

    override fun getComponent(): JComponent = panel
    override fun getPreferredFocusedComponent(): JComponent =
        sourceEditor?.contentComponent ?: markdownPanel?.component ?: textPane
    override fun getName(): String = "Translated Preview"
    override fun getFile(): VirtualFile = file
    override fun getState(level: com.intellij.openapi.fileEditor.FileEditorStateLevel) = FileEditorState.INSTANCE
    override fun setState(state: FileEditorState) {}
    override fun isModified() = false
    override fun isValid() = file.isValid
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun dispose() {
        activeJob?.cancel()
        sourceEditor?.let { EditorFactory.getInstance().releaseEditor(it) }
        markdownPanel?.let { Disposer.dispose(it) }
    }
}
