package com.merlos.powerdetector.execution

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class ActionMessageRendererBatteryTest {
    private val renderer = ActionMessageRenderer()

    @Test
    fun rendersBatteryStatusForUnpluggedState() {
        val rendered = renderer.render(
            template = "Power source: {status}",
            isCharging = false,
            now = Date(0)
        )

        assertEquals("Power source: Battery", rendered)
    }
}
