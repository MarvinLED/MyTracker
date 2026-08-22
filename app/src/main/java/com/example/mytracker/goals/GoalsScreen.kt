package com.example.prokject2_tracker.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.ui.TimeOfDayField
import com.example.prokject2_tracker.core.ui.dismissingKeyboard
import com.example.prokject2_tracker.core.util.GoalPeriod
import com.example.prokject2_tracker.core.util.label
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import com.example.prokject2_tracker.fitness.FitnessGoalMetric
import com.example.prokject2_tracker.fitness.label
import com.example.prokject2_tracker.fitness.strength.MovementDirection
import com.example.prokject2_tracker.fitness.strength.MuscleGroup
import com.example.prokject2_tracker.fitness.strength.label
import com.example.prokject2_tracker.ui.theme.AppDomain
import com.example.prokject2_tracker.ui.theme.topAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // Only one section at a time by default — all four at once is more than fits on a screen.
    // null means "Alle". Pure view state: the whole form stays loaded and is saved either way.
    var selectedCategory by rememberSaveable { mutableStateOf<GoalCategory?>(GoalCategory.NUTRITION) }

    LaunchedEffect(Unit) {
        viewModel.saved.collect { snackbarHostState.showSnackbar("Ziele gespeichert") }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        topBar = {
            TopAppBar(
                colors = AppDomain.GOALS.topAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                title = { Text("Ziele") },
                actions = {
                    TextButton(onClick = dismissingKeyboard(viewModel::save)) { Text("Speichern") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Outside the scrolling column on purpose: the filter stays reachable while a long
            // section is scrolled.
            GoalCategoryFilter(
                selected = selectedCategory,
                onSelected = { selectedCategory = it },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                fun shows(category: GoalCategory) = selectedCategory == null || selectedCategory == category

                if (shows(GoalCategory.NUTRITION)) NutritionGoalsSection(state, viewModel)
                if (shows(GoalCategory.SLEEP)) SleepGoalsSection(state, viewModel)
                if (shows(GoalCategory.FLUID)) FluidGoalsSection(state, viewModel)
                if (shows(GoalCategory.FITNESS)) FitnessGoalsSection(state, viewModel)
            }
        }
    }
}

/**
 * The category filter as one chip that opens a menu. Single selection: "Alle" shows every
 * section again, any other entry replaces what was picked.
 *
 * Same shape as the Tagebuch's tag filter (`TagFilterDropdown` in `DiaryAddEntryScreen`): a plain
 * [DropdownMenu] anchored on the chip rather than an `ExposedDropdownMenuBox`, which would want a
 * text field as its anchor.
 */
@Composable
private fun GoalCategoryFilter(
    selected: GoalCategory?,
    onSelected: (GoalCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(selected?.label ?: "Alle") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = "Kategorie wählen") },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Alle") },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
                trailingIcon = {
                    if (selected == null) Icon(Icons.Filled.Check, contentDescription = null)
                },
            )
            GoalCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.label) },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    },
                    trailingIcon = {
                        if (selected == category) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                )
            }
        }
    }
}

@Composable
private fun NutritionGoalsSection(state: GoalsUiState, viewModel: GoalsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(GoalCategory.NUTRITION.label, style = MaterialTheme.typography.titleMedium)
        Text(
            "Minimum, Maximum oder beides — leer lassen heißt \"kein Ziel\". Der Balken im " +
                "Tagebuch läuft bis zum Maximum und markiert das Minimum darin.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.nutrientGoals.forEach { row ->
            NutrientGoalRow(
                row = row,
                onMinChange = { viewModel.onNutrientGoalMinChange(row.nutrient, it) },
                onMaxChange = { viewModel.onNutrientGoalMaxChange(row.nutrient, it) },
            )
        }
    }
}

