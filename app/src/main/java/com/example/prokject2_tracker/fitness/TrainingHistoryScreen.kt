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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.fitness.cardio.CardioSession
import com.example.prokject2_tracker.fitness.strength.StrengthLogEntry
import com.example.prokject2_tracker.fitness.strength.StrengthSet
import com.example.prokject2_tracker.fitness.strength.formatSetSummary
import com.example.prokject2_tracker.fitness.strength.toDraft
import com.example.prokject2_tracker.ui.theme.AppDomain
import com.example.prokject2_tracker.ui.theme.topAppBarColors
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Everything trained, newest day first. Tapping a row opens the detail page for that exercise or
 * activity **on that row's date** — navigation is by (subject, day) rather than by entry id, which
 * is what lets the detail page show the right "letztes Training" comparison while editing history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingHistoryScreen(
    onBack: () -> Unit,
    onOpenStrengthSession: (exerciseId: String, epochDay: Long) -> Unit,
    onOpenCardioSession: (activityTypeId: String, epochDay: Long, sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrainingHistoryViewModel = hiltViewModel(),
) {
    val rows by viewModel.rows.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.FITNESS.topAppBarColors(),
                title = { Text("Trainingsverlauf") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        if (rows.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Noch keine Trainingseinheiten geloggt.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rows.groupBy { it.epochDay }.forEach { (epochDay, dayRows) ->
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
                                onClick = {
                                    onOpenCardioSession(
                                        row.session.activityTypeId,
                                        row.session.epochDay,
                                        row.session.id,
                                    )
                                },
                                onDelete = { viewModel.deleteCardio(row.session) },
                            )
                            is TrainingListRow.Strength -> StrengthTrainingRow(
                                entry = row.entry,
                                sets = row.sets,
                                onClick = { onOpenStrengthSession(row.entry.exerciseId, row.entry.epochDay) },
                                onDelete = { viewModel.deleteStrength(row.entry) },
                            )
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
            Icon(
                Icons.AutoMirrored.Filled.DirectionsRun,
                contentDescription = null,
                modifier = Modifier.padding(end = 12.dp),
            )
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
private fun StrengthTrainingRow(
    entry: StrengthLogEntry,
    sets: List<StrengthSet>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Category, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f).clickable(onClick = onClick)) {
                Text(entry.exerciseName)
                // Shares formatSetSummary with the detail page so the two can't drift apart.
                Text(formatSetSummary(sets.sortedBy { it.setIndex }.map { it.toDraft() }))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Löschen")
            }
        }
    }
}
