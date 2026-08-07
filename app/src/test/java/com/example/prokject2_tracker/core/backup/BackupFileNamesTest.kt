package com.example.prokject2_tracker.core.backup

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The naming and the retention rule — the part of the Aufbewahrung that decides which files get
 * deleted, kept out of the SAF layer precisely so it can be checked without a device.
 */
class BackupFileNamesTest {
    private val berlin = ZoneId.of("Europe/Berlin")

    // 2026-08-07 15:30 local time in Berlin (CEST, UTC+2).
    private val at = Instant.parse("2026-08-07T13:30:00Z")

    @Test
    fun overwritingAlwaysProducesTheSameName() {
        val first = backupFileName(BackupRetention.OVERWRITE, at, berlin)
        val later = backupFileName(BackupRetention.OVERWRITE, at.plusSeconds(86_400), berlin)

        assertEquals("mytracker-backup.json", first)
        assertEquals(first, later)
    }

    @Test
    fun keepingTheLastOnesStampsTheLocalDateAndTime() {
        assertEquals(
            "mytracker-backup-2026-08-07-1530.json",
            backupFileName(BackupRetention.KEEP_LAST, at, berlin),
        )
    }

    @Test
    fun onlyThisAppsFilesAreRecognised() {
        assertTrue(isBackupFileName("mytracker-backup.json"))
        assertTrue(isBackupFileName("mytracker-backup-2026-08-07-1530.json"))
        assertFalse(isBackupFileName("steuer-2025.json"))
        assertFalse(isBackupFileName("mytracker-backup-2026-08-07-1530.txt"))
    }

    @Test
    fun nothingIsPrunedWhileTheFolderHoldsAtMostTheKeepCount() {
        val files = listOf(file("a", 3), file("b", 2), file("c", 1))

        assertEquals(emptyList<BackupFile>(), backupsToPrune(files, keepCount = 3))
        assertEquals(emptyList<BackupFile>(), backupsToPrune(files.take(2), keepCount = 3))
        assertEquals(emptyList<BackupFile>(), backupsToPrune(emptyList(), keepCount = 3))
    }

    @Test
    fun theOldestBeyondTheKeepCountArePruned() {
        val newest = file("newest", 5)
        val middle = file("middle", 4)
        val older = file("older", 3)
        val oldest = file("oldest", 1)

        val pruned = backupsToPrune(listOf(older, newest, oldest, middle), keepCount = 2)

        assertEquals(listOf(older, oldest), pruned)
    }

    /** The single-file case [BackupRetention.OVERWRITE] produces needs no special handling. */
    @Test
    fun theOneOverwrittenFileIsNeverPruned() {
        val only = listOf(file("mytracker-backup.json", 7))

        assertEquals(emptyList<BackupFile>(), backupsToPrune(only, keepCount = 3))
        assertEquals(emptyList<BackupFile>(), backupsToPrune(only, keepCount = 1))
    }

    /** Age comes from the folder, not from the name — a renamed file still has a real timestamp. */
    @Test
    fun ageIsTakenFromTheFolderNotFromTheName() {
        val renamedButNewest = file("mytracker-backup-2020-01-01-0000.json", 9)
        val stampedButOldest = file("mytracker-backup-2026-08-07-1530.json", 1)

        val pruned = backupsToPrune(listOf(renamedButNewest, stampedButOldest), keepCount = 1)

        assertEquals(listOf(stampedButOldest), pruned)
    }

    private fun file(name: String, modified: Long) = BackupFile(
        uri = "content://test/$name",
        name = name,
        lastModifiedEpochMillis = modified,
        sizeBytes = 1024,
    )
}
