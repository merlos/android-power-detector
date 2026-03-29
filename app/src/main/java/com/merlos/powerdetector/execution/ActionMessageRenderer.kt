package com.merlos.powerdetector.execution

import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ActionMessageRenderer {
    fun render(
        template: String,
        isCharging: Boolean,
        now: Date = Date(),
        locale: Locale = Locale.getDefault(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        return template
            .replace("{status}", if (isCharging) "AC Power" else "Battery")
            .replace("{time}", AppDateTimeFormatter.format(now, locale, timeZone))
    }
}
