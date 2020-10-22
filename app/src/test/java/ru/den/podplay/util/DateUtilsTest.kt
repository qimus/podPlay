package ru.den.podplay.util

import org.junit.Test

import org.junit.Assert.*

class DateUtilsTest {

    @Test
    fun xmlDateToDate_success() {
        val date = DateUtils.xmlDateToDate("Fri, 16 Oct 2020 12:36:46 GMT")
        assertEquals(16, date.date)
        assertEquals(9, date.month)
    }
}