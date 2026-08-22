package com.example.prokject2_tracker.core.backup

import java.time.Duration

/** How often the app writes a backup on its own, once a Speicherort is set. */
enum class BackupInterval(val label: String, val period: Duration?) {
    OFF("Aus", null),
    DAILY("Täglich", Duration.ofDays(1)),
    WEEKLY("Wöchentlich", Duration.ofDays(7)),
    MONTHLY("Monatlich", Duration.ofDays(30)),
}

/** What an automatic backup does with the ones already in the folder. */
enum class BackupRetention(val label: String) {
    /** One file, rewritten every time. Always current, but a bad export overwrites the good one. */
    OVERWRITE("Immer überschreiben"),

    /** A new dated file each time, oldest deleted past [BackupSettings.keepCount]. */
    KEEP_LAST("Die letzten Backups behalten"),
}

/**
 * Everything the Backup screen remembers between runs.
 *
 * [folderUri] is a SAF tree the user picked, held as a string because that is what survives in
 * DataStore; the read/write grant behind it is taken persistably when it is picked, so it outlives
 * a restart. Null means no folder, which is what turns the automatic backup off no matter what
 * [interval] says — there would be nowhere to write.
 */
data class BackupSettings(
    val folderUri: String? = null,
    /** The folder's human-readable name, for showing what was picked without re-querying SAF. */
    val folderLabel: String? = null,
    /** Which categories an automatic backup writes. Manual exports pick their own. */
    val autoScopes: Set<BackupScope> = BackupScope.entries.toSet(),
    val interval: BackupInterval = BackupInterval.OFF,
    val retention: BackupRetention = BackupRetention.KEEP_LAST,
    /** How many files [BackupRetention.KEEP_LAST] holds on to. */
    val keepCount: Int = DEFAULT_KEEP_COUNT,
    val lastBackupAtEpochMillis: Long? = null,
) {
    /** Automatic backups only ever run with somewhere to write, something to write and a schedule. */
    val autoBackupPossible: Boolean
        get() = folderUri != null && interval != BackupInterval.OFF && autoScopes.isNotEmpty()

    companion object {
        const val DEFAULT_KEEP_COUNT = 3
        const val MIN_KEEP_COUNT = 1

        /** An arbitrary but sane ceiling: past this the folder is an archive, not a backup. */
        const val MAX_KEEP_COUNT = 20
    }
}
