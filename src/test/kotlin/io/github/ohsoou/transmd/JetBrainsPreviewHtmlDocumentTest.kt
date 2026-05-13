package io.github.ohsoou.transmd

import io.github.ohsoou.transmd.render.JetBrainsPreviewHtmlDocument
import kotlin.test.Test
import kotlin.test.assertEquals

class JetBrainsPreviewHtmlDocumentTest {

    @Test
    fun `wraps rendered preview html in the document shell expected by JCEF`() {
        val wrapped = JetBrainsPreviewHtmlDocument.wrap("<body><h1>Hello</h1></body>")

        assertEquals(
            "<html><head></head><body><h1>Hello</h1></body></html>",
            wrapped
        )
    }
}
