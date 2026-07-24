package com.example.prokject2_tracker.fitness.cardio

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.prokject2_tracker.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CardioEditState(
    val id: String? = null,
    val epochDay: Long = DateUtils.todayEpochDay(),
    val activityTypeId: String? = null,
    val activityTypeName: String = "",
    val durationMinutes: String = "",
    val distanceKm: String = "",
    val caloriesBurned: String = "",
    val avgHeartRateBpm: String = "",
    val note: String = "",
    val isSaved: Boolean = false,
) {
    val isValid: Boolean
        get() = activityTypeId != null &&
            durationMinutes.toDoubleOrNull()?.let { it > 0.0 } == true &&
            (distanceKm.isBlank() || distanceKm.toDoubleOrNull()?.let { it >= 0.0 } == true) &&
            (caloriesBurned.isBlank() || caloriesBurned.toDoubleOrNull()?.let { it >= 0.0 } == true) &&
            (avgHeartRateBpm.isBlank() || avgHeartRateBpm.toIntOrNull()?.let { it > 0 } == true)
}

@HiltViewModel
class CardioEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardioRepository: CardioRepository,
) : ViewModel() {
    private val route: CardioEditRoute = savedStateHandle.toRoute()
    private var existing: CardioSession? = null

    private val _state = MutableStateFlow(CardioEditState(id = route.sessionId))
    val state: StateFlow<CardioEditState> = _state.asStateFlow()

    val activityTypes: StateFlow<List<CardioActivityType>> = cardioRepository.observeActivityTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val sessionId = route.sessionId
        if (sessionId != null) {
            viewModelScope.launch {
                cardioRepository.getById(sessionId)?.let { session ->
                    existing = session
                    _state.value = CardioEditState(
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
            }
        } else {
            viewModelScope.launch {
                val firstType = cardioRepository.observeActivityTypes().first().firstOrNull()
                if (firstType != null && _state.value.activityTypeId == null) {
                    _state.value = _state.value.copy(
                        activityTypeId = firstType.id,
                        activityTypeName = firstType.name,
                    )
                }
            }
        }
    }

    fun onEpochDayChange(value: Long) { _state.value = _state.value.copy(epochDay = value) }
    fun onActivityTypeChange(value: CardioActivityType) {
        _state.value = _state.value.copy(activityTypeId = value.id, activityTypeName = value.name)
    }
    fun onDurationChange(value: String) { _state.value = _state.value.copy(durationMinutes = value) }
    fun onDistanceChange(value: String) { _state.value = _state.value.copy(distanceKm = value) }
    fun onCaloriesChange(value: String) { _state.value = _state.value.copy(caloriesBurned = value) }
    fun onHeartRateChange(value: String) { _state.value = _state.value.copy(avgHeartRateBpm = value) }
    fun onNoteChange(value: String) { _state.value = _state.value.copy(note = value) }

    fun save() {
        val s = _state.value
        val activityTypeId = s.activityTypeId
        if (!s.isValid || activityTypeId == null) return
        viewModelScope.launch {
            cardioRepository.save(
                existing = existing,
                epochDay = s.epochDay,
                activityTypeId = activityTypeId,
                activityTypeName = s.activityTypeName,
                durationMinutes = s.durationMinutes.toDouble(),
                distanceKm = s.distanceKm.toDoubleOrNull(),
                caloriesBurned = s.caloriesBurned.toDoubleOrNull(),
                avgHeartRateBpm = s.avgHeartRateBpm.toIntOrNull(),
                note = s.note.ifBlank { null },
            )
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}
