package com.example.prokject2_tracker.analyse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.metrics.EpochDayRange
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.metrics.MetricSeriesDescriptor
import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import com.example.prokject2_tracker.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SeriesUiState(
    val descriptor: MetricSeriesDescriptor,
    val points: List<MetricPoint>,
)

private const val DAYS_SHOWN = 30

@HiltViewModel
class AnalyseViewModel @Inject constructor(
    metricSeriesProviders: Set<@JvmSuppressWildcards MetricSeriesProvider>,
) : ViewModel() {
    val series: StateFlow<List<SeriesUiState>> = run {
        val today = DateUtils.todayEpochDay()
        val range = EpochDayRange(startInclusive = today - (DAYS_SHOWN - 1), endInclusive = today)
        val providers = metricSeriesProviders.toList()
        if (providers.isEmpty()) {
            MutableStateFlow(emptyList())
        } else {
            combine(providers.map { it.getSeries(range) }) { pointsArray ->
                providers.indices.map { index ->
                    SeriesUiState(descriptor = providers[index].descriptor(), points = pointsArray[index])
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }
}
