package com.example.prokject2_tracker.fitness.strength

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StrengthLogEditState(
    val id: String? = null,
    val epochDay: Long = DateUtils.todayEpochDay(),
    val exerciseId: String? = null,
    val exerciseName: String = "",
    val sets: String = "",
    val reps: String = "",
    val weightKg: String = "",
    val note: String = "",
    val isSaved: Boolean = false,
) {
    val isValid: Boolean
        get() = exerciseId != null &&
            sets.toIntOrNull()?.let { it > 0 } == true &&
            reps.toIntOrNull()?.let { it > 0 } == true &&
            weightKg.toDoubleOrNull()?.let { it >= 0.0 } == true
}

@HiltViewModel
class StrengthLogEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val strengthLogRepository: StrengthLogRepository,
    strengthExerciseRepository: StrengthExerciseRepository,
) : ViewModel() {
    private val route: StrengthLogEditRoute = savedStateHandle.toRoute()
    private var existing: StrengthLogEntry? = null

    private val _state = MutableStateFlow(StrengthLogEditState(id = route.entryId))
    val state: StateFlow<StrengthLogEditState> = _state.asStateFlow()

    val exercises: StateFlow<List<StrengthExercise>> = strengthExerciseRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val entryId = route.entryId
        if (entryId != null) {
            viewModelScope.launch {
                strengthLogRepository.getById(entryId)?.let { entry ->
                    existing = entry
                    _state.value = StrengthLogEditState(
                        id = entry.id,
                        epochDay = entry.epochDay,
                        exerciseId = entry.exerciseId,
                        exerciseName = entry.exerciseName,
                        sets = entry.sets.toString(),
                        reps = entry.reps.toString(),
                        weightKg = entry.weightKg.toString(),
                        note = entry.note.orEmpty(),
                    )
                }
            }
        }
    }

    fun onEpochDayChange(value: Long) { _state.value = _state.value.copy(epochDay = value) }

    fun onExerciseChange(exercise: StrengthExercise) {
        _state.value = _state.value.copy(exerciseId = exercise.id, exerciseName = exercise.name)
    }

    fun onSetsChange(value: String) { _state.value = _state.value.copy(sets = value) }
    fun onRepsChange(value: String) { _state.value = _state.value.copy(reps = value) }
    fun onWeightChange(value: String) { _state.value = _state.value.copy(weightKg = value) }
    fun onNoteChange(value: String) { _state.value = _state.value.copy(note = value) }

    fun save() {
        val s = _state.value
        val exerciseId = s.exerciseId
        if (!s.isValid || exerciseId == null) return
        viewModelScope.launch {
            strengthLogRepository.save(
                existing = existing,
                epochDay = s.epochDay,
                exerciseId = exerciseId,
                exerciseName = s.exerciseName,
                sets = s.sets.toInt(),
                reps = s.reps.toInt(),
                weightKg = s.weightKg.toDouble(),
                note = s.note.ifBlank { null },
            )
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}
