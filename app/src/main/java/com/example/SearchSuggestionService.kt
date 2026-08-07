package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object SearchSuggestionService {
    suspend fun getSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val url = URL("https://suggestqueries.google.com/complete/search?client=chrome&q=${android.net.Uri.encode(query)}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                if (jsonArray.length() > 1) {
                    val suggestionsArray = jsonArray.getJSONArray(1)
                    val result = mutableListOf<String>()
                    for (i in 0 until suggestionsArray.length()) {
                        result.add(suggestionsArray.getString(i))
                    }
                    return@withContext result
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }
}
