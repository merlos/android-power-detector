package com.merlos.powerdetector.execution

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object AppDateTimeFormatter {
    private const val PATTERN = "dd-MMM-yyyy HH:mm"

    fun format(
        date: Date,
        locale: Locale = Locale.ENGLISH,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val formatter = SimpleDateFormat(PATTERN, locale)
        formatter.timeZone = timeZone
        return formatter.format(date)
    }
}
