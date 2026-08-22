package com.example.mytracker.core.backup

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the app decides, at start-up, that it owes the user a backup. */
class AutoBackupDueTest {
    private val now = Instant.parse("2026-08-07T10:00:00Z")

    @Test
    fun nothingIsEverDueWhileTheIntervalIsOff() {
        assertFalse(isBackupDue(lastBackupAtEpochMillis = null, BackupInterval.OFF, now))
        assertFalse(isBackupDue(daysAgo(365), BackupInterval.OFF, now))
    }

    /** Switching the schedule on should write a file, not leave the folder empty for a day. */
    @Test
    fun neverHavingBackedUpIsDue() {
        assertTrue(isBackupDue(lastBackupAtEpochMillis = null, BackupInterval.DAILY, now))
        assertTrue(isBackupDue(lastBackupAtEpochMillis = null, BackupInterval.MONTHLY, now))
    }

    @Test
    fun aFullPeriodSinceTheLastOneIsDue() {
        assertTrue(isBackupDue(daysAgo(1), BackupInterval.DAILY, now))
        assertTrue(isBackupDue(daysAgo(7), BackupInterval.WEEKLY, now))
        assertTrue(isBackupDue(daysAgo(30), BackupInterval.MONTHLY, now))
    }

    @Test
    fun partOfAPeriodIsNotDueYet() {
        assertFalse(isBackupDue(hoursAgo(23), BackupInterval.DAILY, now))
        assertFalse(isBackupDue(daysAgo(6), BackupInterval.WEEKLY, now))
        assertFalse(isBackupDue(daysAgo(29), BackupInterval.MONTHLY, now))
    }

    /** Long overdue stays due — a phone left in a drawer backs up the moment it is opened. */
    @Test
    fun wellPastAPeriodIsStillDue() {
        assertTrue(isBackupDue(daysAgo(40), BackupInterval.DAILY, now))
    }

    /**
     * A timestamp in the future comes from a clock that was wound back, or from settings restored
     * off another device. Treating it as due beats an automatic backup that silently stops until
     * the calendar catches up.
     */
    @Test
    fun aTimestampFromTheFutureIsDue() {
        val tomorrow = now.plusSeconds(86_400).toEpochMilli()

        assertTrue(isBackupDue(tomorrow, BackupInterval.DAILY, now))
    }

    private fun daysAgo(days: Long) = now.minusSeconds(days * 86_400).toEpochMilli()
    private fun hoursAgo(hours: Long) = now.minusSeconds(hours * 3_600).toEpochMilli()
}
