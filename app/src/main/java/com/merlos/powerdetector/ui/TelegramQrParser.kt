package com.merlos.powerdetector.ui

import java.net.URI
import java.net.URLDecoder

object TelegramQrParser {
    fun parse(rawValue: String): TelegramQrPayload? {
        val uri = runCatching { URI(rawValue) }.getOrNull() ?: return null
        if (uri.scheme != "powerdetector" || uri.host != "telegram") {
            return null
        }

        val queryParameters = parseQuery(uri.rawQuery.orEmpty())
        val botId = queryParameters["botid"].orEmpty().trim()
        val chatId = queryParameters["chatid"].orEmpty().trim()
        if (botId.isBlank() || chatId.isBlank()) {
            return null
        }

        return TelegramQrPayload(
            botId = botId,
            chatId = chatId
        )
    }

    private fun parseQuery(rawQuery: String): Map<String, String> {
        if (rawQuery.isBlank()) {
            return emptyMap()
        }

        return rawQuery.split('&')
            .mapNotNull { token ->
                val index = token.indexOf('=')
                if (index <= 0) {
                    return@mapNotNull null
                }

                val key = URLDecoder.decode(token.substring(0, index), Charsets.UTF_8.name())
                val value = URLDecoder.decode(token.substring(index + 1), Charsets.UTF_8.name())
                key to value
            }
            .toMap()
    }
}
