package io.github.ohsoou.transmd

import io.github.ohsoou.transmd.editor.SwingHtmlFallbackDocument
import javax.swing.JTextPane
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwingHtmlFallbackDocumentTest {

    @Test
    fun `wraps fallback html without stylesheet blocks`() {
        val wrapped = SwingHtmlFallbackDocument.wrap("<p>Hello</p>", "#ffffff", "#000000")

        assertEquals(
            "<html>\n<body bgcolor=\"#ffffff\" text=\"#000000\">\n<p>Hello</p>\n</body>\n</html>",
            wrapped
        )
    }

    @Test
    fun `swing html pane accepts wrapped fallback html`() {
        val pane = JTextPane().apply { contentType = "text/html" }
        val html = SwingHtmlFallbackDocument.wrap("<p>Hello</p>", "#ffffff", "#000000")

        pane.text = html

        assertTrue(pane.document.length > 0)
    }
}
