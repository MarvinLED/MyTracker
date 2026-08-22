package com.example.mytracker.analyse

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.core.metrics.AnalyseDateRange
import com.example.mytracker.core.metrics.Granularity
import com.example.mytracker.core.metrics.MetricAggregation
import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.core.metrics.MetricSeriesDescriptor
import com.example.mytracker.core.metrics.MetricSeriesProvider
import com.example.mytracker.core.metrics.bucketBy
import com.example.mytracker.core.metrics.toEpochDayRange
import com.example.mytracker.fitness.strength.MovementDirection
import com.example.mytracker.fitness.strength.MuscleGroup
import com.example.mytracker.fitness.strength.label
import com.example.mytracker.fitness.strength.StrengthExercise
import com.example.mytracker.fitness.strength.StrengthExerciseRepository
import com.example.mytracker.fitness.strength.StrengthLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class DetailMode { SETS, VOLUME }

private data class AnalyseSelection(
    val dateRange: AnalyseDateRange = AnalyseDateRange.LAST_30,
    val granularity: Granularity = Granularity.DAILY,
    val selectedMetricIds: List<String> = listOf("cardio.duration_minutes", "strength.volume_kg"),
    val exerciseDetailExerciseId: String? = null,
    val exerciseDetailMode: DetailMode = DetailMode.VOLUME,
    val muscleGroupDetailMuscleGroupId: String? = null,
    val muscleGroupDetailMode: DetailMode = DetailMode.VOLUME,
    val movementDirectionDetail: MovementDirection? = null,
    val movementDirectionDetailMode: DetailMode = DetailMode.VOLUME,
)

