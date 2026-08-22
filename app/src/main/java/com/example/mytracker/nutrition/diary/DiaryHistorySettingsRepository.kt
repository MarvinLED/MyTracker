package com.example.mytracker.nutrition.diary

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mytracker.core.metrics.ChartRange
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * A store of its own rather than a corner of `user_preferences`: this is what the Verlauf screen
 * looks like, not what the user is aiming at, and mixing view state into the goals store would put
 * it in front of everything that reads goals.
 */
private val Context.diaryHistorySettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "diary_history_settings",
)

/**
 * What the Verlauf screen was last set to. Persisted rather than kept in the ViewModel, because a
 * chart the user has configured to six specific lines is a setup worth an app restart — unlike the
 * app's other chart ranges, which reset by design.
 */
data class DiaryHistorySettings(
    val selectedSeries: Set<DiaryHistorySeries> = DEFAULT_SERIES,
    val chartRange: ChartRange = ChartRange.MONTH,
    val seriesPickerExpanded: Boolean = true,
    /**
     * Whether the running day is charted. On by default — the Verlauf ending today is what the
     * screen has always shown, and a day silently missing from the end would be the more surprising
     * default of the two.
     */
    val showToday: Boolean = true,
) {
    companion object {
        /** Calories against their target: the one pairing the Tagebuch already leads with. */
        val DEFAULT_SERIES = setOf(DiaryHistorySeries.KCAL_GOAL, DiaryHistorySeries.KCAL_ACTUAL)
    }
}

@Singleton
class DiaryHistorySettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val SELECTED_SERIES = stringSetPreferencesKey("selected_series")
        val CHART_RANGE = stringPreferencesKey("chart_range")
        val PICKER_EXPANDED = booleanPreferencesKey("series_picker_expanded")
        val SHOW_TODAY = booleanPreferencesKey("show_today")
    }

    val settings: Flow<DiaryHistorySettings> = context.diaryHistorySettingsDataStore.data.map { prefs ->
        DiaryHistorySettings(
            // An unknown name means a series was renamed or dropped; skipping it beats failing to
            // read the settings at all, and the screen shows what survived.
            selectedSeries = prefs[Keys.SELECTED_SERIES]
                ?.mapNotNull { name -> DiaryHistorySeries.entries.firstOrNull { it.name == name } }
                ?.toSet()
                ?: DiaryHistorySettings.DEFAULT_SERIES,
            chartRange = prefs[Keys.CHART_RANGE]?.let { name ->
                ChartRange.entries.firstOrNull { it.name == name }
            } ?: ChartRange.MONTH,
            seriesPickerExpanded = prefs[Keys.PICKER_EXPANDED] ?: true,
            showToday = prefs[Keys.SHOW_TODAY] ?: true,
        )
    }

    suspend fun current(): DiaryHistorySettings = settings.first()

    /**
     * An empty set is stored as such, not reset to the default — clearing every checkbox is a
     * deliberate act, and re-checking Kalorien behind the user's back would undo it.
     */
    suspend fun setSelectedSeries(series: Set<DiaryHistorySeries>) {
        context.diaryHistorySettingsDataStore.edit {
            it[Keys.SELECTED_SERIES] = series.map(DiaryHistorySeries::name).toSet()
        }
    }

    suspend fun setChartRange(range: ChartRange) {
        context.diaryHistorySettingsDataStore.edit { it[Keys.CHART_RANGE] = range.name }
    }

    suspend fun setSeriesPickerExpanded(expanded: Boolean) {
        context.diaryHistorySettingsDataStore.edit { it[Keys.PICKER_EXPANDED] = expanded }
    }

    suspend fun setShowToday(show: Boolean) {
        context.diaryHistorySettingsDataStore.edit { it[Keys.SHOW_TODAY] = show }
    }
}
