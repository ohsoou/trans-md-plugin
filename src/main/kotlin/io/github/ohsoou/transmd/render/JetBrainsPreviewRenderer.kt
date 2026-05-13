package io.github.ohsoou.transmd.render

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.intellij.plugins.markdown.ui.preview.html.MarkdownUtil

internal object JetBrainsPreviewHtmlDocument {
    fun wrap(renderedHtml: String): String = "<html><head></head>$renderedHtml</html>"
}

class JetBrainsPreviewRenderer(
    private val file: VirtualFile,
    private val project: Project
) : MarkdownRenderer {
    override fun render(markdown: String): String =
        JetBrainsPreviewHtmlDocument.wrap(
            MarkdownUtil.generateMarkdownHtml(file, markdown, project)
        )
}
