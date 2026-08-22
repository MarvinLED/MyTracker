package com.example.mytracker.fitness.strength

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StrengthExerciseListViewModel @Inject constructor(
    private val strengthExerciseRepository: StrengthExerciseRepository,
) : ViewModel() {
    val exercises: StateFlow<List<StrengthExerciseWithMuscleGroups>> = strengthExerciseRepository.observeAllWithMuscleGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { strengthExerciseRepository.ensureDefaultMuscleGroupsSeeded() }
    }

    fun deleteIfUnused(exercise: StrengthExercise, onBlocked: () -> Unit) {
        viewModelScope.launch {
            if (strengthExerciseRepository.canDelete(exercise.id)) {
                strengthExerciseRepository.delete(exercise)
            } else {
                onBlocked()
            }
        }
    }
}
