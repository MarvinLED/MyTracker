package com.example.prokject2_tracker.core.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A store of its own rather than a corner of `user_preferences`: these settings describe where this
 * device writes its backups, and the only thing that must never be restored *from* a backup is the
 * folder it came out of.
 */
private val Context.backupSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "backup_settings",
)

@Singleton
class BackupSettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val FOLDER_URI = stringPreferencesKey("folder_uri")
        val FOLDER_LABEL = stringPreferencesKey("folder_label")
        val AUTO_SCOPES = stringSetPreferencesKey("auto_scopes")
        val INTERVAL = stringPreferencesKey("interval")
        val RETENTION = stringPreferencesKey("retention")
        val KEEP_COUNT = intPreferencesKey("keep_count")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
    }

    val settings: Flow<BackupSettings> = context.backupSettingsDataStore.data.map { prefs ->
        BackupSettings(
            folderUri = prefs[Keys.FOLDER_URI],
            folderLabel = prefs[Keys.FOLDER_LABEL],
            // An unknown name means a scope was renamed or dropped; skipping it is better than
            // failing to read the settings at all, and the screen shows what survived.
            autoScopes = prefs[Keys.AUTO_SCOPES]
                ?.mapNotNull { name -> BackupScope.entries.firstOrNull { it.name == name } }
                ?.toSet()
                ?: BackupScope.entries.toSet(),
            interval = prefs[Keys.INTERVAL]?.let { name ->
                BackupInterval.entries.firstOrNull { it.name == name }
            } ?: BackupInterval.OFF,
            retention = prefs[Keys.RETENTION]?.let { name ->
                BackupRetention.entries.firstOrNull { it.name == name }
            } ?: BackupRetention.KEEP_LAST,
            keepCount = prefs[Keys.KEEP_COUNT] ?: BackupSettings.DEFAULT_KEEP_COUNT,
            lastBackupAtEpochMillis = prefs[Keys.LAST_BACKUP_AT],
        )
    }

    suspend fun current(): BackupSettings = settings.first()

    /** Null clears the folder, which also stops automatic backups — there is nowhere to write. */
    suspend fun setFolder(uri: String?, label: String?) {
        context.backupSettingsDataStore.edit { prefs ->
            if (uri != null) prefs[Keys.FOLDER_URI] = uri else prefs.remove(Keys.FOLDER_URI)
            if (label != null) prefs[Keys.FOLDER_LABEL] = label else prefs.remove(Keys.FOLDER_LABEL)
        }
    }

    suspend fun setAutoScopes(scopes: Set<BackupScope>) {
        context.backupSettingsDataStore.edit { it[Keys.AUTO_SCOPES] = scopes.map(BackupScope::name).toSet() }
    }

    suspend fun setInterval(interval: BackupInterval) {
        context.backupSettingsDataStore.edit { it[Keys.INTERVAL] = interval.name }
    }

    suspend fun setRetention(retention: BackupRetention) {
        context.backupSettingsDataStore.edit { it[Keys.RETENTION] = retention.name }
    }

    suspend fun setKeepCount(count: Int) {
        context.backupSettingsDataStore.edit {
            it[Keys.KEEP_COUNT] = count.coerceIn(BackupSettings.MIN_KEEP_COUNT, BackupSettings.MAX_KEEP_COUNT)
        }
    }

    suspend fun setLastBackupAt(epochMillis: Long) {
        context.backupSettingsDataStore.edit { it[Keys.LAST_BACKUP_AT] = epochMillis }
    }
}
