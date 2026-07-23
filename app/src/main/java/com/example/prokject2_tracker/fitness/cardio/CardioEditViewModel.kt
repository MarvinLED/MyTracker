package com.example.prokject2_tracker.fitness.cardio

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.prokject2_tracker.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CardioEditState(
    val id: String? = null,
    val epochDay: Long = DateUtils.todayEpochDay(),
    val activityType: CardioActivityType = CardioActivityType.RUNNING,
    val durationMinutes: String = "",
    val distanceKm: String = "",
    val caloriesBurned: String = "",
    val note: String = "",
    val isSaved: Boolean = false,
) {
    val isValid: Boolean
        get() = durationMinutes.toDoubleOrNull()?.let { it > 0.0 } == true &&
            caloriesBurned.toDoubleOrNull()?.let { it >= 0.0 } == true &&
            (distanceKm.isBlank() || distanceKm.toDoubleOrNull()?.let { it >= 0.0 } == true)
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

    init {
        val sessionId = route.sessionId
        if (sessionId != null) {
            viewModelScope.launch {
                cardioRepository.getById(sessionId)?.let { session ->
                    existing = session
                    _state.value = CardioEditState(
                        id = session.id,
                        epochDay = session.epochDay,
                        activityType = session.activityType,
                        durationMinutes = session.durationMinutes.toString(),
                        distanceKm = session.distanceKm?.toString().orEmpty(),
                        caloriesBurned = session.caloriesBurned.toString(),
                        note = session.note.orEmpty(),
                    )
                }
            }
        }
    }

    fun onEpochDayChange(value: Long) { _state.value = _state.value.copy(epochDay = value) }
    fun onActivityTypeChange(value: CardioActivityType) { _state.value = _state.value.copy(activityType = value) }
    fun onDurationChange(value: String) { _state.value = _state.value.copy(durationMinutes = value) }
    fun onDistanceChange(value: String) { _state.value = _state.value.copy(distanceKm = value) }
    fun onCaloriesChange(value: String) { _state.value = _state.value.copy(caloriesBurned = value) }
    fun onNoteChange(value: String) { _state.value = _state.value.copy(note = value) }

    fun save() {
        val s = _state.value
        if (!s.isValid) return
        viewModelScope.launch {
            cardioRepository.save(
                existing = existing,
                epochDay = s.epochDay,
                activityType = s.activityType,
                durationMinutes = s.durationMinutes.toDouble(),
                distanceKm = s.distanceKm.toDoubleOrNull(),
                caloriesBurned = s.caloriesBurned.toDouble(),
                note = s.note.ifBlank { null },
            )
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}
