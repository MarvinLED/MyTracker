package com.example.prokject2_tracker.nutrition.diary

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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
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
import com.example.prokject2_tracker.ui.theme.AppDomain
import com.example.prokject2_tracker.ui.theme.topAppBarColors
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onAddEntry: (Long) -> Unit,
    onEditEntry: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val undoableDelete by viewModel.undoableDelete.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.DIARY.topAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::goToPreviousDay) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Vorheriger Tag")
                        }
                        Text(
                            DateUtils.localDateOfEpochDay(uiState.epochDay).format(dateFormatter),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(onClick = viewModel::goToNextDay) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Nächster Tag")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            // The undo sits beside the add button rather than in a snackbar, so it stays reachable
            // for as long as the day is on screen instead of timing out after a few seconds.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                undoableDelete?.let { deleted ->
                    SmallFloatingActionButton(
                        onClick = viewModel::undoDelete,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Icon(
                            Icons.Filled.Undo,
                            contentDescription = "Löschen von \"${deleted.entry.sourceName}\" rückgängig machen",
                        )
                    }
                }
                FloatingActionButton(
                    onClick = { onAddEntry(uiState.epochDay) },
                    containerColor = AppDomain.DIARY.accent(),
                    contentColor = AppDomain.DIARY.onAccent(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Eintrag hinzufügen")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "day-total") { DayTotalCard(uiState) }
            item(key = "macro-ring") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        MacroEnergyRing(uiState.totals)
                    }
                }
            }
            if (uiState.nutrientGoals.isNotEmpty()) {
                item(key = "nutrient-goals") {
                    NutrientGoalBars(totals = uiState.totals, goals = uiState.nutrientGoals)
                }
            }

            if (uiState.entriesByMeal.isEmpty()) {
                item(key = "empty") {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Noch nichts für diesen Tag geloggt.")
                    }
                }
            } else {
                MealType.entries.forEach { mealType ->
                    val entries = uiState.entriesByMeal[mealType].orEmpty()
                    if (entries.isNotEmpty()) {
                        item(key = "header-${mealType.name}") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    mealType.label(),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${entries.sumOf { it.kcal }.formatCompact()} kcal",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(entries, key = { it.id }) { entry ->
                            DiaryEntryRow(
                                entry = entry,
                                onEdit = { onEditEntry(entry.id) },
                                onDelete = { viewModel.deleteEntry(entry) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayTotalCard(uiState: DiaryDayUiState) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "${uiState.totalKcal.formatCompact()} / ${uiState.calorieGoalKcal.formatCompact()} kcal",
                style = MaterialTheme.typography.titleMedium,
            )
            val progress = if (uiState.calorieGoalKcal > 0) {
                (uiState.totalKcal / uiState.calorieGoalKcal).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DiaryEntryRow(entry: DiaryEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
                Text(entry.sourceName)
                // A Schnelleintrag has no meaningful quantity — its "1 Schnelleintrag" would just be noise.
                val details = if (entry.sourceType == DiarySourceType.QUICK) {
                    "${entry.quantityUnit} · ${entry.kcal.formatCompact()} kcal"
                } else {
                    "${entry.quantity.formatCompact()} ${entry.quantityUnit} · ${entry.kcal.formatCompact()} kcal"
                }
                Text(details)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Bearbeiten")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Löschen")
            }
        }
    }
}
