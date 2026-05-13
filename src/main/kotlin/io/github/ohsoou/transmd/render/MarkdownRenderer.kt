package io.github.ohsoou.transmd.render

fun interface MarkdownRenderer {
    fun render(markdown: String): String
}
