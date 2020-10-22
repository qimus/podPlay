package ru.den.podplay.util

import android.text.format.DateUtils
import java.lang.Exception
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun jsonDateToShortDate(jsonDate: String?): String {
        if (jsonDate == null) {
            return "-"
        }

        val inFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
        val date = inFormat.parse(jsonDate) ?: return "-"
        val outputFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.ENGLISH)

        return outputFormat.format(date)
    }

    fun xmlDateToDate(dateString: String?): Date {
        val date = dateString ?: return Date()
        val inFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
        return try {
            inFormat.parse(date) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    fun dateToShortDate(date: Date): String {
        val outputFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.ENGLISH)
        return outputFormat.format(date)
    }

    fun formatDuration(time: String): String {
        return if (time.indexOf(":") > -1) {
            time
        } else {
            DateUtils.formatElapsedTime(time.toLong())
        }
    }
}
