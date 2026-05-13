package io.github.ohsoou.transmd.service

sealed interface TranslationJobState {
    data class Translating(val currentChunk: Int, val totalChunks: Int) : TranslationJobState
    data class Succeeded(val translatedMarkdown: String) : TranslationJobState
    data class Failed(val reason: TranslationJobFailure) : TranslationJobState
}
