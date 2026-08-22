package com.example.mytracker.core.backup

import kotlinx.serialization.json.JsonElement

/**
 * Implemented by each feature module to make its data backupable. Each module contributes one entry,
 * keyed by [key], to the JSON envelope built by [BackupRepository]; [scope] decides which of the
 * user's three categories that entry travels in, so new feature modules plug in by contributing a
 * provider, with no changes to the repository.
 */
interface BackupExportProvider {
    val key: String

    /** Which of the user-selectable categories this provider's data belongs to. */
    val scope: BackupScope

    /**
     * Import order across providers, ascending (default 0). Raise this above 0 when a provider's
     * rows have a foreign key into another provider's rows (e.g. recipes reference foods), so the
     * referenced data is imported first on a fresh install. Tracked data ([BackupScope.DAILY_ENTRIES])
     * points into the library, so it starts at [DAILY_ENTRIES_PRIORITY].
     */
    val importPriority: Int get() = 0

    suspend fun export(): JsonElement

    /** Upsert by id: only overwrite an existing local row if the imported entity is newer. */
    suspend fun import(json: JsonElement)

    /**
     * Deletes everything this provider owns — what [ImportMode.REPLACE] runs before importing, so a
     * restore reproduces the backup exactly instead of merging into what is already there. Called in
     * reverse [importPriority] order, i.e. dependants before what they depend on.
     */
    suspend fun clear()

    companion object {
        /**
         * Where tracked data starts, comfortably above every library provider: a diary entry names a
         * food, a strength set names an exercise, so the library has to land first.
         */
        const val DAILY_ENTRIES_PRIORITY = 100
    }
}
