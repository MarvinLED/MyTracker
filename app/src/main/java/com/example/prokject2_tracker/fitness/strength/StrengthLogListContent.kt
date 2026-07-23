package com.example.prokject2_tracker.fitness.strength

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
fun StrengthLogListContent(
    onAddEntry: () -> Unit,
    onEditEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StrengthLogListViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsState()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)

    Box(modifier = modifier.fillMaxSize()) {
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Noch keine Kraftübungen geloggt.")
            }
        } else {
            val grouped = entries.groupBy { it.epochDay }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                grouped.forEach { (epochDay, dayEntries) ->
                    item(key = "header-$epochDay") {
                        Text(
                            DateUtils.localDateOfEpochDay(epochDay).format(dateFormatter),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    items(dayEntries, key = { it.id }) { entry ->
                        StrengthLogEntryRow(
                            entry = entry,
                            onClick = { onEditEntry(entry.id) },
                            onDelete = { viewModel.delete(entry) },
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAddEntry,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Kraftübung hinzufügen")
        }
    }
}

@Composable
private fun StrengthLogEntryRow(entry: StrengthLogEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).clickable(onClick = onClick)) {
                Text(entry.exerciseName)
                Text("${entry.sets} x ${entry.reps} @ ${entry.weightKg.formatCompact()} kg")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Löschen")
            }
        }
    }
}
