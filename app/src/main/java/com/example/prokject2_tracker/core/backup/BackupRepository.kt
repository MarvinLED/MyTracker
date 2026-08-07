package com.example.prokject2_tracker.core.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** What an import does with the rows already on the device. */
enum class ImportMode(val label: String) {
    /** Upsert by id, keeping whichever side is newer. Nothing is ever deleted. */
    MERGE("Zusammenführen"),

    /** Empty the imported categories first, so the result is exactly what the file holds. */
    REPLACE("Ersetzen"),
}

/** What a backup file turned out to contain, read before anything is written to the database. */
data class BackupFileInfo(
    val schemaVersion: Int,
    val exportedAt: Instant?,
    val scopes: Set<BackupScope>,
)

/**
 * Builds/reads the versioned JSON envelope every backup travels in. Each [BackupExportProvider] owns
 * one key in it, and each key belongs to one [BackupScope], so both export and import can be limited
 * to the categories the user ticked.
 *
 * The provider keys sit at the root of the envelope rather than under a wrapper, which is what makes
 * a schema-1 file — written when the only thing backupable was the library — still import today: it
 * simply has no `scopes` key, and is read as a library-only backup, which is exactly what it was.
 */
@Singleton
class BackupRepository @Inject constructor(
    private val exportProviders: Set<@JvmSuppressWildcards BackupExportProvider>,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportToJson(scopes: Set<BackupScope>): String {
        val providers = exportProviders.filter { it.scope in scopes }
        val payload = buildJsonObject {
            put("schemaVersion", SCHEMA_VERSION)
            put("exportedAt", Instant.now().toEpochMilli())
            put("scopes", JsonArray(scopes.map { JsonPrimitive(it.name) }))
            providers.sortedBy { it.key }.forEach { provider ->
                put(provider.key, provider.export())
            }
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    /**
     * What [rawJson] holds, so the import screen can offer only the categories that are actually in
     * the file. Throws if the text isn't a backup envelope at all — the caller reports that as a
     * failed import rather than silently restoring nothing.
     */
    fun readInfo(rawJson: String): BackupFileInfo {
        val root = json.parseToJsonElement(rawJson).jsonObject
        val schemaVersion = root["schemaVersion"]?.jsonPrimitive?.int
            ?: error("Keine Backup-Datei: 'schemaVersion' fehlt")
        val declared = (root["scopes"] as? JsonArray)
            ?.mapNotNull { element ->
                BackupScope.entries.firstOrNull { it.name == element.jsonPrimitive.content }
            }
            ?.toSet()
        return BackupFileInfo(
            schemaVersion = schemaVersion,
            exportedAt = root["exportedAt"]?.jsonPrimitive?.content?.toLongOrNull()
                ?.let(Instant::ofEpochMilli),
            // A schema-1 file predates scopes entirely and only ever held the library. Newer files
            // are still trusted to say so themselves, but fall back to the keys actually present —
            // an envelope hand-edited down to a few keys should offer only those.
            scopes = declared ?: scopesPresentIn(root),
        )
    }

    /**
     * Restores the [scopes] the user picked. Providers outside them are left untouched, even when the
     * file carries their data — "nur die Bibliothek zurückholen" has to mean exactly that.
     */
    suspend fun importFromJson(rawJson: String, scopes: Set<BackupScope>, mode: ImportMode) {
        val root = json.parseToJsonElement(rawJson).jsonObject
        // Only what the file can actually put back again. A provider the envelope says nothing about
        // — a table added after the backup was written — is left alone even under REPLACE: emptying
        // it would delete data this file was never going to restore.
        val providers = exportProviders.filter { it.scope in scopes && root.containsKey(it.key) }

        if (mode == ImportMode.REPLACE) {
            // Dependants first: a strength set has to go before the exercise it points at, or the
            // foreign key stops the delete halfway.
            providers.sortedByDescending { it.importPriority }.forEach { it.clear() }
        }
        providers.sortedBy { it.importPriority }.forEach { provider ->
            root.getValue(provider.key).let { provider.import(it) }
        }
    }

    private fun scopesPresentIn(root: JsonObject): Set<BackupScope> =
        exportProviders.filter { root.containsKey(it.key) }.map { it.scope }.toSet()

    private companion object {
        /** 1: library only, provider keys at the root. 2: adds `scopes`, all three categories. */
        const val SCHEMA_VERSION = 2
    }
}
