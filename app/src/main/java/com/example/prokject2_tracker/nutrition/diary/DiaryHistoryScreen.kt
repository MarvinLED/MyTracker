package com.example.prokject2_tracker.nutrition.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.prokject2_tracker.core.metrics.label
import com.example.prokject2_tracker.core.ui.DatedLineChart

/**
 * How the Tagebuch's numbers moved over time: every selected nutrient's target and intake in one
 * plot area, plus body weight.
 *
 * The lines are **overlaid, each on its own scale**, because the question here is whether two things
 * moved together, not what either of them measures — a shared axis would flatten salt to a flat line
 * next to calories. [DatedLineChart] discloses each series' own range so the crossings can't be
 * misread as meaningful.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Verlauf") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
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
                DiaryHistoryRanges.forEach { range ->
                    FilterChip(
                        selected = range == state.chartRange,
                        onClick = { viewModel.onChartRangeChange(range) },
                        label = { Text(range.label()) },
                    )
                }
            }

            if (state.lines.isEmpty()) {
                Text(
                    "Wähle unten aus, was angezeigt werden soll.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                DatedLineChart(lines = state.lines, panelHeight = 280, overlaid = true)
                // Spelled out rather than implied: once the range coarsens, a point is an average
                // day of its week or month, and the goal line it is read against is too.
                Text(
                    "Ein Punkt: Durchschnitt pro ${state.granularity.label()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SeriesPicker(
                expanded = state.seriesPickerExpanded,
                selected = state.selectedSeries,
                onExpandedChange = viewModel::onSeriesPickerExpandedChange,
                onToggle = viewModel::onSeriesToggle,
            )
        }
    }
}

@Composable
private fun SeriesPicker(
    expanded: Boolean,
    selected: Set<DiaryHistorySeries>,
    onExpandedChange: (Boolean) -> Unit,
    onToggle: (DiaryHistorySeries, Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Anzeige",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Auswahl einklappen" else "Auswahl ausklappen",
                )
            }

            if (expanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    NutrientRows.forEach { row ->
                        val (goal, actual) = row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SeriesLabel(rowLabel(row), goal, modifier = Modifier.weight(1f))
                            LabeledCheckbox("Soll", goal in selected) { onToggle(goal, it) }
                            LabeledCheckbox("Ist", actual in selected) { onToggle(actual, it) }
                        }
                    }

                    // Gewicht has no Soll to sit beside — the app has no weight goal — so it gets a
                    // line of its own rather than a half-empty nutrient row.
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    val weight = DiaryHistorySeries.WEIGHT
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SeriesLabel(weight.label, weight, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = weight in selected,
                            onCheckedChange = { onToggle(weight, it) },
                        )
                    }
                }
            }
        }
    }
}

/** The row's name behind its chart colour, so a line can be traced back to its checkbox. */
@Composable
private fun SeriesLabel(text: String, series: DiaryHistorySeries, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(series.color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LabeledCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
