package io.github.ohsoou.transmd.service

sealed interface TranslationProviderResult {
    data class Succeeded(val translatedText: String) : TranslationProviderResult
    data class Failed(val reason: TranslationProviderFailure) : TranslationProviderResult
}

sealed interface TranslationProviderFailure {
    data object PermissionDenied : TranslationProviderFailure
    data object QuotaExceeded : TranslationProviderFailure
    data class NetworkError(val message: String) : TranslationProviderFailure
    data class UnexpectedError(val cause: Throwable) : TranslationProviderFailure
}
