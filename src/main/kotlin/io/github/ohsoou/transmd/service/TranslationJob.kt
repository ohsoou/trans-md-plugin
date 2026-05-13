package io.github.ohsoou.transmd.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TranslationJob(
    private val cache: TranslationCache,
    private val providerFactory: (String) -> TranslationService
) {
    fun run(
        rawMarkdown: String,
        targetLanguage: String,
        apiKey: String?
    ): Flow<TranslationJobState> = flow {
        if (apiKey.isNullOrBlank()) {
            emit(TranslationJobState.Failed(TranslationJobFailure.MissingApiKey))
            return@flow
        }

        val cacheKey = rawMarkdown.hashCode()
        val cached = cache.get(cacheKey, targetLanguage)
        if (cached != null) {
            emit(TranslationJobState.Succeeded(cached))
            return@flow
        }

        val preprocessed = MarkdownPreprocessor.preprocess(rawMarkdown)
        val chunks = MarkdownPreprocessor.splitIntoChunks(preprocessed.sanitized)
        val total = chunks.size
        val service = providerFactory(apiKey)
        val translated = StringBuilder()

        for ((index, chunk) in chunks.withIndex()) {
            emit(TranslationJobState.Translating(index + 1, total))
            when (val result = service.translate(chunk, targetLanguage)) {
                is TranslationProviderResult.Succeeded -> {
                    translated.append(result.translatedText)
                    if (index < chunks.lastIndex) translated.append("\n\n")
                }
                is TranslationProviderResult.Failed -> {
                    val failure = when (result.reason) {
                        TranslationProviderFailure.PermissionDenied -> TranslationJobFailure.PermissionDenied
                        TranslationProviderFailure.QuotaExceeded -> TranslationJobFailure.QuotaExceeded
                        is TranslationProviderFailure.NetworkError -> TranslationJobFailure.ProviderFailure(result.reason.message)
                        is TranslationProviderFailure.UnexpectedError -> TranslationJobFailure.UnexpectedFailure(result.reason.cause)
                    }
                    emit(TranslationJobState.Failed(failure))
                    return@flow
                }
            }
        }

        val restored = MarkdownPreprocessor.restore(translated.toString(), preprocessed.placeholders)
        cache.put(cacheKey, targetLanguage, restored)
        emit(TranslationJobState.Succeeded(restored))
    }
}
