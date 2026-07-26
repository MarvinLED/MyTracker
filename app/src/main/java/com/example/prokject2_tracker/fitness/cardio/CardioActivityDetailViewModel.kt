package com.example.prokject2_tracker.fitness.cardio

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.prokject2_tracker.core.metrics.Granularity
import com.example.prokject2_tracker.core.metrics.MetricAggregation
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.metrics.bucketBy
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How many weeks of history the chart shows — matches the strength detail page. */
private const val CHART_WEEKS = 26

data class CardioActivityDetailUiState(
    val activityTypeName: String = "",
    val selectedEpochDay: Long = DateUtils.todayEpochDay(),
    val currentDay: CardioDayStats? = null,
    val previousDay: CardioDayStats? = null,
    /** Current minus previous day's total minutes; null while either side is missing. */
    val minutesDelta: Double? = null,
    /** The day's sessions, for the selector chips. Empty when nothing is logged that day. */
    val sessionsOfDay: List<CardioSession> = emptyList(),
    val editingSessionId: String? = null,
    val form: CardioEditState = CardioEditState(),
    val weeklyMinutes: List<MetricPoint> = emptyList(),
    val weeklyDistance: List<MetricPoint> = emptyList(),
    val weeklyPace: List<MetricPoint> = emptyList(),
    val isSaved: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CardioActivityDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardioRepository: CardioRepository,
) : ViewModel() {
    private val route: CardioActivityDetailRoute = savedStateHandle.toRoute()

    private val _selectedEpochDay = MutableStateFlow(route.epochDay)
    private val _activityTypeName = MutableStateFlow("")
    private val _editingSessionId = MutableStateFlow(route.sessionId)
    private val _form = MutableStateFlow(CardioEditState(epochDay = route.epochDay))
    private val _isSaved = MutableStateFlow(false)

    private val sessions: StateFlow<List<CardioSession>> =
        cardioRepository.observeForActivityType(route.activityTypeId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<CardioActivityDetailUiState> = combine(
        sessions,
        _selectedEpochDay,
        combine(_activityTypeName, _editingSessionId, _isSaved) { name, editingId, saved ->
            Triple(name, editingId, saved)
        },
        _form,
    ) { all, day, (name, editingId, saved), form ->
        val current = all.dayStatsOn(day)
        val previous = all.previousSessionDay(before = day)?.let { all.dayStatsOn(it) }
        val window = all.chartWindow()

        CardioActivityDetailUiState(
            activityTypeName = name,
            selectedEpochDay = day,
            currentDay = current,
            previousDay = previous,
            minutesDelta = if (current != null && previous != null) {
                current.totalMinutes - previous.totalMinutes
            } else {
                null
            },
            sessionsOfDay = current?.sessions.orEmpty(),
            editingSessionId = editingId,
            form = form,
            weeklyMinutes = window.dailyMinutePoints().bucketBy(Granularity.WEEKLY, MetricAggregation.SUM),
            weeklyDistance = window.dailyDistancePoints().bucketBy(Granularity.WEEKLY, MetricAggregation.SUM),
            // Pace can't go through bucketBy: the week's pace is its total minutes over its total
            // distance, which no per-day aggregate preserves enough information to reconstruct.
            weeklyPace = window.weeklyPacePoints(),
            isSaved = saved,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CardioActivityDetailUiState(selectedEpochDay = route.epochDay),
    )

    init {
        viewModelScope.launch {
            _activityTypeName.value = cardioRepository.observeActivityTypes().first()
                .firstOrNull { it.id == route.activityTypeId }?.name.orEmpty()
            // Queried straight from the repository: nothing is collecting `sessions` yet on this
            // first pass, so reading its (empty) initial value would blank the form even when the
            // history screen asked for a specific session.
            val loaded = cardioRepository.observeForActivityType(route.activityTypeId).first()
            val session = route.sessionId?.let { id -> loaded.firstOrNull { it.id == id } }
                ?: loaded.filter { it.epochDay == route.epochDay }.maxByOrNull { it.createdAt }
            _editingSessionId.value = session?.id
            _form.value = formFor(session, route.epochDay)
        }
    }

    // --- date + session selection ----------------------------------------------------------

    fun goToPreviousDay() = selectDay(_selectedEpochDay.value - 1)
    fun goToNextDay() = selectDay(_selectedEpochDay.value + 1)

    fun selectDay(epochDay: Long) {
        if (epochDay == _selectedEpochDay.value) return
        _selectedEpochDay.value = epochDay
        val newest = sessions.value.filter { it.epochDay == epochDay }.maxByOrNull { it.createdAt }
        _editingSessionId.value = newest?.id
        loadIntoForm(newest?.id)
    }

    fun selectSession(sessionId: String?) {
        _editingSessionId.value = sessionId
        loadIntoForm(sessionId)
    }

    /** Null [sessionId] starts a blank session on the selected day. */
    private fun loadIntoForm(sessionId: String?) {
        val session = sessionId?.let { id -> sessions.value.firstOrNull { it.id == id } }
        _form.value = formFor(session, _selectedEpochDay.value)
    }

    /** A null [session] yields a blank form for [day] — the "+ Neu" case. */
    private fun formFor(session: CardioSession?, day: Long): CardioEditState =
        if (session == null) {
            CardioEditState(
                epochDay = day,
                activityTypeId = route.activityTypeId,
                activityTypeName = _activityTypeName.value,
            )
        } else {
            CardioEditState(
                id = session.id,
                epochDay = session.epochDay,
                activityTypeId = session.activityTypeId,
                activityTypeName = session.activityTypeName,
                durationMinutes = session.durationMinutes.toString(),
                distanceKm = session.distanceKm?.toString().orEmpty(),
                caloriesBurned = session.caloriesBurned?.toString().orEmpty(),
                avgHeartRateBpm = session.avgHeartRateBpm?.toString().orEmpty(),
                note = session.note.orEmpty(),
            )
        }

    // --- form ------------------------------------------------------------------------------

    fun onDurationChange(value: String) = updateForm { it.copy(durationMinutes = value) }
    fun onDistanceChange(value: String) = updateForm { it.copy(distanceKm = value) }
    fun onCaloriesChange(value: String) = updateForm { it.copy(caloriesBurned = value) }
    fun onHeartRateChange(value: String) = updateForm { it.copy(avgHeartRateBpm = value) }
    fun onNoteChange(value: String) = updateForm { it.copy(note = value) }

    private fun updateForm(transform: (CardioEditState) -> CardioEditState) {
        _form.value = transform(_form.value)
    }

    /**
     * Cardio keeps an explicit save, unlike the strength page: these are free numeric fields with a
     * soft keyboard, so there is no per-tap gesture to auto-save from, and writing every keystroke
     * would be worse than a button.
     */
    fun save() {
        val form = _form.value
        if (!form.isValid) return
        val existing = form.id?.let { id -> sessions.value.firstOrNull { it.id == id } }
        viewModelScope.launch {
            cardioRepository.save(
                existing = existing,
                epochDay = _selectedEpochDay.value,
                activityTypeId = route.activityTypeId,
                activityTypeName = _activityTypeName.value,
                durationMinutes = form.durationMinutes.toLocaleDoubleOrNull()!!,
                distanceKm = form.distanceKm.toLocaleDoubleOrNull(),
                caloriesBurned = form.caloriesBurned.toLocaleDoubleOrNull(),
                avgHeartRateBpm = form.avgHeartRateBpm.toIntOrNull(),
                note = form.note.takeIf { it.isNotBlank() },
            )
            _isSaved.value = true
        }
    }

    fun deleteEditedSession() {
        val id = _editingSessionId.value ?: return
        val session = sessions.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            cardioRepository.delete(session)
            selectSession(null)
        }
    }

    fun consumeSaved() {
        _isSaved.value = false
    }
}

private fun List<CardioSession>.chartWindow(today: Long = DateUtils.todayEpochDay()): List<CardioSession> {
    val firstDay = DateUtils.startOfWeekEpochDay(today) - (CHART_WEEKS - 1) * 7L
    return filter { it.epochDay >= firstDay }
}
