package io.github.ohsoou.transmd.service.impl

import com.google.gson.JsonParser
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

    override suspend fun translate(text: String, targetLang: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = """{"q":${gson(text)},"target":"$targetLang","format":"text"}"""

                val request = Request.Builder()
                    .url("https://translation.googleapis.com/language/translate/v2?key=$apiKey")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val errorMsg = parseErrorMessage(responseBody, response.code)
                    throw TranslationException(response.code, errorMsg)
                }

                parseTranslatedText(responseBody)
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
            when (code) {
                400 -> "잘못된 요청입니다. API 키와 언어 설정을 확인하세요."
                403 -> "API 키가 유효하지 않거나 결제 계정이 연결되지 않았습니다."
                429 -> "번역 할당량을 초과했습니다. 잠시 후 다시 시도하세요."
                else -> "번역 서버 오류 (HTTP $code)"
            }
        }
    }
}

class TranslationException(val httpCode: Int, message: String) : Exception(message)