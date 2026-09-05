package com.example.mytracker.achievements

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A store of its own rather than a corner of `user_preferences`: this is what the screen has already
 * shown, not something the user is aiming at.
 */
private val Context.achievementsSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "achievements_settings",
)

/** What the wall looked like the last time it was open. */
data class AchievementsSettings(
    val seenIds: Set<String> = emptySet(),
    /**
     * False until the wall has been opened once. It is what stops the first visit from marking a
     * year of earned history as brand new — everything is new then, and a screen where every row
     * shouts is a screen where nothing does.
     */
    val hasBeenSeen: Boolean = false,
)

@Singleton
class AchievementsSettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val SEEN_IDS = stringSetPreferencesKey("seen_ids")
        val HAS_BEEN_SEEN = booleanPreferencesKey("has_been_seen")
    }

    val settings: Flow<AchievementsSettings> = context.achievementsSettingsDataStore.data.map { prefs ->
        AchievementsSettings(
            seenIds = prefs[Keys.SEEN_IDS].orEmpty(),
            hasBeenSeen = prefs[Keys.HAS_BEEN_SEEN] ?: false,
        )
    }

    /**
     * Records everything currently on the wall as seen. Written wholesale rather than added to, so
     * a record that has since been beaten stops taking up room in the store.
     */
    suspend fun markSeen(ids: Set<String>) {
        context.achievementsSettingsDataStore.edit {
            it[Keys.SEEN_IDS] = ids
            it[Keys.HAS_BEEN_SEEN] = true
        }
    }
}
