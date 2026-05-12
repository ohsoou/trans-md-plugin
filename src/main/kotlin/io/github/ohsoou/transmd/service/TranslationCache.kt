package io.github.ohsoou.transmd.service

import com.intellij.openapi.components.Service
import java.util.Collections

@Service(Service.Level.APP)
class TranslationCache {

    private val cache: MutableMap<String, String> = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>) =
                size > MAX_ENTRIES
        }
    )

    fun get(contentHash: Int, targetLang: String): String? =
        cache[key(contentHash, targetLang)]

    fun put(contentHash: Int, targetLang: String, value: String) {
        cache[key(contentHash, targetLang)] = value
    }

    private fun key(contentHash: Int, targetLang: String) = "google:auto:$targetLang:$contentHash"

    companion object {
        private const val MAX_ENTRIES = 200
    }
}