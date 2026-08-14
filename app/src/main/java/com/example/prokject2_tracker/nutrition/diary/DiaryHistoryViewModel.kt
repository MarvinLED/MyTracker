package com.example.prokject2_tracker.nutrition.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.datastore.UserPreferencesRepository
import com.example.prokject2_tracker.core.metrics.ChartRange
import com.example.prokject2_tracker.core.metrics.Granularity
import com.example.prokject2_tracker.core.metrics.granularityFor
import com.example.prokject2_tracker.core.ui.ChartLine
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.goals.NutrientGoalChangeDao
import com.example.prokject2_tracker.weight.BodyWeightDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiaryHistoryUiState(
    val chartRange: ChartRange = ChartRange.MONTH,
    val selectedSeries: Set<DiaryHistorySeries> = DiaryHistorySettings.DEFAULT_SERIES,
    val seriesPickerExpanded: Boolean = true,
    val lines: List<ChartLine> = emptyList(),
    /** What one point stands for, so the y values are never ambiguous once the range coarsens. */
    val granularity: Granularity = Granularity.DAILY,
    /** One nutrient on screen: all lines share a scale with finely labelled steps. */
    val sharedScale: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiaryHistoryViewModel @Inject constructor(
    private val settingsRepository: DiaryHistorySettingsRepository,
    diaryDao: DiaryDao,
    bodyWeightDao: BodyWeightDao,
    nutrientGoalChangeDao: NutrientGoalChangeDao,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    // Fixed at construction like the Habits and Tagesziele screens: a window that silently rolled
    // over at midnight would move the chart under the user's hands.
    private val today = DateUtils.todayEpochDay()

    private val firstLoggedDay = combine(
        diaryDao.observeFirstLoggedDay(),
        bodyWeightDao.observeFirstLoggedDay(),
    ) { firstDiaryDay, firstWeightDay ->
        // Gewicht may well go back further than the diary — "Insgesamt" has to mean both.
        listOfNotNull(firstDiaryDay, firstWeightDay).minOrNull()
    }

    val uiState: StateFlow<DiaryHistoryUiState> = combine(
        settingsRepository.settings,
        firstLoggedDay,
    ) { settings, firstDay -> settings to firstDay }
        .flatMapLatest { (settings, firstDay) ->
            val range = diaryHistoryRange(settings.chartRange, firstDay, today)
            val granularity = settings.chartRange
                .granularityFor(range.endInclusive - range.startInclusive)

            combine(
                diaryDao.observeDailyNutritionTotals(range.startInclusive, range.endInclusive),
                bodyWeightDao.observeRange(range.startInclusive, range.endInclusive),
                nutrientGoalChangeDao.observeAll(),
                userPreferencesRepository.userPreferences,
            ) { nutritionTotals, weights, goalChanges, prefs ->
                DiaryHistoryUiState(
                    chartRange = settings.chartRange,
                    selectedSeries = settings.selectedSeries,
                    seriesPickerExpanded = settings.seriesPickerExpanded,
                    lines = diaryHistoryLines(
                        selected = settings.selectedSeries,
                        range = range,
                        granularity = granularity,
                        nutritionTotals = nutritionTotals,
                        weights = weights,
                        goalChanges = goalChanges,
                        currentGoals = prefs.nutrientGoals,
                    ),
                    granularity = granularity,
                    sharedScale = isSingleNutrientSelection(settings.selectedSeries),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiaryHistoryUiState())

    fun onChartRangeChange(range: ChartRange) {
        viewModelScope.launch { settingsRepository.setChartRange(range) }
    }

    fun onSeriesToggle(series: DiaryHistorySeries, selected: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.current().selectedSeries
            val next = if (selected) current + series else current - series
            settingsRepository.setSelectedSeries(next)
        }
    }

    fun onSeriesPickerExpandedChange(expanded: Boolean) {
        viewModelScope.launch { settingsRepository.setSeriesPickerExpanded(expanded) }
    }
}
