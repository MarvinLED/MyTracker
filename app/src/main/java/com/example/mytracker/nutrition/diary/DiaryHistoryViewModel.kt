package com.example.mytracker.nutrition.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.core.datastore.Nutrient
import com.example.mytracker.core.datastore.UserPreferencesRepository
import com.example.mytracker.core.metrics.ChartRange
import com.example.mytracker.core.metrics.Granularity
import com.example.mytracker.core.metrics.granularityFor
import com.example.mytracker.core.ui.ChartLine
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.goals.NutrientGoalChangeDao
import com.example.mytracker.goals.nutrientGoalTimeline
import com.example.mytracker.weight.BodyWeightDao
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
    /** The Kalorien-Gewicht comparison in place of the Verlauf — see [weeklyEnergy]. */
    val weeklyComparison: Boolean = false,
    /** One dot per complete week, null while the comparison is switched off. */
    val weeklyEnergy: WeeklyEnergySummary? = null,
    /** Whether the running — and therefore only part-logged — day is charted. */
    val showToday: Boolean = true,
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
            val range = diaryHistoryRange(settings.chartRange, firstDay, today, settings.showToday)
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
                    showToday = settings.showToday,
                    weeklyComparison = settings.weeklyComparison,
                    // Only walked when it is on screen: it is a second pass over the same window,
                    // and the Verlauf is what most visits to this screen are for.
                    weeklyEnergy = if (settings.weeklyComparison) {
                        weeklyEnergySummary(
                            points = weeklyEnergyPoints(range, nutritionTotals, weights),
                            // The same Soll the Verlauf draws, so "dein Ziel" is the line the other
                            // view shows and not a second reading of the goal history.
                            goalTimeline = nutrientGoalTimeline(
                                range = range,
                                changes = goalChanges.filter { it.nutrient == Nutrient.KCAL },
                                currentGoal = prefs.nutrientGoals[Nutrient.KCAL],
                            ),
                        )
                    } else {
                        null
                    },
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

    fun onShowTodayChange(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowToday(show) }
    }

    fun onWeeklyComparisonChange(comparing: Boolean) {
        viewModelScope.launch { settingsRepository.setWeeklyComparison(comparing) }
    }

    fun onSeriesPickerExpandedChange(expanded: Boolean) {
        viewModelScope.launch { settingsRepository.setSeriesPickerExpanded(expanded) }
    }
}