@Composable
private fun SleepGoalsSection(state: GoalsUiState, viewModel: GoalsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(GoalCategory.SLEEP.label, style = MaterialTheme.typography.titleMedium)
        Text(
            "Schlafdauer in Stunden (7,5 = 7 h 30 min) und die Uhrzeit, zu der du spätestens " +
                "schlafen willst. Leer lassen heißt \"kein Ziel\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.sleepDurationMinHours,
                onValueChange = viewModel::onSleepDurationMinChange,
                label = { Text("Mindestens (h)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.sleepDurationMaxHours,
                onValueChange = viewModel::onSleepDurationMaxChange,
                label = { Text("Höchstens (h)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TimeOfDayField(
                label = "Schlafenszeit spätestens",
                value = state.bedtimeGoalMinuteOfDay,
                onValueChange = viewModel::onBedtimeGoalChange,
                emptyLabel = "kein Ziel",
                defaultMinuteOfDay = 23 * 60,
            )
            if (state.bedtimeGoalMinuteOfDay != null) {
                TextButton(onClick = { viewModel.onBedtimeGoalChange(null) }) { Text("Ziel entfernen") }
            }
        }
    }
}

@Composable
private fun FluidGoalsSection(state: GoalsUiState, viewModel: GoalsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(GoalCategory.FLUID.label, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.waterGoal,
            onValueChange = viewModel::onWaterGoalChange,
            label = { Text("Gesamtziel pro Tag (ml)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        state.fluidTypeGoals.forEach { row ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.type.name, style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = row.minText,
                        onValueChange = { viewModel.onFluidTypeMinChange(row.type.id, it) },
                        label = { Text("Minimum (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = row.maxText,
                        onValueChange = { viewModel.onFluidTypeMaxChange(row.type.id, it) },
                        label = { Text("Maximum (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FitnessGoalsSection(state: GoalsUiState, viewModel: GoalsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(GoalCategory.FITNESS.label, style = MaterialTheme.typography.titleMedium)
        state.fitnessGoals.forEach { row ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val scope = row.muscleGroupName ?: row.movementDirection?.label()
                Text(
                    "${row.metric.label()} · ${row.period.label()}" + (scope?.let { " · $it" } ?: ""),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = row.targetText,
                    onValueChange = { viewModel.onFitnessGoalTargetChange(row.id, it) },
                    label = { Text("Ziel") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(100.dp),
                )
                IconButton(onClick = { viewModel.removeFitnessGoal(row.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Ziel löschen")
                }
            }
        }
        AddFitnessGoalRow(
            availableMuscleGroups = state.availableMuscleGroups,
            onAdd = viewModel::addFitnessGoal,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFitnessGoalRow(
    availableMuscleGroups: List<MuscleGroup>,
    onAdd: (FitnessGoalMetric, GoalPeriod, String?, MovementDirection?, Double) -> Unit,
) {
    var metric by remember { mutableStateOf(FitnessGoalMetric.CARDIO_SESSIONS) }
    var metricMenuExpanded by remember { mutableStateOf(false) }
    var period by remember { mutableStateOf(GoalPeriod.WEEKLY) }
    var muscleGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    var muscleGroupMenuExpanded by remember { mutableStateOf(false) }
    var movementDirection by remember { mutableStateOf<MovementDirection?>(null) }
    var targetText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = metricMenuExpanded,
            onExpandedChange = { metricMenuExpanded = it },
        ) {
            OutlinedTextField(
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                readOnly = true,
                value = metric.label(),
                onValueChange = {},
                label = { Text("Metrik") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = metricMenuExpanded) },
            )
            ExposedDropdownMenu(
                expanded = metricMenuExpanded,
                onDismissRequest = { metricMenuExpanded = false },
            ) {
                FitnessGoalMetric.entries.forEach { candidate ->
                    DropdownMenuItem(
                        text = { Text(candidate.label()) },
                        onClick = {
                            metric = candidate
                            if (candidate != FitnessGoalMetric.STRENGTH_SETS_MUSCLE_GROUP) muscleGroup = null
                            if (candidate != FitnessGoalMetric.STRENGTH_SETS_MOVEMENT_DIRECTION) movementDirection = null
                            metricMenuExpanded = false
                        },
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(GoalPeriod.WEEKLY, GoalPeriod.MONTHLY).forEach { candidate ->
                FilterChip(
                    selected = period == candidate,
                    onClick = { period = candidate },
                    label = { Text(candidate.label()) },
                )
            }
        }

        if (metric == FitnessGoalMetric.STRENGTH_SETS_MUSCLE_GROUP) {
            ExposedDropdownMenuBox(
                expanded = muscleGroupMenuExpanded,
                onExpandedChange = { muscleGroupMenuExpanded = it },
            ) {
                OutlinedTextField(
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                    readOnly = true,
                    value = muscleGroup?.name.orEmpty(),
                    onValueChange = {},
                    label = { Text("Muskelgruppe") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = muscleGroupMenuExpanded) },
                )
                ExposedDropdownMenu(
                    expanded = muscleGroupMenuExpanded,
                    onDismissRequest = { muscleGroupMenuExpanded = false },
                ) {
                    availableMuscleGroups.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(candidate.name) },
                            onClick = {
                                muscleGroup = candidate
                                muscleGroupMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        if (metric == FitnessGoalMetric.STRENGTH_SETS_MOVEMENT_DIRECTION) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MovementDirection.entries.forEach { candidate ->
                    FilterChip(
                        selected = movementDirection == candidate,
                        onClick = { movementDirection = candidate },
                        label = { Text(candidate.label()) },
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = targetText,
                onValueChange = { targetText = it },
                label = { Text("Ziel") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            val target = targetText.toLocaleDoubleOrNull()
            val muscleGroupMissing = metric == FitnessGoalMetric.STRENGTH_SETS_MUSCLE_GROUP && muscleGroup == null
            val movementDirectionMissing =
                metric == FitnessGoalMetric.STRENGTH_SETS_MOVEMENT_DIRECTION && movementDirection == null
            TextButton(
                onClick = {
                    onAdd(metric, period, muscleGroup?.id, movementDirection, target!!)
                    metric = FitnessGoalMetric.CARDIO_SESSIONS
                    period = GoalPeriod.WEEKLY
                    muscleGroup = null
                    movementDirection = null
                    targetText = ""
                },
                enabled = target != null && !muscleGroupMissing && !movementDirectionMissing,
            ) { Text("Ziel hinzufügen") }
        }
    }
}

/**
 * One nutrient's goal: a lower and an upper bound, either of which may stay blank. Same two-field
 * shape as the Getränkearten below, so both kinds of goal are entered the same way.
 */
@Composable
private fun NutrientGoalRow(
    row: NutrientGoalInput,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
) {
    val min = row.minText.toLocaleDoubleOrNull()
    val max = row.maxText.toLocaleDoubleOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${row.nutrient.label} (${row.nutrient.unit})", style = MaterialTheme.typography.bodyMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = row.minText,
                onValueChange = onMinChange,
                label = { Text("Minimum") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = row.maxText,
                onValueChange = onMaxChange,
                label = { Text("Maximum") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        // Such a goal can never be met, and nothing else in the app would say so.
        if (min != null && max != null && min > max) {
            Text(
                "Minimum liegt über dem Maximum — dieses Ziel ist nicht erreichbar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
