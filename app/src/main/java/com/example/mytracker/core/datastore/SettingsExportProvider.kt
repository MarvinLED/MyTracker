package com.example.mytracker.core.datastore

import com.example.mytracker.core.backup.BackupExportProvider
import com.example.mytracker.core.backup.BackupInterval
import com.example.mytracker.core.backup.BackupRetention
import com.example.mytracker.core.backup.BackupScope
import com.example.mytracker.core.backup.BackupSettings
import com.example.mytracker.core.backup.BackupSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class SettingsDto(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val backupInterval: BackupInterval = BackupInterval.OFF,
    val backupRetention: BackupRetention = BackupRetention.KEEP_LAST,
    val backupKeepCount: Int = BackupSettings.DEFAULT_KEEP_COUNT,
)

/**
 * The app's preferences: the units it displays in, plus how the backup itself is configured — so a
 * new phone arrives with the same schedule rather than a silently disabled one.
 *
 * The Speicherort is deliberately *not* in here. A SAF tree uri is a grant issued to one app install
 * on one device; restoring one somewhere else would point the backup at a folder it has no
 * permission to write, and the failure would only show up the next time a backup was due.
 *
 * The goals live in `com.example.mytracker.goals.GoalsExportProvider` instead, even though
 * they share this DataStore: a Ziel is something the user built, and belongs with the Bibliothek.
 */
class SettingsExportProvider @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val backupSettingsRepository: BackupSettingsRepository,
) : BackupExportProvider {
    override val key = "settings"
    override val scope = BackupScope.SETTINGS

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement {
        val prefs = userPreferencesRepository.userPreferences.first()
        val backup = backupSettingsRepository.current()
        return json.encodeToJsonElement(
            SettingsDto(
                weightUnit = prefs.weightUnit,
                backupInterval = backup.interval,
                backupRetention = backup.retention,
                backupKeepCount = backup.keepCount,
            ),
        )
    }

    /** A single record, so there is nothing to merge: the imported settings simply win. */
    override suspend fun import(json: JsonElement) {
        val dto = this.json.decodeFromJsonElement<SettingsDto>(json)
        userPreferencesRepository.setWeightUnit(dto.weightUnit)
        backupSettingsRepository.setInterval(dto.backupInterval)
        backupSettingsRepository.setRetention(dto.backupRetention)
        backupSettingsRepository.setKeepCount(dto.backupKeepCount)
    }

    /** Back to the defaults — the settings equivalent of an empty table. */
    override suspend fun clear() {
        val defaults = SettingsDto()
        userPreferencesRepository.setWeightUnit(defaults.weightUnit)
        backupSettingsRepository.setInterval(defaults.backupInterval)
        backupSettingsRepository.setRetention(defaults.backupRetention)
        backupSettingsRepository.setKeepCount(defaults.backupKeepCount)
    }
}
