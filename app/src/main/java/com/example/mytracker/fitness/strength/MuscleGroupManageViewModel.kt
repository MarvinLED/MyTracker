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
class MuscleGroupManageViewModel @Inject constructor(
    private val strengthExerciseRepository: StrengthExerciseRepository,
) : ViewModel() {
    val groups: StateFlow<List<MuscleGroup>> = strengthExerciseRepository.observeMuscleGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun create(name: String) {
        viewModelScope.launch { strengthExerciseRepository.createMuscleGroup(name) }
    }

    fun update(group: MuscleGroup, name: String) {
        viewModelScope.launch { strengthExerciseRepository.updateMuscleGroup(group, name) }
    }

    fun deleteIfUnused(group: MuscleGroup, onBlocked: () -> Unit) {
        viewModelScope.launch {
            if (strengthExerciseRepository.canDeleteMuscleGroup(group.id)) {
                strengthExerciseRepository.deleteMuscleGroup(group)
            } else {
                onBlocked()
            }
        }
    }
}
