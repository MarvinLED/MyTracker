package com.example.mytracker.fitness.strength

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
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.mytracker.core.metrics.ChartRange
import com.example.mytracker.core.metrics.label
import com.example.mytracker.core.metrics.pointLabel
import com.example.mytracker.core.ui.ChartLine
import com.example.mytracker.core.ui.ChartLineStyle
import com.example.mytracker.core.ui.DatedLineChart
import com.example.mytracker.fluid.fluidPalette
import com.example.mytracker.ui.theme.AppDomain
import com.example.mytracker.ui.theme.topAppBarColors

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

            if (state.hasGoals) {
                SectionHeader(
                    title = "Ziele",
                    expanded = state.isGoalsExpanded,
                    onToggle = viewModel::toggleGoalsExpanded,
                )
                AnimatedVisibility(visible = state.isGoalsExpanded) {
                    GoalsCard(state = state)
                }
            }

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
                        //
                        // The units are what group the axes (see DatedLineChart's sharedScale), and
                        // they have to differ: volume runs in the thousands and a top set in the
                        // tens, so one axis for both would flatten the heavier line to the floor.
                        // "kg gesamt" is also the more honest name for a volume anyway.
                        DatedLineChart(
                            lines = listOfNotNull(
                                ChartLine(
                                    label = "Volumen",
                                    unit = "kg gesamt",
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
                                // The plan on the same axis as the line it is a plan for — that is
                                // the whole point, and why it shares the "kg" unit. Dashed and
                                // without markers: it is a target, not a set of measurements.
                                state.goalPlanSeries.takeIf { it.size >= 2 }?.let { points ->
                                    ChartLine(
                                        label = "Soll",
                                        unit = "kg",
                                        color = palette[3],
                                        points = points,
                                        zeroBased = false,
                                        style = ChartLineStyle.DASHED,
                                        markers = false,
                                    )
                                },
                            ),
                            panelHeight = 180,
                            overlaid = true,
                            sharedScale = true,
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
 * What this exercise is being trained towards, where it is trained. The Steigerungen sit above the
 * long-term goal because they are what today's session can still change; the long-term one is the
 * context they add up to.
 */
@Composable
private fun GoalsCard(state: StrengthExerciseDetailUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.goalRows.forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(row.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        row.valueText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (row.isMet) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    LinearProgressIndicator(progress = { row.fraction }, modifier = Modifier.fillMaxWidth())
                }
            }
            state.maxWeightGoalRow?.let { row ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(row.label, style = MaterialTheme.typography.bodyMedium)
                    Text(row.valueText, style = MaterialTheme.typography.bodySmall)
                    Text(
                        row.statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (row.isOnTrack) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    LinearProgressIndicator(progress = { row.fraction }, modifier = Modifier.fillMaxWidth())
                    // Out of the change log: the goal row itself is overwritten in place, so this is
                    // the only place the answer to "seit wann eigentlich?" survives.
                    state.goalSince?.let { since ->
                        Text(
                            "Ziel gesetzt am $since",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
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
