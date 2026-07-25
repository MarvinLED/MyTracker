package com.example.prokject2_tracker.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.datastore.NutrientGoalType
import com.example.prokject2_tracker.core.datastore.label
import com.example.prokject2_tracker.core.ui.dismissingKeyboard
import com.example.prokject2_tracker.core.util.GoalPeriod
import com.example.prokject2_tracker.core.util.label
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import com.example.prokject2_tracker.fitness.FitnessGoalMetric
import com.example.prokject2_tracker.fitness.label
import com.example.prokject2_tracker.fitness.strength.MuscleGroup
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
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Ernährung", style = MaterialTheme.typography.titleMedium)
            Text(
                "Leer lassen heißt \"kein Ziel\". Ob ein Wert erreicht oder eingehalten werden soll, " +
                    "legt die Auswahl darunter fest — das Tagebuch zeigt die Balken entsprechend.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.nutrientGoals.forEach { row ->
                NutrientGoalRow(
                    row = row,
                    onValueChange = { viewModel.onNutrientGoalValueChange(row.nutrient, it) },
                    onTypeChange = { viewModel.onNutrientGoalTypeChange(row.nutrient, it) },
                )
            }

            Text("Flüssigkeiten", style = MaterialTheme.typography.titleMedium)
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

            Text("Fitness", style = MaterialTheme.typography.titleMedium)
            state.fitnessGoals.forEach { row ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "${row.metric.label()} · ${row.period.label()}" + (row.muscleGroupName?.let { " · $it" } ?: ""),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFitnessGoalRow(
    availableMuscleGroups: List<MuscleGroup>,
    onAdd: (FitnessGoalMetric, GoalPeriod, String?, Double) -> Unit,
) {
    var metric by remember { mutableStateOf(FitnessGoalMetric.CARDIO_SESSIONS) }
    var metricMenuExpanded by remember { mutableStateOf(false) }
    var period by remember { mutableStateOf(GoalPeriod.WEEKLY) }
    var muscleGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    var muscleGroupMenuExpanded by remember { mutableStateOf(false) }
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
            TextButton(
                onClick = {
                    onAdd(metric, period, muscleGroup?.id, target!!)
                    metric = FitnessGoalMetric.CARDIO_SESSIONS
                    period = GoalPeriod.WEEKLY
                    muscleGroup = null
                    targetText = ""
                },
                enabled = target != null && !muscleGroupMissing,
            ) { Text("Ziel hinzufügen") }
        }
    }
}

/**
 * One nutrient's goal: the value, plus what that value means. The type selector is only enabled once
 * a value is there — picking "höchstens" for a nutrient you aren't tracking has no meaning.
 */
@Composable
private fun NutrientGoalRow(
    row: NutrientGoalInput,
    onValueChange: (String) -> Unit,
    onTypeChange: (NutrientGoalType) -> Unit,
) {
    val hasValue = row.valueText.toLocaleDoubleOrNull()?.let { it > 0.0 } == true
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = row.valueText,
            onValueChange = onValueChange,
            label = { Text("${row.nutrient.label} (${row.nutrient.unit})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            NutrientGoalType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = row.type == type,
                    onClick = { onTypeChange(type) },
                    enabled = hasValue,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = NutrientGoalType.entries.size),
                    label = { Text(type.label(), style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
    }
}
