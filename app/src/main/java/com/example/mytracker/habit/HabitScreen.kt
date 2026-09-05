package com.example.mytracker.habit

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.core.util.DayStreak
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.core.util.label
import com.example.mytracker.core.util.toLocaleDoubleOrNull
import com.example.mytracker.ui.theme.AppDomain
import com.example.mytracker.ui.theme.topAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Habit?>(null) }
    var valueTarget by remember { mutableStateOf<Habit?>(null) }
    var goalsTarget by remember { mutableStateOf<Habit?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.HABIT.topAppBarColors(),
                title = { Text("Habits") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Habit hinzufügen")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.habits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Noch keine Habits angelegt.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.habits, key = { it.id }) { habit ->
                        val checked = habit.id in uiState.checkedInHabitIds
                        val streak = uiState.streaksByHabitId[habit.id] ?: DayStreak(current = 0, best = 0)
                        val value = uiState.valuesByHabitId[habit.id]
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (habit.type == HabitType.YES_NO) {
                                    Checkbox(checked = checked, onCheckedChange = { viewModel.toggleCheckedIn(habit) })
                                }
                                Column(
                                    modifier = Modifier.weight(1f).clickable {
                                        if (habit.type == HabitType.YES_NO) {
                                            renameTarget = habit
                                        } else {
                                            valueTarget = habit
                                        }
                                    },
                                ) {
                                    Text(habit.name)
                                    val subtitle = buildString {
                                        if (streak.current > 0) append("🔥 ${streak.current}")
                                        // Only once it beats the running one: repeating the same
                                        // number twice in a row would say nothing. A record of one
                                        // is not a record either — that is just "einmal geschafft".
                                        if (streak.best >= 2 && streak.best > streak.current) {
                                            if (isNotEmpty()) append(" · ")
                                            append("🏆 ${streak.best}")
                                        }
                                        if (habit.type != HabitType.YES_NO) {
                                            if (isNotEmpty()) append(" · ")
                                            append(value?.let { it.formatCompact() } ?: "–")
                                        }
                                    }
                                    if (subtitle.isNotEmpty()) {
                                        // The two emoji carry the meaning for the eye but say
                                        // nothing to a screen reader, so it gets the words instead.
                                        val spoken = listOfNotNull(
                                            "Serie ${streak.current} Tage".takeIf { streak.current > 0 },
                                            "Rekord ${streak.best} Tage"
                                                .takeIf { streak.best >= 2 && streak.best > streak.current },
                                        ).joinToString(", ")
                                        Text(
                                            subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = if (spoken.isEmpty()) {
                                                Modifier
                                            } else {
                                                Modifier.semantics { contentDescription = spoken }
                                            },
                                        )
                                    }
                                }
                                if (habit.type != HabitType.YES_NO) {
                                    IconButton(onClick = { goalsTarget = habit }) {
                                        Icon(Icons.Filled.Flag, contentDescription = "Ziele")
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteHabit(habit) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Löschen")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        HabitAddDialog(
            onConfirm = { name, type ->
                viewModel.addHabit(name, type)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    renameTarget?.let { habit ->
        HabitNameDialog(
            title = "Habit umbenennen",
            initialName = habit.name,
            onConfirm = { name ->
                viewModel.renameHabit(habit, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    valueTarget?.let { habit ->
        HabitValueDialog(
            habit = habit,
            initialValue = uiState.valuesByHabitId[habit.id],
            onConfirm = { value ->
                viewModel.logValue(habit, value)
                valueTarget = null
            },
            onDismiss = { valueTarget = null },
        )
    }

    goalsTarget?.let { habit ->
        HabitGoalsDialog(
            habit = habit,
            goals = uiState.goalsByHabitId[habit.id] ?: emptyList(),
            progressByGoalId = uiState.progressByGoalId,
            onSetGoal = { period, target -> viewModel.setGoal(habit, period, target) },
            onDismiss = { goalsTarget = null },
        )
    }
}


@Composable
private fun HabitNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}

@Composable
private fun HabitAddDialog(
    onConfirm: (String, HabitType) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(HabitType.YES_NO) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Habit hinzufügen") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HabitType.entries.forEach { candidate ->
                        FilterChip(
                            selected = type == candidate,
                            onClick = { type = candidate },
                            label = { Text(candidate.label()) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, type) }, enabled = name.isNotBlank()) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}

@Composable
private fun HabitGoalsDialog(
    habit: Habit,
    goals: List<HabitGoal>,
    progressByGoalId: Map<String, Double>,
    onSetGoal: (GoalPeriod, Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    val goalsByPeriod = goals.associateBy { it.period }
    val textByPeriod = remember {
        mutableStateOf(
            GoalPeriod.entries.associateWith { period -> goalsByPeriod[period]?.targetValue?.let { it.formatCompact() } ?: "" },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ziele: ${habit.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GoalPeriod.entries.forEach { period ->
                    val goal = goalsByPeriod[period]
                    val progress = goal?.let { progressByGoalId[it.id] }
                    Column {
                        OutlinedTextField(
                            value = textByPeriod.value[period].orEmpty(),
                            onValueChange = { textByPeriod.value = textByPeriod.value + (period to it) },
                            label = { Text(period.label()) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (goal != null && progress != null) {
                            Text(
                                "${progress.formatCompact()} / ${goal.targetValue.formatCompact()} ${periodContextLabel(period)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    GoalPeriod.entries.forEach { period ->
                        onSetGoal(period, textByPeriod.value[period].orEmpty().toLocaleDoubleOrNull())
                    }
                    onDismiss()
                },
            ) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}

private fun periodContextLabel(period: GoalPeriod): String = when (period) {
    GoalPeriod.DAILY -> "heute"
    GoalPeriod.WEEKLY -> "diese Woche"
    GoalPeriod.MONTHLY -> "diesen Monat"
}
