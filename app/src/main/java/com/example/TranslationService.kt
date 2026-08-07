package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LanguageOption(val code: String, val name: String, val flag: String)

object TranslationLanguages {
    val supportedLanguages = listOf(
        LanguageOption("en", "English", "🇺🇸"),
        LanguageOption("es", "Spanish", "🇪🇸"),
        LanguageOption("fr", "French", "🇫🇷"),
        LanguageOption("de", "German", "🇩🇪"),
        LanguageOption("zh-CN", "Chinese (Simplified)", "🇨🇳"),
        LanguageOption("ja", "Japanese", "🇯🇵"),
        LanguageOption("ko", "Korean", "🇰🇷"),
        LanguageOption("it", "Italian", "🇮🇹"),
        LanguageOption("pt", "Portuguese", "🇵🇹"),
        LanguageOption("ru", "Russian", "🇷🇺"),
        LanguageOption("ar", "Arabic", "🇸🇦"),
        LanguageOption("hi", "Hindi", "🇮🇳"),
        LanguageOption("tr", "Turkish", "🇹🇷"),
        LanguageOption("nl", "Dutch", "🇳🇱"),
        LanguageOption("pl", "Polish", "🇵🇱"),
        LanguageOption("sv", "Swedish", "🇸🇪"),
        LanguageOption("id", "Indonesian", "🇮🇩"),
        LanguageOption("vi", "Vietnamese", "🇻🇳"),
        LanguageOption("th", "Thai", "🇹🇭")
    )
}

object TranslationService {

    /**
     * Translates a block of text using the free MyMemory Translation API.
     */
    suspend fun translateText(
        text: String,
        sourceLang: String = "autodetect",
        targetLang: String = "en"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext Result.success("")
        try {
            val src = if (sourceLang == "autodetect") "auto" else sourceLang
            val langPair = "$src|$targetLang"
            val encodedQuery = URLEncoder.encode(text, "UTF-8")
            val urlString = "https://api.mymemory.translated.net/get?q=$encodedQuery&langpair=$langPair"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val responseData = json.optJSONObject("responseData")
                val translated = responseData?.optString("translatedText") ?: ""
                if (translated.isNotBlank()) {
                    Result.success(translated)
                } else {
                    Result.failure(Exception("Translation returned empty result"))
                }
            } else {
                Result.failure(Exception("HTTP error ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates Google Web Translate URL for translating the active webpage.
     */
    fun getWebPageTranslationUrl(webUrl: String, targetLangCode: String): String {
        if (webUrl.isBlank() || webUrl.startsWith("about:") || webUrl.startsWith("data:")) return webUrl
        val encodedUrl = URLEncoder.encode(webUrl, "UTF-8")
        return "https://translate.google.com/translate?sl=auto&tl=$targetLangCode&u=$encodedUrl"
    }
}
