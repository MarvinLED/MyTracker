package com.example.mytracker.fitness.cardio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.core.ui.ChartLine
import com.example.mytracker.core.ui.DatedLineChart
import com.example.mytracker.core.ui.dismissingKeyboard
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.fluid.fluidPalette
import com.example.mytracker.ui.theme.AppDomain
import com.example.mytracker.ui.theme.topAppBarColors
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MILLIS_PER_DAY = 86_400_000L
private val dayFormatter = DateTimeFormatter.ofPattern("EEE, d. MMMM", Locale.GERMAN)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardioActivityDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CardioActivityDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val palette = fluidPalette()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) viewModel.consumeSaved()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.FITNESS.topAppBarColors(),
                title = { Text(state.activityTypeName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (state.editingSessionId != null) {
                        IconButton(onClick = viewModel::deleteEditedSession) {
                            Icon(Icons.Filled.Delete, contentDescription = "Einheit löschen")
                        }
                    }
                    TextButton(
                        onClick = dismissingKeyboard(viewModel::save),
                        enabled = state.form.isValid,
                    ) { Text("Speichern") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::goToPreviousDay) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Vorheriger Tag")
                        }
                        TextButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                            Text(
                                DateUtils.localDateOfEpochDay(state.selectedEpochDay).format(dayFormatter),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        IconButton(onClick = viewModel::goToNextDay) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Nächster Tag")
                        }
                    }

                    val previousLabel = state.previousDay
                        ?.let {
                            DateUtils.formatDaysSince(
                                DateUtils.daysBetweenEpochDays(it.epochDay, state.selectedEpochDay),
                            )
                        }
                        ?: "—"
                    Row {
                        Text("", modifier = Modifier.weight(1.1f))
                        HeaderCell("Letztes ($previousLabel)", Modifier.weight(1f))
                        HeaderCell("Dieses", Modifier.weight(1f))
                    }
                    StatRow(
                        "Dauer",
                        state.previousDay?.let { "${it.totalMinutes.formatCompact()} min" },
                        state.currentDay?.let { "${it.totalMinutes.formatCompact()} min" },
                    )
                    StatRow(
                        "Distanz",
                        state.previousDay?.totalDistanceKm?.let { "${it.formatCompact()} km" },
                        state.currentDay?.totalDistanceKm?.let { "${it.formatCompact()} km" },
                    )
                    StatRow(
                        "Ø Pace",
                        state.previousDay?.paceMinPerKm?.let { "${it.formatCompact()} min/km" },
                        state.currentDay?.paceMinPerKm?.let { "${it.formatCompact()} min/km" },
                    )
                    state.minutesDelta?.let { delta ->
                        Row {
                            Text(
                                "Δ Dauer",
                                modifier = Modifier.weight(1.1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text("", modifier = Modifier.weight(1f))
                            Text(
                                (if (delta >= 0) "+" else "−") + "${kotlin.math.abs(delta).formatCompact()} min",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (delta >= 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Two runs in one day are legitimate and each keeps its own numbers, so the day's
                    // sessions are selectable rather than merged.
                    if (state.sessionsOfDay.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.sessionsOfDay.forEachIndexed { index, session ->
                                FilterChip(
                                    selected = state.editingSessionId == session.id,
                                    onClick = { viewModel.selectSession(session.id) },
                                    label = { Text("Einheit ${index + 1}") },
                                )
                            }
                            FilterChip(
                                selected = state.editingSessionId == null,
                                onClick = { viewModel.selectSession(null) },
                                label = { Text("+ Neu") },
                            )
                        }
                        HorizontalDivider()
                    }

                    CardioEntryForm(
                        state = state.form,
                        onDurationChange = viewModel::onDurationChange,
                        onDistanceChange = viewModel::onDistanceChange,
                        onCaloriesChange = viewModel::onCaloriesChange,
                        onHeartRateChange = viewModel::onHeartRateChange,
                        onNoteChange = viewModel::onNoteChange,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Verlauf pro Woche", style = MaterialTheme.typography.titleMedium)
                    DatedLineChart(
                        lines = listOf(
                            ChartLine("Dauer", "min", palette[0], state.weeklyMinutes, zeroBased = true),
                            ChartLine("Distanz", "km", palette[1], state.weeklyDistance, zeroBased = true),
                            // A 4,5–6,5 min/km range on a zero axis is a flat line.
                            ChartLine("Ø Pace", "min/km", palette[2], state.weeklyPace, zeroBased = false),
                        ),
                        panelHeight = 110,
                    )
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

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(
        text,
        modifier = modifier,
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatRow(label: String, previous: String?, current: String?) {
    Row {
        Text(
            label,
            modifier = Modifier.weight(1.1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(previous ?: "—", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
        Text(current ?: "—", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
    }
}
