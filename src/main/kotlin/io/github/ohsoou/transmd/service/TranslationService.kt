package io.github.ohsoou.transmd.service

fun interface TranslationService {
    suspend fun translate(text: String, targetLang: String): TranslationProviderResult
}