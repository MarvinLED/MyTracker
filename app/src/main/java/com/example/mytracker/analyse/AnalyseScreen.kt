package com.example.mytracker.analyse

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.core.metrics.AnalyseDateRange
import com.example.mytracker.core.metrics.Granularity
import com.example.mytracker.core.metrics.label
import com.example.mytracker.core.ui.DatedLineChart
import com.example.mytracker.fitness.strength.MovementDirection
import com.example.mytracker.fitness.strength.MuscleGroup
import com.example.mytracker.fitness.strength.label
import com.example.mytracker.fitness.strength.StrengthExercise
import com.example.mytracker.fluid.fluidPalette
import com.example.mytracker.ui.theme.AppDomain
import com.example.mytracker.ui.theme.topAppBarColors

private val METRIC_CATEGORY_ORDER = listOf("cardio", "strength", "nutrition", "fluid", "habit", "weight", "overall")

private val METRIC_CATEGORY_LABELS = mapOf(
    "cardio" to "Cardio",
    "strength" to "Kraft",
    "nutrition" to "Ernährung",
    "fluid" to "Flüssigkeit",
    "habit" to "Habits",
    "weight" to "Gewicht",
    "overall" to "Gesamt",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyseScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val muscleGroups by viewModel.muscleGroups.collectAsState()
    // The same validated categorical palette the rest of the app's charts use, so a series here is
    // coloured out of the same set as a slice in the Flüssigkeiten donut.
    val chartPalette = fluidPalette()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.ANALYSE.topAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                title = { Text("Analyse") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyseDateRange.entries.forEach { range ->
                    FilterChip(
                        selected = range == state.dateRange,
                        onClick = { viewModel.onDateRangeChange(range) },
                        label = { Text(range.label()) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Granularity.entries.forEach { granularity ->
                    FilterChip(
                        selected = granularity == state.granularity,
                        onClick = { viewModel.onGranularityChange(granularity) },
                        label = { Text(granularity.label()) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                METRIC_CATEGORY_ORDER.forEach { category ->
                    val metrics = viewModel.availableMetrics.filter { it.category == category }
                    if (metrics.isNotEmpty()) {
                        Text(METRIC_CATEGORY_LABELS[category] ?: category, style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            metrics.forEach { metric ->
                                FilterChip(
                                    selected = metric.id in state.selectedMetricIds,
                                    onClick = { viewModel.onMetricToggle(metric.id) },
                                    label = { Text(metric.displayName) },
                                )
                            }
                        }
                    }
                }
            }

            val comparisonLines = listOfNotNull(
                state.primarySeries?.toChartLine(chartPalette[0]),
                state.secondarySeries?.toChartLine(chartPalette[1]),
            )
            if (comparisonLines.isNotEmpty()) {
                DatedLineChart(lines = comparisonLines)
            } else {
                Text("Wähle mindestens eine Metrik aus.")
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Volumen/Sätze pro Übung", style = MaterialTheme.typography.titleMedium)
                    if (exercises.isEmpty()) {
                        Text("Noch keine Übungen angelegt.")
                    } else {
                        ExerciseDetailPicker(
                            exercises = exercises,
                            selectedExerciseId = state.exerciseDetailExerciseId,
                            onExerciseChange = viewModel::onExerciseDetailChange,
                        )
                        DetailModeChips(
                            mode = state.exerciseDetailMode,
                            onModeChange = viewModel::onExerciseDetailModeChange,
                        )
                        val exerciseSeries = state.exerciseDetailSeries?.toChartLine(chartPalette[0])
                        if (exerciseSeries != null) {
                            DatedLineChart(lines = listOf(exerciseSeries))
                        } else {
                            Text("Wähle eine Übung aus.")
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sätze/Volumen pro Muskelgruppe", style = MaterialTheme.typography.titleMedium)
                    if (muscleGroups.isEmpty()) {
                        Text("Noch keine Muskelgruppen angelegt.")
                    } else {
                        // Chips rather than a dropdown: several groups at once is the point, and a
                        // dropdown that has to be reopened per group makes comparing them a chore.
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            muscleGroups.forEach { group ->
                                FilterChip(
                                    selected = group.id in state.muscleGroupDetailIds,
                                    onClick = { viewModel.onMuscleGroupDetailToggle(group) },
                                    label = { Text(group.name) },
                                )
                            }
                        }
                        DetailModeChips(
                            mode = state.muscleGroupDetailMode,
                            onModeChange = viewModel::onMuscleGroupDetailModeChange,
                        )
                        val muscleGroupLines = state.muscleGroupDetailSeries.mapIndexed { index, series ->
                            series.toChartLine(chartPalette[index % chartPalette.size])
                                // Volume and sets are both amounts done in a period, so their axis
                                // starts at zero — half as much work has to look like half as much.
                                .copy(zeroBased = true)
                        }
                        if (muscleGroupLines.isNotEmpty()) {
                            // One axis for all of them: the groups are in the same unit, and lines on
                            // separate scales would make an imbalance look like a match.
                            DatedLineChart(
                                lines = muscleGroupLines,
                                overlaid = true,
                                sharedScale = true,
                            )
                        } else {
                            Text("Wähle mindestens eine Muskelgruppe aus.")
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sätze/Volumen pro Bewegungsrichtung", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MovementDirection.entries.forEach { direction ->
                            FilterChip(
                                selected = state.movementDirectionDetail == direction,
                                onClick = { viewModel.onMovementDirectionDetailChange(direction) },
                                label = { Text(direction.label()) },
                            )
                        }
                    }
                    DetailModeChips(
                        mode = state.movementDirectionDetailMode,
                        onModeChange = viewModel::onMovementDirectionDetailModeChange,
                    )
                    val movementDirectionSeries = state.movementDirectionDetailSeries?.toChartLine(chartPalette[0])
                    if (movementDirectionSeries != null) {
                        DatedLineChart(lines = listOf(movementDirectionSeries))
                    } else {
                        Text("Wähle eine Bewegungsrichtung aus.")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDetailPicker(
    exercises: List<StrengthExercise>,
    selectedExerciseId: String?,
    onExerciseChange: (StrengthExercise) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val selectedName = exercises.find { it.id == selectedExerciseId }?.name.orEmpty()

    ExposedDropdownMenuBox(
        expanded = menuExpanded,
        onExpandedChange = { menuExpanded = it },
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
            readOnly = true,
            value = selectedName,
            onValueChange = {},
            label = { Text("Übung") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
        )
        ExposedDropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            exercises.forEach { exercise ->
                DropdownMenuItem(
                    text = { Text(exercise.name) },
                    onClick = {
                        onExerciseChange(exercise)
                        menuExpanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)

@Composable
private fun DetailModeChips(mode: DetailMode, onModeChange: (DetailMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = mode == DetailMode.SETS,
            onClick = { onModeChange(DetailMode.SETS) },
            label = { Text("Sätze") },
        )
        FilterChip(
            selected = mode == DetailMode.VOLUME,
            onClick = { onModeChange(DetailMode.VOLUME) },
            label = { Text("Volumen") },
        )
    }
}
