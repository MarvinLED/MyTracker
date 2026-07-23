package com.example.prokject2_tracker.fitness.strength

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StrengthExerciseEditState(
    val id: String? = null,
    val name: String = "",
    val muscleGroup: MuscleGroup = MuscleGroup.FULL_BODY,
    val isSaved: Boolean = false,
) {
    val isValid: Boolean get() = name.isNotBlank()
}

@HiltViewModel
class StrengthExerciseEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val strengthExerciseRepository: StrengthExerciseRepository,
) : ViewModel() {
    private val route: StrengthExerciseEditRoute = savedStateHandle.toRoute()
    private var existing: StrengthExercise? = null

    private val _state = MutableStateFlow(StrengthExerciseEditState(id = route.exerciseId))
    val state: StateFlow<StrengthExerciseEditState> = _state.asStateFlow()

    init {
        val exerciseId = route.exerciseId
        if (exerciseId != null) {
            viewModelScope.launch {
                strengthExerciseRepository.getById(exerciseId)?.let { exercise ->
                    existing = exercise
                    _state.value = StrengthExerciseEditState(
                        id = exercise.id,
                        name = exercise.name,
                        muscleGroup = exercise.muscleGroup,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value) }
    fun onMuscleGroupChange(value: MuscleGroup) { _state.value = _state.value.copy(muscleGroup = value) }

    fun save() {
        val s = _state.value
        if (!s.isValid) return
        viewModelScope.launch {
            val current = existing
            if (current == null) {
                strengthExerciseRepository.create(name = s.name, muscleGroup = s.muscleGroup)
            } else {
                strengthExerciseRepository.update(
                    current,
                    current.copy(name = s.name, muscleGroup = s.muscleGroup),
                )
            }
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}
