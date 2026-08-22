package com.example.prokject2_tracker.core.backup

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** One backup sitting in the chosen folder. */
data class BackupFile(
    /** The SAF document uri, as a string — what [BackupFileStore] reads and deletes by. */
    val uri: String,
    val name: String,
    val lastModifiedEpochMillis: Long,
    val sizeBytes: Long,
)

/** The prefix every backup this app writes starts with — and how it recognises its own files again. */
const val BACKUP_FILE_PREFIX = "mytracker-backup"
const val BACKUP_FILE_EXTENSION = ".json"

private val FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")

/**
 * What the next backup is called.
 *
 * [BackupRetention.OVERWRITE] always returns the same name, which is what makes it overwrite: the
 * store finds the existing document and rewrites it. [BackupRetention.KEEP_LAST] stamps the local
 * date and time into the name, so the files sort by age under a plain alphabetical listing and stay
 * readable to a human scrolling the folder.
 */
fun backupFileName(
    retention: BackupRetention,
    at: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
): String = when (retention) {
    BackupRetention.OVERWRITE -> "$BACKUP_FILE_PREFIX$BACKUP_FILE_EXTENSION"
    BackupRetention.KEEP_LAST ->
        "$BACKUP_FILE_PREFIX-${FILE_STAMP.format(at.atZone(zone))}$BACKUP_FILE_EXTENSION"
}

/** Whether [name] is one of this app's backups, i.e. something it may prune or offer to restore. */
fun isBackupFileName(name: String): Boolean =
    name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_EXTENSION)

/**
 * The files to delete so that at most [keepCount] remain, newest kept.
 *
 * Sorted by the folder's own modification time rather than by the stamp in the name: a file the user
 * renamed still has a real age, and a clock that jumped backwards would otherwise make the newest
 * backup look like the oldest and delete it. Ties break on the name so the result is deterministic.
 *
 * Returns nothing when there are [keepCount] files or fewer — including the single-file case that
 * [BackupRetention.OVERWRITE] produces, which is why that mode needs no special handling here.
 */
fun backupsToPrune(existing: List<BackupFile>, keepCount: Int): List<BackupFile> {
    if (keepCount < 1) return emptyList()
    return existing
        .sortedWith(compareByDescending<BackupFile> { it.lastModifiedEpochMillis }.thenByDescending { it.name })
        .drop(keepCount)
}
