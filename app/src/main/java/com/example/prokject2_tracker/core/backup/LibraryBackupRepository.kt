package com.example.prokject2_tracker.core.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds/reads the versioned JSON envelope used to export and import library data (Lebensmittel,
 * Rezepte, ...) independently of tracked events. Each [LibraryExportProvider] owns one key in the
 * envelope; new feature modules plug in by contributing a provider, with no changes here.
 */
@Singleton
class LibraryBackupRepository @Inject constructor(
    private val exportProviders: Set<@JvmSuppressWildcards LibraryExportProvider>,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportToJson(): String {
        val payload = buildJsonObject {
            put("schemaVersion", SCHEMA_VERSION)
            put("exportedAt", Instant.now().toEpochMilli())
            exportProviders.forEach { provider ->
                put(provider.key, provider.export())
            }
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    suspend fun importFromJson(rawJson: String) {
        val root = json.parseToJsonElement(rawJson).jsonObject
        exportProviders.sortedBy { it.importPriority }.forEach { provider ->
            root[provider.key]?.let { provider.import(it) }
        }
    }

    private companion object {
        const val SCHEMA_VERSION = 1
    }
}
