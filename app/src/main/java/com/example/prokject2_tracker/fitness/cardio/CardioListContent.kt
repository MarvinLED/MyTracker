package com.example.prokject2_tracker.fitness.cardio

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.formatCompact
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CardioListContent(
    onAddSession: () -> Unit,
    onEditSession: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CardioListViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsState()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)

    Box(modifier = modifier.fillMaxSize()) {
        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Noch keine Cardio-Einheiten geloggt.")
            }
        } else {
            val grouped = sessions.groupBy { it.epochDay }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                grouped.forEach { (epochDay, daySessions) ->
                    item(key = "header-$epochDay") {
                        Text(
                            DateUtils.localDateOfEpochDay(epochDay).format(dateFormatter),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    items(daySessions, key = { it.id }) { session ->
                        CardioSessionRow(
                            session = session,
                            onClick = { onEditSession(session.id) },
                            onDelete = { viewModel.delete(session) },
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAddSession,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Cardio-Einheit hinzufügen")
        }
    }
}

@Composable
private fun CardioSessionRow(session: CardioSession, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
            Column(
                modifier = Modifier.weight(1f).clickable(onClick = onClick),
            ) {
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
