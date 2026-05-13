package io.github.ohsoou.transmd.service

sealed interface TranslationJobFailure {
    data object MissingApiKey : TranslationJobFailure
    data object PermissionDenied : TranslationJobFailure
    data object QuotaExceeded : TranslationJobFailure
    data class ProviderFailure(val message: String) : TranslationJobFailure
    data class UnexpectedFailure(val cause: Throwable) : TranslationJobFailure
}
