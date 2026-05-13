package io.github.ohsoou.transmd

import io.github.ohsoou.transmd.render.FallbackMarkdownRenderer
import io.github.ohsoou.transmd.render.MarkdownRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FallbackMarkdownRendererTest {
    @Test
    fun `uses primary renderer while available`() {
        val renderer = FallbackMarkdownRenderer(
            primary = MarkdownRenderer { markdown -> "<primary>$markdown</primary>" },
            fallback = MarkdownRenderer { markdown -> "<fallback>$markdown</fallback>" }
        )

        val html = renderer.render("hello")

        assertEquals("<primary>hello</primary>", html)
        assertTrue(!renderer.isUsingFallback)
    }

    @Test
    fun `falls back permanently after recoverable primary failure`() {
        var primaryCalls = 0
        val renderer = FallbackMarkdownRenderer(
            primary = MarkdownRenderer { markdown ->
                primaryCalls += 1
                error("boom: $markdown")
            },
            fallback = MarkdownRenderer { markdown -> "<fallback>$markdown</fallback>" }
        )

        val first = renderer.render("hello")
        val second = renderer.render("world")

        assertEquals("<fallback>hello</fallback>", first)
        assertEquals("<fallback>world</fallback>", second)
        assertEquals(1, primaryCalls)
        assertTrue(renderer.isUsingFallback)
    }
}
