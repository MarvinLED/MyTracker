package com.example.prokject2_tracker.fitness

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.core.util.label
import com.example.prokject2_tracker.fitness.cardio.CardioSession
import com.example.prokject2_tracker.fitness.strength.StrengthLogEntry
import com.example.prokject2_tracker.fitness.strength.StrengthSet
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Hosts the fitness domain: a unified, date-sorted list of cardio + strength training entries. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessScreen(
    onAddTraining: () -> Unit,
    onEditCardioSession: (String) -> Unit,
    onEditStrengthLogEntry: (String) -> Unit,
    onOpenExerciseLibrary: () -> Unit,
    onOpenMuscleGroupLibrary: () -> Unit,
    onOpenCardioActivityTypeLibrary: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FitnessViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                title = { Text("Fitness") },
                actions = {
                    IconButton(onClick = onOpenExerciseLibrary) {
                        Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Übungen verwalten")
                    }
                    IconButton(onClick = onOpenMuscleGroupLibrary) {
                        Icon(Icons.Filled.Category, contentDescription = "Muskelgruppen verwalten")
                    }
                    IconButton(onClick = onOpenCardioActivityTypeLibrary) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = "Cardio-Aktivitäten verwalten")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTraining) {
                Icon(Icons.Filled.Add, contentDescription = "Training hinzufügen")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Zuletzt Cardio: ${state.daysSinceLastCardio?.let { DateUtils.formatDaysSince(it) } ?: "noch keins"}")
                Text("Zuletzt Krafttraining: ${state.daysSinceLastStrength?.let { DateUtils.formatDaysSince(it) } ?: "noch keins"}")
            }

            if (state.goals.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.goals.forEach { goal ->
                        val progress = state.progressByGoalId[goal.id] ?: 0.0
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${goal.metric.label()} · ${goal.period.label()}", style = MaterialTheme.typography.bodyMedium)
                            Text("${progress.formatCompact()} / ${goal.targetValue.formatCompact()}")
                            val fraction = if (goal.targetValue > 0) {
                                (progress / goal.targetValue).toFloat().coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            if (state.rows.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Noch keine Trainingseinheiten geloggt.")
                }
            } else {
                val grouped = state.rows.groupBy { it.epochDay }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    grouped.forEach { (epochDay, dayRows) ->
                        item(key = "header-$epochDay") {
                            Text(
                                DateUtils.localDateOfEpochDay(epochDay).format(dateFormatter),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        items(
                            dayRows,
                            key = { row ->
                                when (row) {
                                    is TrainingListRow.Cardio -> "cardio-${row.session.id}"
                                    is TrainingListRow.Strength -> "strength-${row.entry.id}"
                                }
                            },
                        ) { row ->
                            when (row) {
                                is TrainingListRow.Cardio -> CardioTrainingRow(
                                    session = row.session,
                                    onClick = { onEditCardioSession(row.session.id) },
                                    onDelete = { viewModel.deleteCardio(row.session) },
                                )
                                is TrainingListRow.Strength -> StrengthTrainingRow(
                                    entry = row.entry,
                                    sets = row.sets,
                                    onClick = { onEditStrengthLogEntry(row.entry.id) },
                                    onDelete = { viewModel.deleteStrength(row.entry) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardioTrainingRow(session: CardioSession, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f).clickable(onClick = onClick)) {
                Text(session.activityTypeName)
                val distancePart = session.distanceKm?.let { " · ${it.formatCompact()} km" }.orEmpty()
                val caloriesPart = session.caloriesBurned?.let { " · ${it.formatCompact()} kcal" }.orEmpty()
                val heartRatePart = session.avgHeartRateBpm?.let { " · ⌀ $it bpm" }.orEmpty()
                Text("${session.durationMinutes.formatCompact()} min$distancePart$caloriesPart$heartRatePart")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Löschen")
            }
        }
    }
}

@Composable
private fun StrengthTrainingRow(entry: StrengthLogEntry, sets: List<StrengthSet>, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Category, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f).clickable(onClick = onClick)) {
                Text(entry.exerciseName)
                val summary = sets.sortedBy { it.setIndex }
                    .joinToString(" · ") { set -> "${set.reps}× ${set.weightKg?.let { "${it.formatCompact()} kg" } ?: "KG"}" }
                Text(summary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Löschen")
            }
        }
    }
}
