package io.github.ohsoou.transmd.service.impl

import com.google.gson.JsonParser
import io.github.ohsoou.transmd.service.TranslationProviderFailure
import io.github.ohsoou.transmd.service.TranslationProviderResult
import io.github.ohsoou.transmd.service.TranslationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GoogleTranslateService(private val apiKey: String) : TranslationService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun translate(text: String, targetLang: String): TranslationProviderResult =
        withContext(Dispatchers.IO) {
            try {
                val body = """{"q":${gson(text)},"target":"$targetLang","format":"text"}"""

                val request = Request.Builder()
                    .url("https://translation.googleapis.com/language/translate/v2?key=$apiKey")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext when (response.code) {
                        403 -> TranslationProviderResult.Failed(TranslationProviderFailure.PermissionDenied)
                        429 -> TranslationProviderResult.Failed(TranslationProviderFailure.QuotaExceeded)
                        else -> TranslationProviderResult.Failed(
                            TranslationProviderFailure.NetworkError(parseErrorMessage(responseBody, response.code))
                        )
                    }
                }

                TranslationProviderResult.Succeeded(parseTranslatedText(responseBody))
            } catch (e: Exception) {
                TranslationProviderResult.Failed(TranslationProviderFailure.UnexpectedError(e))
            }
        }

    private fun gson(value: String): String {
        val sb = StringBuilder("\"")
        for (c in value) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    private fun parseTranslatedText(json: String): String {
        val root = JsonParser.parseString(json).asJsonObject
        return root
            .getAsJsonObject("data")
            .getAsJsonArray("translations")
            .get(0).asJsonObject
            .get("translatedText").asString
    }

    private fun parseErrorMessage(body: String, code: Int): String {
        return try {
            JsonParser.parseString(body).asJsonObject
                .getAsJsonObject("error")
                .get("message").asString
        } catch (_: Exception) {
            "번역 서버 오류 (HTTP $code)"
        }
    }
}
