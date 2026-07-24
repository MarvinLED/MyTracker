package com.example.prokject2_tracker.fitness

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.prokject2_tracker.fitness.cardio.CardioActivityType
import com.example.prokject2_tracker.fitness.cardio.CardioEditState
import com.example.prokject2_tracker.fitness.cardio.CardioRepository
import com.example.prokject2_tracker.fitness.cardio.CardioSession
import com.example.prokject2_tracker.fitness.strength.StrengthEntryFormState
import com.example.prokject2_tracker.fitness.strength.StrengthExercise
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseRepository
import com.example.prokject2_tracker.fitness.strength.StrengthLogRepository
import com.example.prokject2_tracker.fitness.strength.StrengthSetInput
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TrainingType { CARDIO, STRENGTH }

data class TrainingEntryUiState(
    val showTypeChooser: Boolean = true,
    val selectedType: TrainingType = TrainingType.CARDIO,
    val cardioState: CardioEditState = CardioEditState(),
    val strengthState: StrengthEntryFormState = StrengthEntryFormState(),
    val isSaved: Boolean = false,
)

@HiltViewModel
class TrainingEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardioRepository: CardioRepository,
    private val strengthLogRepository: StrengthLogRepository,
    private val strengthExerciseRepository: StrengthExerciseRepository,
) : ViewModel() {
    private val route: TrainingEntryRoute = savedStateHandle.toRoute()
    private var existingCardioSession: CardioSession? = null

    private val _state = MutableStateFlow(TrainingEntryUiState())
    val state: StateFlow<TrainingEntryUiState> = _state.asStateFlow()

    val activityTypes: StateFlow<List<CardioActivityType>> = cardioRepository.observeActivityTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exercises: StateFlow<List<StrengthExercise>> = strengthExerciseRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val cardioSessionId = route.cardioSessionId
        val strengthLogEntryId = route.strengthLogEntryId
        when {
            cardioSessionId != null -> viewModelScope.launch {
                cardioRepository.getById(cardioSessionId)?.let { session ->
                    existingCardioSession = session
                    _state.value = _state.value.copy(
                        showTypeChooser = false,
                        selectedType = TrainingType.CARDIO,
                        cardioState = CardioEditState(
                            id = session.id,
                            epochDay = session.epochDay,
                            activityTypeId = session.activityTypeId,
                            activityTypeName = session.activityTypeName,
                            durationMinutes = session.durationMinutes.toString(),
                            distanceKm = session.distanceKm?.toString().orEmpty(),
                            caloriesBurned = session.caloriesBurned?.toString().orEmpty(),
                            avgHeartRateBpm = session.avgHeartRateBpm?.toString().orEmpty(),
                            note = session.note.orEmpty(),
                        ),
                    )
                }
            }
            strengthLogEntryId != null -> viewModelScope.launch {
                val entry = strengthLogRepository.getById(strengthLogEntryId)
                if (entry != null) {
                    val sets = strengthLogRepository.getSetsForEntry(strengthLogEntryId)
                    _state.value = _state.value.copy(
                        showTypeChooser = false,
                        selectedType = TrainingType.STRENGTH,
                        strengthState = StrengthEntryFormState(
                            epochDay = entry.epochDay,
                            exerciseId = entry.exerciseId,
                            exerciseName = entry.exerciseName,
                            sets = sets.map { set ->
                                StrengthSetInput(
                                    reps = set.reps.toString(),
                                    weightText = set.weightKg?.toString().orEmpty(),
                                    weightManuallyEdited = true,
                                )
                            }.ifEmpty { listOf(StrengthSetInput()) },
                            note = entry.note.orEmpty(),
                        ),
                    )
                }
            }
            else -> viewModelScope.launch {
                val firstType = cardioRepository.observeActivityTypes().first().firstOrNull()
                if (firstType != null && _state.value.cardioState.activityTypeId == null) {
                    _state.value = _state.value.copy(
                        cardioState = _state.value.cardioState.copy(
                            activityTypeId = firstType.id,
                            activityTypeName = firstType.name,
                        ),
                    )
                }
            }
        }
    }

    fun onTypeSelected(type: TrainingType) {
        if (!_state.value.showTypeChooser) return
        _state.value = _state.value.copy(selectedType = type)
    }

    fun onCardioEpochDayChange(value: Long) {
        _state.value = _state.value.copy(cardioState = _state.value.cardioState.copy(epochDay = value))
    }

    fun onCardioActivityTypeChange(value: CardioActivityType) {
        _state.value = _state.value.copy(
            cardioState = _state.value.cardioState.copy(activityTypeId = value.id, activityTypeName = value.name),
        )
    }

    fun onCardioDurationChange(value: String) {
        _state.value = _state.value.copy(cardioState = _state.value.cardioState.copy(durationMinutes = value))
    }

    fun onCardioDistanceChange(value: String) {
        _state.value = _state.value.copy(cardioState = _state.value.cardioState.copy(distanceKm = value))
    }

    fun onCardioCaloriesChange(value: String) {
        _state.value = _state.value.copy(cardioState = _state.value.cardioState.copy(caloriesBurned = value))
    }

    fun onCardioHeartRateChange(value: String) {
        _state.value = _state.value.copy(cardioState = _state.value.cardioState.copy(avgHeartRateBpm = value))
    }

    fun onCardioNoteChange(value: String) {
        _state.value = _state.value.copy(cardioState = _state.value.cardioState.copy(note = value))
    }

    fun onStrengthEpochDayChange(value: Long) {
        _state.value = _state.value.copy(strengthState = _state.value.strengthState.copy(epochDay = value))
    }

    fun onStrengthExerciseChange(exercise: StrengthExercise) {
        _state.value = _state.value.copy(
            strengthState = _state.value.strengthState.copy(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                sets = listOf(StrengthSetInput()),
            ),
        )
        viewModelScope.launch {
            val mostRecent = strengthLogRepository.getMostRecentSetForExercise(exercise.id)
            if (mostRecent != null) {
                val current = _state.value.strengthState
                if (current.exerciseId == exercise.id && current.sets.size == 1 && current.sets[0].reps.isBlank()) {
                    _state.value = _state.value.copy(
                        strengthState = current.copy(
                            sets = listOf(current.sets[0].copy(weightText = mostRecent.weightKg?.toString().orEmpty())),
                        ),
                    )
                }
            }
        }
    }

    fun onStrengthSetRepsChange(index: Int, value: String) {
        val sets = _state.value.strengthState.sets.toMutableList()
        if (index !in sets.indices) return
        sets[index] = sets[index].copy(reps = value)
        _state.value = _state.value.copy(strengthState = _state.value.strengthState.copy(sets = sets))
    }

    fun onStrengthSetWeightChange(index: Int, value: String) {
        val sets = _state.value.strengthState.sets.toMutableList()
        if (index !in sets.indices) return
        sets[index] = sets[index].copy(weightText = value, weightManuallyEdited = true)
        if (index == 0) {
            for (i in 1 until sets.size) {
                if (!sets[i].weightManuallyEdited) {
                    sets[i] = sets[i].copy(weightText = value)
                }
            }
        }
        _state.value = _state.value.copy(strengthState = _state.value.strengthState.copy(sets = sets))
    }

    fun onAddStrengthSet() {
        val current = _state.value.strengthState
        val firstWeight = current.sets.firstOrNull()?.weightText.orEmpty()
        val newSet = if (firstWeight.isNotBlank()) {
            StrengthSetInput(weightText = firstWeight, weightManuallyEdited = false)
        } else {
            StrengthSetInput()
        }
        _state.value = _state.value.copy(strengthState = current.copy(sets = current.sets + newSet))
    }

    fun onRemoveStrengthSet(index: Int) {
        val current = _state.value.strengthState
        if (current.sets.size <= 1 || index !in current.sets.indices) return
        _state.value = _state.value.copy(
            strengthState = current.copy(sets = current.sets.filterIndexed { i, _ -> i != index }),
        )
    }

    fun onStrengthNoteChange(value: String) {
        _state.value = _state.value.copy(strengthState = _state.value.strengthState.copy(note = value))
    }

    fun save() {
        val s = _state.value
        when (s.selectedType) {
            TrainingType.CARDIO -> {
                val cardio = s.cardioState
                val activityTypeId = cardio.activityTypeId
                if (!cardio.isValid || activityTypeId == null) return
                viewModelScope.launch {
                    cardioRepository.save(
                        existing = existingCardioSession,
                        epochDay = cardio.epochDay,
                        activityTypeId = activityTypeId,
                        activityTypeName = cardio.activityTypeName,
                        durationMinutes = cardio.durationMinutes.toDouble(),
                        distanceKm = cardio.distanceKm.toDoubleOrNull(),
                        caloriesBurned = cardio.caloriesBurned.toDoubleOrNull(),
                        avgHeartRateBpm = cardio.avgHeartRateBpm.toIntOrNull(),
                        note = cardio.note.ifBlank { null },
                    )
                    _state.value = _state.value.copy(isSaved = true)
                }
            }
            TrainingType.STRENGTH -> {
                val strength = s.strengthState
                val exerciseId = strength.exerciseId
                if (!strength.isValid || exerciseId == null) return
                viewModelScope.launch {
                    strengthLogRepository.save(
                        existingId = route.strengthLogEntryId,
                        epochDay = strength.epochDay,
                        exerciseId = exerciseId,
                        exerciseName = strength.exerciseName,
                        note = strength.note.ifBlank { null },
                        sets = strength.sets.map { it.reps.toInt() to it.weightText.toDoubleOrNull() },
                    )
                    _state.value = _state.value.copy(isSaved = true)
                }
            }
        }
    }
}
