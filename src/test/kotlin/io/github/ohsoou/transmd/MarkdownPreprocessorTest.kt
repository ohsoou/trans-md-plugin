package io.github.ohsoou.transmd

import io.github.ohsoou.transmd.service.MarkdownPreprocessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarkdownPreprocessorTest {

    @Test
    fun `restore is inverse of preprocess`() {
        val text = """
            ---
            title: Hello
            ---

            # Title

            Some text with `inline code` and a [link](https://example.com).

            ```kotlin
            fun main() = println("hello")
            ```
        """.trimIndent()

        val doc = MarkdownPreprocessor.preprocess(text)
        val restored = MarkdownPreprocessor.restore(doc.sanitized, doc.placeholders)
        assertEquals(text, restored)
    }

    @Test
    fun `fenced code block is replaced with placeholder`() {
        val text = "Before\n```kotlin\nval x = 1\n```\nAfter"
        val doc = MarkdownPreprocessor.preprocess(text)

        assertFalse(doc.sanitized.contains("val x = 1"))
        assertTrue(doc.sanitized.contains("__PLACEHOLDER_"))
        assertTrue(doc.placeholders.values.any { it.contains("val x = 1") })
    }

    @Test
    fun `inline code is replaced with placeholder`() {
        val text = "Use `println()` to print."
        val doc = MarkdownPreprocessor.preprocess(text)

        assertFalse(doc.sanitized.contains("println()"))
        assertTrue(doc.placeholders.values.any { it == "`println()`" })
    }

    @Test
    fun `block math is replaced with placeholder`() {
        val text = """
            Before

            $$
            E = mc^2
            $$

            After
        """.trimIndent()
        val doc = MarkdownPreprocessor.preprocess(text)

        assertFalse(doc.sanitized.contains("E = mc^2"))
        assertTrue(doc.sanitized.contains("__PLACEHOLDER_"))
        assertTrue(doc.placeholders.values.any { it.contains("E = mc^2") })
    }

    @Test
    fun `url in markdown link is replaced but link text is preserved`() {
        val text = "See [the docs](https://example.com/docs) for more."
        val doc = MarkdownPreprocessor.preprocess(text)

        assertFalse(doc.sanitized.contains("https://example.com/docs"))
        assertTrue(doc.sanitized.contains("[the docs]"))
        assertTrue(doc.placeholders.values.any { it == "https://example.com/docs" })
    }

    @Test
    fun `front matter is replaced with placeholder`() {
        val text = "---\ntitle: Hello\ndate: 2024-01-01\n---\n\n# Content"
        val doc = MarkdownPreprocessor.preprocess(text)

        assertFalse(doc.sanitized.startsWith("---"))
        assertTrue(doc.sanitized.contains("# Content"))
        assertTrue(doc.placeholders.values.any { it.contains("title: Hello") })
    }

    @Test
    fun `plain text passes through unchanged`() {
        val text = "This is plain text with no code or links."
        val doc = MarkdownPreprocessor.preprocess(text)

        assertEquals(text, doc.sanitized)
        assertTrue(doc.placeholders.isEmpty())
    }

    @Test
    fun `stripFrontMatter removes front matter block`() {
        val text = "---\ntitle: Hello\ndate: 2024-01-01\n---\n\n# Content\n\nBody text."
        val stripped = MarkdownPreprocessor.stripFrontMatter(text)

        assertFalse(stripped.contains("title: Hello"))
        assertTrue(stripped.contains("# Content"))
        assertTrue(stripped.contains("Body text."))
    }

    @Test
    fun `stripFrontMatter leaves text without front matter unchanged`() {
        val text = "# Just a heading\n\nNo front matter here."
        assertEquals(text, MarkdownPreprocessor.stripFrontMatter(text))
    }

    @Test
    fun `splitIntoChunks splits on heading boundaries`() {
        val text = """
            # Introduction

            Some intro text.

            ## Section One

            Content of section one.

            ## Section Two

            Content of section two.
        """.trimIndent()

        val chunks = MarkdownPreprocessor.splitIntoChunks(text)
        assertEquals(3, chunks.size)
        assertTrue(chunks[0].startsWith("# Introduction"))
        assertTrue(chunks[1].startsWith("## Section One"))
        assertTrue(chunks[2].startsWith("## Section Two"))
    }

    @Test
    fun `splitIntoChunks further splits oversized sections on lines`() {
        val longLine = "word ".repeat(100)
        val text = "## Section\n" + (1..20).joinToString("\n") { "$it. $longLine" }

        val chunks = MarkdownPreprocessor.splitIntoChunks(text, maxChars = 1000)
        assertTrue(chunks.size > 1)
        chunks.forEach { assertTrue(it.length <= 1000 + 200) } // some tolerance for line boundaries
    }
}
