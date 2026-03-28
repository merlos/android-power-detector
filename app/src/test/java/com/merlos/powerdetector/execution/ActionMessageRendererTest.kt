package com.merlos.powerdetector.execution

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ActionMessageRendererTest {
    private val renderer = ActionMessageRenderer()

    @Test
    fun rendersStatusAndTimePlaceholdersForCharging() {
        val rendered = renderer.render(
            template = "State {status} at {time}",
            isCharging = true,
            now = Date(0),
            locale = Locale.US,
            timeZone = TimeZone.getTimeZone("UTC")
        )

        assertEquals("State AC Power at 1970-01-01 00:00:00", rendered)
    }

    @Test
    fun leavesPlainTextUntouchedWhenNoPlaceholdersExist() {
        val rendered = renderer.render(
            template = "No placeholders here",
            isCharging = false,
            now = Date(123456789)
        )

        assertEquals("No placeholders here", rendered)
    }
}
