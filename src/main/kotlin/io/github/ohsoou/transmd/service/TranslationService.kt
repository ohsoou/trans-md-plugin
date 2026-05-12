package io.github.ohsoou.transmd.service

interface TranslationService {
    suspend fun translate(text: String, targetLang: String): Result<String>
}