package io.github.ohsoou.transmd.render

class FallbackMarkdownRenderer(
    private val primary: MarkdownRenderer,
    private val fallback: MarkdownRenderer
) : MarkdownRenderer {
    var isUsingFallback: Boolean = false
        private set
    var lastFailure: Throwable? = null
        private set

    override fun render(markdown: String): String {
        if (isUsingFallback) {
            return fallback.render(markdown)
        }

        return try {
            primary.render(markdown)
        } catch (t: Throwable) {
            if (t !is Exception && t !is LinkageError) throw t
            isUsingFallback = true
            lastFailure = t
            fallback.render(markdown)
        }
    }

    fun forceFallback() {
        isUsingFallback = true
    }
}
