package com.example.prokject2_tracker.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BodySiteManageViewModel @Inject constructor(
    private val measurementRepository: MeasurementRepository,
) : ViewModel() {
    val sites: StateFlow<List<BodySite>> = measurementRepository.observeSites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun create(name: String, measuringHint: String?) {
        viewModelScope.launch { measurementRepository.createSite(name, measuringHint) }
    }

    fun update(site: BodySite, name: String, measuringHint: String?) {
        viewModelScope.launch { measurementRepository.updateSite(site, name, measuringHint) }
    }

    /**
     * Unlike a Getränkeart, a used site isn't delete-blocked: its measurements belong to it and
     * nothing else refers to them, so there'd be no way to ever remove it. Instead [onConfirmNeeded]
     * reports how much history the delete would take along, and the screen asks first.
     */
    fun requestDelete(site: BodySite, onConfirmNeeded: (Int) -> Unit) {
        viewModelScope.launch {
            val count = measurementRepository.measurementCount(site.id)
            if (count == 0) {
                measurementRepository.deleteSite(site)
            } else {
                onConfirmNeeded(count)
            }
        }
    }

    fun delete(site: BodySite) {
        viewModelScope.launch { measurementRepository.deleteSite(site) }
    }
}
