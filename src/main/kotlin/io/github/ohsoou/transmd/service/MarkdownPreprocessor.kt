package io.github.ohsoou.transmd.service

/**
 * Extracts Exclusions (code blocks, inline code, URLs, front matter) into Placeholders
 * before translation, and restores them afterward.
 *
 * Contract: restore(preprocess(text).sanitized, preprocess(text).placeholders) == text
 */
object MarkdownPreprocessor {

    private val FRONT_MATTER_REGEX = Regex("^---\\s*\\n.*?\\n---\\s*\\n", RegexOption.DOT_MATCHES_ALL)
    private val FENCED_CODE_REGEX = Regex("```[\\s\\S]*?```|~~~[\\s\\S]*?~~~")
    private val INLINE_CODE_REGEX = Regex("`[^`\\n]+`")
    // Matches the URL part in [text](URL) and bare <URL> autolinks
    private val MARKDOWN_LINK_URL_REGEX = Regex("\\[([^\\]]*)]\\(([^)]+)\\)")
    private val AUTOLINK_REGEX = Regex("<(https?://[^>]+)>")

    data class PreprocessedDocument(
        val sanitized: String,
        val placeholders: Map<String, String>
    )

    fun preprocess(text: String): PreprocessedDocument {
        val placeholders = mutableMapOf<String, String>()
        var counter = 0

        fun placeholder(original: String): String {
            val key = "__PLACEHOLDER_${counter++}__"
            placeholders[key] = original
            return key
        }

        var result = text

        // Front matter first (must be at very start of file)
        result = FRONT_MATTER_REGEX.replace(result) { placeholder(it.value) }

        // Fenced code blocks before inline code to avoid double-matching
        result = FENCED_CODE_REGEX.replace(result) { placeholder(it.value) }

        // Inline code spans
        result = INLINE_CODE_REGEX.replace(result) { placeholder(it.value) }

        // Markdown links: preserve URL, keep link text for translation
        result = MARKDOWN_LINK_URL_REGEX.replace(result) { match ->
            val linkText = match.groupValues[1]
            val url = match.groupValues[2]
            val urlPlaceholder = placeholder(url)
            "[$linkText]($urlPlaceholder)"
        }

        // Autolinks
        result = AUTOLINK_REGEX.replace(result) { match ->
            val url = match.groupValues[1]
            "<${placeholder(url)}>"
        }

        return PreprocessedDocument(result, placeholders)
    }

    fun stripFrontMatter(text: String): String =
        FRONT_MATTER_REGEX.replace(text, "")

    fun restore(translated: String, placeholders: Map<String, String>): String {
        var result = translated
        for ((key, original) in placeholders) {
            result = result.replace(key, original)
        }
        return result
    }

    /**
     * Splits sanitized Markdown into Chunks on heading boundaries.
     * Each Chunk is small enough for a single translation API request.
     */
    fun splitIntoChunks(text: String, maxChars: Int = 4500): List<String> {
        val headingRegex = Regex("^#{1,6} ", RegexOption.MULTILINE)
        val boundaries = mutableListOf(0)
        headingRegex.findAll(text).forEach { boundaries.add(it.range.first) }
        boundaries.add(text.length)

        val sections = boundaries.zipWithNext { start, end -> text.substring(start, end) }

        // Further split any section that exceeds maxChars on line boundaries
        return sections.flatMap { section ->
            if (section.length <= maxChars) listOf(section)
            else splitOnLines(section, maxChars)
        }.filter { it.isNotBlank() }
    }

    private fun splitOnLines(text: String, maxChars: Int): List<String> {
        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        for (line in text.lines()) {
            if (current.length + line.length + 1 > maxChars && current.isNotEmpty()) {
                chunks.add(current.toString())
                current.clear()
            }
            if (current.isNotEmpty()) current.append('\n')
            current.append(line)
        }
        if (current.isNotEmpty()) chunks.add(current.toString())
        return chunks
    }
}