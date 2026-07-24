package com.example.prokject2_tracker.core.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object DateUtils {
    fun todayEpochDay(zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zoneId).toEpochDay()

    fun epochDayOf(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.ofInstant(instant, zoneId).toEpochDay()

    fun epochDayOfMillis(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        epochDayOf(Instant.ofEpochMilli(epochMillis), zoneId)

    fun localDateOfEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun startOfWeekEpochDay(epochDay: Long): Long =
        localDateOfEpochDay(epochDay).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()

    fun startOfMonthEpochDay(epochDay: Long): Long =
        localDateOfEpochDay(epochDay).withDayOfMonth(1).toEpochDay()
}
