package com.merlos.powerdetector.execution

import android.util.Log
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class TelegramApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class)
    fun sendMessage(botToken: String, chatId: String, text: String) {
        // Log only the suffix of the token to avoid leaking credentials
        val tokenHint = if (botToken.length > 6) "...${botToken.takeLast(6)}" else "***"
        Log.d(TAG, "POST sendMessage chatId=$chatId token=$tokenHint")

        val body = FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", text)
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$botToken/sendMessage")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            Log.d(TAG, "Response HTTP ${response.code} for chatId=$chatId")
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()?.take(200) ?: ""
                Log.e(TAG, "Telegram API error ${response.code}: $errorBody")
                throw IOException("Telegram API error ${response.code}: $errorBody")
            }
        }
    }

    companion object {
        private const val TAG = "TelegramApiClient"
    }
}
