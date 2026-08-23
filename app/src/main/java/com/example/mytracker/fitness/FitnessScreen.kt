package com.example.mytracker.fitness

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.core.util.label
import com.example.mytracker.fitness.strength.label
import com.example.mytracker.ui.theme.AppDomain
import com.example.mytracker.ui.theme.topAppBarColors

/**
 * The Fitness landing screen: goals, then the list of everything trainable. Tapping a row opens
 * that exercise's or activity's page for today, which is where logging actually happens — the
 * chronological history moved to its own screen behind the app bar's history icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessScreen(
    onOpenHistory: () -> Unit,
    onOpenExercise: (exerciseId: String) -> Unit,
    onOpenCardioActivity: (activityTypeId: String) -> Unit,
    onAddExercise: () -> Unit,
    onOpenMuscleGroupLibrary: () -> Unit,
    onOpenExerciseLibrary: () -> Unit,
    onOpenCardioActivityTypeLibrary: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FitnessViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showOverflow by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.FITNESS.topAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                title = { Text("Fitness") },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.History, contentDescription = "Trainingsverlauf")
                    }
                    // The three library screens are rarely used and read far better as labelled
                    // menu items than as three more ambiguous glyphs beside the history icon.
                    IconButton(onClick = { showOverflow = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Weitere Optionen")
                    }
                    DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                        DropdownMenuItem(
                            text = { Text("Übungen verwalten") },
                            onClick = {
                                showOverflow = false
                                onOpenExerciseLibrary()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Muskelgruppen verwalten") },
                            onClick = {
                                showOverflow = false
                                onOpenMuscleGroupLibrary()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Cardio-Aktivitäten verwalten") },
                            onClick = {
                                showOverflow = false
                                onOpenCardioActivityTypeLibrary()
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            // Adding an exercise mid-workout is a real scenario, so the FAB follows the active tab.
            FloatingActionButton(
                onClick = if (state.selectedTab == FitnessTab.STRENGTH) onAddExercise else onOpenCardioActivityTypeLibrary,
                containerColor = AppDomain.FITNESS.accent(),
                contentColor = AppDomain.FITNESS.onAccent(),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = if (state.selectedTab == FitnessTab.STRENGTH) {
                        "Übung hinzufügen"
                    } else {
                        "Cardio-Aktivität hinzufügen"
                    },
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (state.goalRows.isNotEmpty() || state.maxWeightGoalRows.isNotEmpty()) {
                // Capped and scrollable: now that every goal can be set from one list, there can be
                // a dozen of them, and an uncapped block would push the exercise list — the reason
                // this screen is opened mid-session — off the bottom of the screen.
                Column(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.goalRows.forEach { row ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(row.label, style = MaterialTheme.typography.bodyMedium)
                            Text(row.valueText)
                            // What the goal has been doing, not only what it is doing: one week says
                            // little, eight say whether the target is the right one.
                            row.streakText?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { row.fraction },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    state.maxWeightGoalRows.forEach { row ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(row.label, style = MaterialTheme.typography.bodyMedium)
                            Text(row.valueText, style = MaterialTheme.typography.bodySmall)
                            Text(
                                row.statusText,
                                style = MaterialTheme.typography.labelMedium,
                                // The status is the whole point of a long-term goal, so it is the
                                // one line that carries a colour: behind plan has to be visible
                                // without reading two numbers and doing the subtraction.
                                color = if (row.isOnTrack) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                            LinearProgressIndicator(
                                progress = { row.fraction },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.selectedTab == FitnessTab.STRENGTH,
                    onClick = { viewModel.onTabSelected(FitnessTab.STRENGTH) },
                    label = { Text("Kraft") },
                )
                FilterChip(
                    selected = state.selectedTab == FitnessTab.CARDIO,
                    onClick = { viewModel.onTabSelected(FitnessTab.CARDIO) },
                    label = { Text("Kardio") },
                )
            }

            if (state.items.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.selectedTab == FitnessTab.STRENGTH) {
                            "Noch keine Übungen angelegt."
                        } else {
                            "Noch keine Cardio-Aktivitäten angelegt."
                        },
                    )
                }
            } else {
                val today = DateUtils.todayEpochDay()
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                when (state.selectedTab) {
                                    FitnessTab.STRENGTH -> onOpenExercise(item.id)
                                    FitnessTab.CARDIO -> onOpenCardioActivity(item.id)
                                }
                            },
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(item.name, style = MaterialTheme.typography.bodyLarge)
                                val lastTrained = item.lastTrainedEpochDay
                                    ?.let { "zuletzt: ${DateUtils.formatDaysSince(DateUtils.daysBetweenEpochDays(it, today))}" }
                                    ?: "noch nie trainiert"
                                Text(
                                    listOf(item.subtitle, lastTrained).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                // The top set gets its own line and the stronger ink: it is the
                                // number you open this list to find before starting the exercise.
                                item.topSets?.let { topSets ->
                                    Text(
                                        topSets,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
