package io.github.ohsoou.transmd

import io.github.ohsoou.transmd.service.TranslationCache
import io.github.ohsoou.transmd.service.TranslationJob
import io.github.ohsoou.transmd.service.TranslationJobFailure
import io.github.ohsoou.transmd.service.TranslationJobState
import io.github.ohsoou.transmd.service.TranslationProviderFailure
import io.github.ohsoou.transmd.service.TranslationProviderResult
import io.github.ohsoou.transmd.service.TranslationService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TranslationJobTest {

    private fun makeJob(service: TranslationService): TranslationJob =
        TranslationJob(TranslationCache(), providerFactory = { service })

    @Test
    fun `null API key emits Failed with MissingApiKey`() = runBlocking {
        val job = TranslationJob(TranslationCache(), providerFactory = { error("should not be called") })
        val states = job.run("# Hello", "ko", null).toList()
        assertEquals(listOf(TranslationJobState.Failed(TranslationJobFailure.MissingApiKey)), states)
    }

    @Test
    fun `blank API key emits Failed with MissingApiKey`() = runBlocking {
        val job = TranslationJob(TranslationCache(), providerFactory = { error("should not be called") })
        val states = job.run("# Hello", "ko", "   ").toList()
        assertEquals(listOf(TranslationJobState.Failed(TranslationJobFailure.MissingApiKey)), states)
    }

    @Test
    fun `cache hit emits Succeeded without calling provider`() = runBlocking {
        val markdown = "# Hello"
        val cache = TranslationCache().also { it.put(markdown.hashCode(), "ko", "# 안녕") }
        val job = TranslationJob(cache, providerFactory = { error("provider should not be called on cache hit") })
        val states = job.run(markdown, "ko", "apikey").toList()
        assertEquals(listOf(TranslationJobState.Succeeded("# 안녕")), states)
    }

    @Test
    fun `successful translation emits Translating states then Succeeded`() = runBlocking {
        val service = TranslationService { text, _ -> TranslationProviderResult.Succeeded("번역: $text") }
        val states = makeJob(service).run("# Hello", "ko", "apikey").toList()

        assertTrue(states.isNotEmpty())
        assertIs<TranslationJobState.Succeeded>(states.last())
        assertTrue(states.dropLast(1).all { it is TranslationJobState.Translating })
    }

    @Test
    fun `PermissionDenied from provider maps to job PermissionDenied`() = runBlocking {
        val service = TranslationService { _, _ ->
            TranslationProviderResult.Failed(TranslationProviderFailure.PermissionDenied)
        }
        val states = makeJob(service).run("# Hello", "ko", "apikey").toList()

        val last = states.last()
        assertIs<TranslationJobState.Failed>(last)
        assertEquals(TranslationJobFailure.PermissionDenied, last.reason)
    }

    @Test
    fun `QuotaExceeded from provider maps to job QuotaExceeded`() = runBlocking {
        val service = TranslationService { _, _ ->
            TranslationProviderResult.Failed(TranslationProviderFailure.QuotaExceeded)
        }
        val states = makeJob(service).run("# Hello", "ko", "apikey").toList()

        val last = states.last()
        assertIs<TranslationJobState.Failed>(last)
        assertEquals(TranslationJobFailure.QuotaExceeded, last.reason)
    }
}
