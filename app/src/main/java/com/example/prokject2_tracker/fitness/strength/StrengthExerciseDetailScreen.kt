package com.example.prokject2_tracker.fitness.strength

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.metrics.ChartRange
import com.example.prokject2_tracker.core.metrics.label
import com.example.prokject2_tracker.core.metrics.pointLabel
import com.example.prokject2_tracker.core.ui.ChartLine
import com.example.prokject2_tracker.core.ui.DatedLineChart
import com.example.prokject2_tracker.fluid.fluidPalette
import com.example.prokject2_tracker.ui.theme.AppDomain
import com.example.prokject2_tracker.ui.theme.topAppBarColors

private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrengthExerciseDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StrengthExerciseDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showRangeMenu by remember { mutableStateOf(false) }
    val palette = fluidPalette()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.FITNESS.topAppBarColors(),
                title = { Text(state.exerciseName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        // A plain scrolling Column rather than a LazyColumn: the content is bounded, and a lazy
        // container would compete with the chart's horizontal drag gesture.
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SessionComparisonCard(
                state = state,
                onPreviousDay = viewModel::goToPreviousDay,
                onNextDay = viewModel::goToNextDay,
                onPickDate = { showDatePicker = true },
                onSelectDay = viewModel::selectDay,
            )

            SectionHeader(
                title = "Training hinzufügen",
                expanded = state.isEntryExpanded,
                onToggle = viewModel::toggleEntryExpanded,
            )
            AnimatedVisibility(visible = state.isEntryExpanded) {
                StrengthSetEntryPanel(
                    state = state,
                    onAdjustWeight = viewModel::adjustWeight,
                    onSetWeight = viewModel::setWeight,
                    onToggleBodyweight = viewModel::toggleBodyweight,
                    onAdjustReps = viewModel::adjustReps,
                    onCommitSet = viewModel::commitSet,
                    onRemoveSet = viewModel::removeSetAt,
                    onResumeAt = viewModel::resumeAt,
                    onUndoRemoval = viewModel::undoRemoval,
                    onNoteChange = viewModel::onNoteChange,
                    onNoteCommit = viewModel::persistNote,
                )
            }

            SectionHeader(
                // The header names what one point covers, so switching the x-axis span can never
                // leave you guessing whether a value is a session or a whole month.
                title = "Verlauf ${state.chartGranularity.pointLabel()}",
                expanded = state.isChartExpanded,
                onToggle = viewModel::toggleChartExpanded,
            )
            AnimatedVisibility(visible = state.isChartExpanded) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(modifier = Modifier.align(Alignment.End)) {
                            TextButton(onClick = { showRangeMenu = true }) {
                                Text(state.chartRange.label())
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Zeitraum wählen")
                            }
                            DropdownMenu(
                                expanded = showRangeMenu,
                                onDismissRequest = { showRangeMenu = false },
                            ) {
                                ChartRange.entries.forEach { range ->
                                    DropdownMenuItem(
                                        text = { Text(range.label()) },
                                        onClick = {
                                            viewModel.onChartRangeChange(range)
                                            showRangeMenu = false
                                        },
                                    )
                                }
                            }
                        }
                        // Built here rather than via ChartSeries.toChartLine, which infers zeroBased
                        // from the unit string and would put volume on a non-zero axis too.
                        DatedLineChart(
                            lines = listOf(
                                ChartLine(
                                    label = "Volumen",
                                    unit = "kg",
                                    color = palette[0],
                                    points = state.volumeSeries,
                                    zeroBased = true,
                                ),
                                ChartLine(
                                    label = "Max. Gewicht",
                                    unit = "kg",
                                    color = palette[1],
                                    points = state.maxWeightSeries,
                                    // A working range of 80–100 kg on a zero axis is a flat line.
                                    zeroBased = false,
                                ),
                            ),
                            panelHeight = 180,
                            overlaid = true,
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedEpochDay * MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.selectDay(it / MILLIS_PER_DAY) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/**
 * The fold handle of one block. Full-width and outside the block's own Card, so a collapsed section
 * is a single row of chrome — the point of folding it away in the first place.
 */
@Composable
private fun SectionHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) "$title einklappen" else "$title ausklappen",
        )
    }
}
