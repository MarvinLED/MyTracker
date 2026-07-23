package com.example.prokject2_tracker.core.backup

import kotlinx.serialization.json.JsonElement

/**
 * Implemented by each feature module to make its library/master data (e.g. Lebensmittel, Rezepte)
 * exportable and importable independently of tracked events (diary, workouts, ...). Each module
 * contributes one entry, keyed by [key], to the JSON envelope built by [LibraryBackupRepository].
 */
interface LibraryExportProvider {
    val key: String

    /**
     * Import order across providers, ascending (default 0). Raise this above 0 when a provider's
     * rows have a foreign key into another provider's rows (e.g. recipes reference foods), so the
     * referenced data is imported first on a fresh install.
     */
    val importPriority: Int get() = 0

    suspend fun export(): JsonElement

    /** Upsert by id: only overwrite an existing local row if the imported entity is newer. */
    suspend fun import(json: JsonElement)
}
