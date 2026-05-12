package io.github.ohsoou.transmd

import io.github.ohsoou.transmd.service.TranslationCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TranslationCacheTest {

    private fun cache() = TranslationCache()

    @Test
    fun `cache hit returns stored value`() {
        val cache = cache()
        cache.put(12345, "ko", "번역된 텍스트")
        assertEquals("번역된 텍스트", cache.get(12345, "ko"))
    }

    @Test
    fun `cache miss returns null`() {
        val cache = cache()
        assertNull(cache.get(99999, "ko"))
    }

    @Test
    fun `different target languages are distinct keys`() {
        val cache = cache()
        cache.put(12345, "ko", "한국어")
        cache.put(12345, "en", "English")

        assertEquals("한국어", cache.get(12345, "ko"))
        assertEquals("English", cache.get(12345, "en"))
    }

    @Test
    fun `different content hashes are distinct keys`() {
        val cache = cache()
        cache.put(111, "ko", "first")
        cache.put(222, "ko", "second")

        assertEquals("first", cache.get(111, "ko"))
        assertEquals("second", cache.get(222, "ko"))
    }
}