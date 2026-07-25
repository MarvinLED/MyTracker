package com.example.prokject2_tracker.fluid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.datastore.UserPreferencesRepository
import com.example.prokject2_tracker.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FluidDayUiState(
    val epochDay: Long,
    val entries: List<FluidEntry> = emptyList(),
    val totalMl: Double = 0.0,
    val goalMl: Double = 2000.0,
)

@HiltViewModel
class FluidViewModel @Inject constructor(
    private val fluidRepository: FluidRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val _selectedEpochDay = MutableStateFlow(DateUtils.todayEpochDay())
    val selectedEpochDay: StateFlow<Long> = _selectedEpochDay.asStateFlow()

    val types: StateFlow<List<FluidType>> = fluidRepository.observeTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val units: StateFlow<List<FluidUnit>> = fluidRepository.observeUnits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { fluidRepository.ensureDefaultTypesSeeded() }
        viewModelScope.launch { fluidRepository.ensureDefaultUnitsSeeded() }
    }

    val uiState: StateFlow<FluidDayUiState> = _selectedEpochDay
        .flatMapLatest { epochDay ->
            combine(
                fluidRepository.observeForDay(epochDay),
                fluidRepository.observeDayTotalMl(epochDay),
                userPreferencesRepository.userPreferences,
            ) { entries, total, prefs ->
                FluidDayUiState(
                    epochDay = epochDay,
                    entries = entries,
                    totalMl = total,
                    goalMl = prefs.dailyWaterGoalMl,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FluidDayUiState(_selectedEpochDay.value))

    fun goToPreviousDay() {
        _selectedEpochDay.value -= 1
    }

    fun goToNextDay() {
        _selectedEpochDay.value += 1
    }

    fun quickAdd(type: FluidType, amountMl: Double) {
        viewModelScope.launch { fluidRepository.logFluid(_selectedEpochDay.value, type, amountMl) }
    }

    fun addWithUnit(type: FluidType, unit: FluidUnit) {
        viewModelScope.launch { fluidRepository.logFluid(_selectedEpochDay.value, type, unit.amountMl, unit) }
    }

    fun updateAmount(entry: FluidEntry, amountMl: Double) {
        viewModelScope.launch { fluidRepository.updateEntryAmount(entry, amountMl) }
    }

    fun delete(entry: FluidEntry) {
        viewModelScope.launch { fluidRepository.delete(entry) }
    }
}
