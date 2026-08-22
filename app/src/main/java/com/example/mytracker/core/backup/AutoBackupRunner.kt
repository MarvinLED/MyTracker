package com.example.mytracker.core.backup

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What one attempt at an automatic backup came to. */
sealed interface AutoBackupOutcome {
    /** No folder, no interval, or nothing selected — the automatic backup is simply off. */
    data object NotConfigured : AutoBackupOutcome

    data object NotDue : AutoBackupOutcome
    data class Written(val fileName: String, val prunedCount: Int) : AutoBackupOutcome
    data class Failed(val message: String) : AutoBackupOutcome
}

/**
 * Whether an automatic backup is owed at [now].
 *
 * Never backed up before counts as due, so switching the interval on writes a first file straight
 * away instead of leaving the folder empty for a day. A [lastBackupAtEpochMillis] in the future —
 * a clock that was wound back, or a restored settings file from another device — also counts as
 * due, because the alternative is an automatic backup that silently stops until the date catches up.
 */
fun isBackupDue(lastBackupAtEpochMillis: Long?, interval: BackupInterval, now: Instant): Boolean {
    val period = interval.period ?: return false
    val last = lastBackupAtEpochMillis ?: return true
    if (last > now.toEpochMilli()) return true
    return !Instant.ofEpochMilli(last).plus(period).isAfter(now)
}

/**
 * Writes backups into the configured folder — on demand for the "Jetzt sichern" button, and on a
 * schedule for [runIfDue].
 *
 * The schedule is checked when the app starts, not by a background job: without WorkManager there is
 * nothing to wake the app up, so "Täglich" means "on the first launch of a new day". That is the
 * honest limit of this design — an app left unopened for a week writes no backups in that week.
 */
@Singleton
class AutoBackupRunner @Inject constructor(
    private val settingsRepository: BackupSettingsRepository,
    private val backupRepository: BackupRepository,
    private val fileStore: BackupFileStore,
) {
    suspend fun runIfDue(now: Instant = Instant.now()): AutoBackupOutcome {
        val settings = settingsRepository.current()
        if (!settings.autoBackupPossible) return AutoBackupOutcome.NotConfigured
        if (!isBackupDue(settings.lastBackupAtEpochMillis, settings.interval, now)) {
            return AutoBackupOutcome.NotDue
        }
        return backupNow(settings.autoScopes, now)
    }

    /**
     * Exports [scopes] into the configured folder and prunes what falls out of the retention rule.
     *
     * Pruning runs after the new file is written and never touches it, so a folder is only ever cut
     * back around a backup that already exists. A failure to delete an old file does not fail the
     * backup — the new one is on disk either way, and reporting it as failed would be a lie that
     * costs the user the next scheduled attempt.
     */
    suspend fun backupNow(
        scopes: Set<BackupScope>,
        now: Instant = Instant.now(),
    ): AutoBackupOutcome = withContext(Dispatchers.IO) {
        val settings = settingsRepository.current()
        val folder = settings.folderUri ?: return@withContext AutoBackupOutcome.NotConfigured
        if (scopes.isEmpty()) return@withContext AutoBackupOutcome.NotConfigured

        runCatching {
            val name = backupFileName(settings.retention, now)
            fileStore.write(folder, name, backupRepository.exportToJson(scopes))
            settingsRepository.setLastBackupAt(now.toEpochMilli())

            val pruned = if (settings.retention == BackupRetention.KEEP_LAST) {
                backupsToPrune(fileStore.list(folder), settings.keepCount)
                    .count { file -> runCatching { fileStore.delete(file.uri) }.isSuccess }
            } else {
                0
            }
            AutoBackupOutcome.Written(fileName = name, prunedCount = pruned)
        }.getOrElse { AutoBackupOutcome.Failed(it.message ?: "Backup fehlgeschlagen") }
    }
}
