package com.example.prokject2_tracker.fitness.strength

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StrengthExerciseEditState(
    val id: String? = null,
    val name: String = "",
    val muscleGroupIds: Set<String> = emptySet(),
    val isSaved: Boolean = false,
) {
    val isValid: Boolean get() = name.isNotBlank() && muscleGroupIds.isNotEmpty()
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

    val muscleGroups: StateFlow<List<MuscleGroup>> = strengthExerciseRepository.observeMuscleGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val exerciseId = route.exerciseId
        if (exerciseId != null) {
            viewModelScope.launch {
                strengthExerciseRepository.getById(exerciseId)?.let { exercise ->
                    existing = exercise
                    val muscleGroupIds = strengthExerciseRepository.getMuscleGroupsForExerciseOnce(exercise.id)
                        .map { it.id }.toSet()
                    _state.value = StrengthExerciseEditState(
                        id = exercise.id,
                        name = exercise.name,
                        muscleGroupIds = muscleGroupIds,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value) }
    fun onMuscleGroupToggle(group: MuscleGroup) {
        val current = _state.value.muscleGroupIds
        val updated = if (group.id in current) current - group.id else current + group.id
        _state.value = _state.value.copy(muscleGroupIds = updated)
    }

    fun save() {
        val s = _state.value
        if (!s.isValid) return
        viewModelScope.launch {
            val current = existing
            if (current == null) {
                strengthExerciseRepository.create(name = s.name, muscleGroupIds = s.muscleGroupIds.toList())
            } else {
                strengthExerciseRepository.update(current, name = s.name, muscleGroupIds = s.muscleGroupIds.toList())
            }
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}
