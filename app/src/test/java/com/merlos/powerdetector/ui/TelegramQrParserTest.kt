package com.merlos.powerdetector.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TelegramQrParserTest {
    @Test
    fun parsesValidTelegramSetupPayload() {
        val payload = TelegramQrParser.parse("powerdetector://telegram?botid=123456%3AABCDEF&chatid=-100123456")

        requireNotNull(payload)
        assertEquals("123456:ABCDEF", payload.botId)
        assertEquals("-100123456", payload.chatId)
    }

    @Test
    fun rejectsInvalidScheme() {
        assertNull(TelegramQrParser.parse("https://example.com"))
    }

    @Test
    fun rejectsMissingFields() {
        assertNull(TelegramQrParser.parse("powerdetector://telegram?botid=only"))
    }
}