data class AnalyseUiState(
    val dateRange: AnalyseDateRange = AnalyseDateRange.LAST_30,
    val granularity: Granularity = Granularity.DAILY,
    val selectedMetricIds: List<String> = emptyList(),
    val primarySeries: ChartSeries? = null,
    val secondarySeries: ChartSeries? = null,
    val exerciseDetailExerciseId: String? = null,
    val exerciseDetailMode: DetailMode = DetailMode.VOLUME,
    val exerciseDetailSeries: ChartSeries? = null,
    val muscleGroupDetailMuscleGroupId: String? = null,
    val muscleGroupDetailMode: DetailMode = DetailMode.VOLUME,
    val muscleGroupDetailSeries: ChartSeries? = null,
    val movementDirectionDetail: MovementDirection? = null,
    val movementDirectionDetailMode: DetailMode = DetailMode.VOLUME,
    val movementDirectionDetailSeries: ChartSeries? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyseViewModel @Inject constructor(
    metricSeriesProviders: Set<@JvmSuppressWildcards MetricSeriesProvider>,
    private val strengthExerciseRepository: StrengthExerciseRepository,
    private val strengthLogRepository: StrengthLogRepository,
) : ViewModel() {
    private val providers = metricSeriesProviders.toList()

    val availableMetrics: List<MetricSeriesDescriptor> = providers.map { it.descriptor() }.sortedBy { it.category }

    val exercises: StateFlow<List<StrengthExercise>> = strengthExerciseRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val muscleGroups: StateFlow<List<MuscleGroup>> = strengthExerciseRepository.observeMuscleGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selection = MutableStateFlow(AnalyseSelection())

    val uiState: StateFlow<AnalyseUiState> = _selection
        .flatMapLatest { selection ->
            val range = selection.dateRange.toEpochDayRange()

            val primaryProvider = providers.find { it.descriptor().id == selection.selectedMetricIds.getOrNull(0) }
            val secondaryProvider = providers.find { it.descriptor().id == selection.selectedMetricIds.getOrNull(1) }
            val primaryFlow: Flow<List<MetricPoint>> = primaryProvider?.getSeries(range) ?: flowOf(emptyList())
            val secondaryFlow: Flow<List<MetricPoint>> = secondaryProvider?.getSeries(range) ?: flowOf(emptyList())

            val exerciseDetailFlow: Flow<List<MetricPoint>> = selection.exerciseDetailExerciseId?.let { exerciseId ->
                when (selection.exerciseDetailMode) {
                    DetailMode.VOLUME -> strengthLogRepository
                        .observeDailyVolumeTotalsForExercise(exerciseId, range.startInclusive, range.endInclusive)
                        .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
                    DetailMode.SETS -> strengthLogRepository
                        .observeDailySetsTotalsForExercise(exerciseId, range.startInclusive, range.endInclusive)
                        .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
                }
            } ?: flowOf(emptyList())

            val muscleGroupDetailFlow: Flow<List<MetricPoint>> = selection.muscleGroupDetailMuscleGroupId?.let { groupId ->
                when (selection.muscleGroupDetailMode) {
                    DetailMode.VOLUME -> strengthLogRepository
                        .observeDailyVolumeTotalsForMuscleGroup(groupId, range.startInclusive, range.endInclusive)
                        .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
                    DetailMode.SETS -> strengthLogRepository
                        .observeDailySetsTotalsForMuscleGroup(groupId, range.startInclusive, range.endInclusive)
                        .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
                }
            } ?: flowOf(emptyList())

            val movementDirectionDetailFlow: Flow<List<MetricPoint>> = selection.movementDirectionDetail?.let { direction ->
                when (selection.movementDirectionDetailMode) {
                    DetailMode.VOLUME -> strengthLogRepository
                        .observeDailyVolumeTotalsForMovementDirection(direction, range.startInclusive, range.endInclusive)
                        .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
                    DetailMode.SETS -> strengthLogRepository
                        .observeDailySetsTotalsForMovementDirection(direction, range.startInclusive, range.endInclusive)
                        .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
                }
            } ?: flowOf(emptyList())

            combine(
                primaryFlow,
                secondaryFlow,
                exerciseDetailFlow,
                muscleGroupDetailFlow,
                movementDirectionDetailFlow,
            ) { primaryPoints, secondaryPoints, exercisePoints, muscleGroupPoints, movementDirectionPoints ->
                val exerciseName = exercises.value.find { it.id == selection.exerciseDetailExerciseId }?.name
                val muscleGroupName = muscleGroups.value.find { it.id == selection.muscleGroupDetailMuscleGroupId }?.name

                AnalyseUiState(
                    dateRange = selection.dateRange,
                    granularity = selection.granularity,
                    selectedMetricIds = selection.selectedMetricIds,
                    primarySeries = primaryProvider?.descriptor()?.let { descriptor ->
                        ChartSeries(
                            points = primaryPoints.bucketBy(selection.granularity, descriptor.aggregation),
                            label = descriptor.displayName,
                            unit = descriptor.unit,
                            color = Color.Unspecified,
                        )
                    },
                    secondarySeries = secondaryProvider?.descriptor()?.let { descriptor ->
                        ChartSeries(
                            points = secondaryPoints.bucketBy(selection.granularity, descriptor.aggregation),
                            label = descriptor.displayName,
                            unit = descriptor.unit,
                            color = Color.Unspecified,
                        )
                    },
                    exerciseDetailExerciseId = selection.exerciseDetailExerciseId,
                    exerciseDetailMode = selection.exerciseDetailMode,
                    exerciseDetailSeries = exerciseName?.let { name ->
                        ChartSeries(
                            points = exercisePoints.bucketBy(selection.granularity, MetricAggregation.SUM),
                            label = name,
                            unit = if (selection.exerciseDetailMode == DetailMode.VOLUME) "kg" else "Sätze",
                            color = Color.Unspecified,
                        )
                    },
                    muscleGroupDetailMuscleGroupId = selection.muscleGroupDetailMuscleGroupId,
                    muscleGroupDetailMode = selection.muscleGroupDetailMode,
                    muscleGroupDetailSeries = muscleGroupName?.let { name ->
                        ChartSeries(
                            points = muscleGroupPoints.bucketBy(selection.granularity, MetricAggregation.SUM),
                            label = name,
                            unit = if (selection.muscleGroupDetailMode == DetailMode.VOLUME) "kg" else "Sätze",
                            color = Color.Unspecified,
                        )
                    },
                    movementDirectionDetail = selection.movementDirectionDetail,
                    movementDirectionDetailMode = selection.movementDirectionDetailMode,
                    movementDirectionDetailSeries = selection.movementDirectionDetail?.let { direction ->
                        ChartSeries(
                            points = movementDirectionPoints.bucketBy(selection.granularity, MetricAggregation.SUM),
                            label = direction.label(),
                            unit = if (selection.movementDirectionDetailMode == DetailMode.VOLUME) "kg" else "Sätze",
                            color = Color.Unspecified,
                        )
                    },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyseUiState())

    fun onDateRangeChange(value: AnalyseDateRange) {
        _selection.value = _selection.value.copy(dateRange = value)
    }

    fun onGranularityChange(value: Granularity) {
        _selection.value = _selection.value.copy(granularity = value)
    }

    fun onMetricToggle(id: String) {
        val current = _selection.value.selectedMetricIds
        val updated = when {
            id in current -> current - id
            current.size >= 2 -> current.drop(1) + id
            else -> current + id
        }
        _selection.value = _selection.value.copy(selectedMetricIds = updated)
    }

    fun onExerciseDetailChange(exercise: StrengthExercise) {
        _selection.value = _selection.value.copy(exerciseDetailExerciseId = exercise.id)
    }

    fun onExerciseDetailModeChange(mode: DetailMode) {
        _selection.value = _selection.value.copy(exerciseDetailMode = mode)
    }

    fun onMuscleGroupDetailChange(group: MuscleGroup) {
        _selection.value = _selection.value.copy(muscleGroupDetailMuscleGroupId = group.id)
    }

    fun onMuscleGroupDetailModeChange(mode: DetailMode) {
        _selection.value = _selection.value.copy(muscleGroupDetailMode = mode)
    }

    fun onMovementDirectionDetailChange(direction: MovementDirection) {
        _selection.value = _selection.value.copy(movementDirectionDetail = direction)
    }

    fun onMovementDirectionDetailModeChange(mode: DetailMode) {
        _selection.value = _selection.value.copy(movementDirectionDetailMode = mode)
    }
}
