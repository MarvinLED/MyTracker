package com.example.prokject2_tracker.fluid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FluidTypeManageViewModel @Inject constructor(
    private val fluidRepository: FluidRepository,
) : ViewModel() {
    val types: StateFlow<List<FluidType>> = fluidRepository.observeTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun create(name: String, defaultQuickAddMl: Double) {
        viewModelScope.launch { fluidRepository.createType(name, defaultQuickAddMl) }
    }

    fun update(type: FluidType, name: String, defaultQuickAddMl: Double) {
        viewModelScope.launch { fluidRepository.updateType(type, name, defaultQuickAddMl) }
    }

    fun deleteIfUnused(type: FluidType, onBlocked: () -> Unit) {
        viewModelScope.launch {
            if (fluidRepository.canDeleteType(type.id)) {
                fluidRepository.deleteType(type)
            } else {
                onBlocked()
            }
        }
    }
}
