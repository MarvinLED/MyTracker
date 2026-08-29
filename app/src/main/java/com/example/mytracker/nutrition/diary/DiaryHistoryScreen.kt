package com.example.mytracker.nutrition.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.core.metrics.label
import com.example.mytracker.core.ui.DatedLineChart
import com.example.mytracker.core.ui.ScatterChart
import com.example.mytracker.core.ui.ScatterPoint
import com.example.mytracker.core.util.DateUtils
import java.time.format.DateTimeFormatter
import java.util.Locale

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
            // A FlowRow, not a Row: four chips do not fit a narrow screen on one line, and the
            // Heute toggle wrapping under the ranges beats it being cut off at the edge.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DiaryHistoryRanges.forEach { range ->
                    FilterChip(
                        selected = range == state.chartRange,
                        onClick = { viewModel.onChartRangeChange(range) },
                        label = { Text(range.label()) },
                    )
                }
                // The two chips carrying a tick when they are on, so they read as switches rather
                // than as further spans sitting beside Monat, Jahr und Insgesamt.
                ToggleChip(
                    label = "Heute",
                    selected = state.showToday,
                    onToggle = { viewModel.onShowTodayChange(!state.showToday) },
                )
                ToggleChip(
                    label = "Kalorien ↔ Gewicht",
                    selected = state.weeklyComparison,
                    onToggle = { viewModel.onWeeklyComparisonChange(!state.weeklyComparison) },
                )
            }

            if (state.weeklyComparison) {
                WeeklyComparison(summary = state.weeklyEnergy)
            } else if (state.lines.isEmpty()) {
                Text(
                    "Wähle unten aus, was angezeigt werden soll.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                DatedLineChart(
                    lines = state.lines,
                    panelHeight = 280,
                    overlaid = true,
                    sharedScale = state.sharedScale,
                )
                // Spelled out rather than implied: once the range coarsens, a point is an average
                // day of its week or month, and the goal line it is read against is too. The Ø mark
                // is explained here too — the legend has room for the sign, not for what it means.
                val averageNote = if (state.selectedSeries.any { it.kind == DiaryHistorySeriesKind.AVERAGE }) {
                    " · Ø: Durchschnitt der Kalenderwoche"
                } else {
                    ""
                }
                Text(
                    "Ein Punkt: Durchschnitt pro ${state.granularity.label()}$averageNote",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Hidden while the comparison is up: it configures the lines of the other chart, and a
            // table of checkboxes that changes nothing on screen reads as a broken control.
            if (!state.weeklyComparison) {
                SeriesPicker(
                    expanded = state.seriesPickerExpanded,
                    selected = state.selectedSeries,
                    onExpandedChange = viewModel::onSeriesPickerExpandedChange,
                    onToggle = viewModel::onSeriesToggle,
                )
            }
        }
    }
}

/**
 * Kalorien gegen Gewicht, eine Woche je Punkt.
 *
 * The Verlauf cannot answer whether eating more moved the weight: on one axis the weight (±2 %) is a
 * flat line beside the calories (±30 %), and on scales of their own both fill the panel, so any two
 * series look as though they move together. Dropping the time axis puts the relationship itself on
 * screen — how far right a week sits is what was eaten, how far up is what the weight did.
 *
 * The upright line is the Kalorienziel over the same window, so it can be read against where the
 * cloud actually crosses "Gewicht gehalten".
 */
@Composable
private fun WeeklyComparison(summary: WeeklyEnergySummary?) {
    val weekFormatter = remember { DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN) }
    val points = summary?.points.orEmpty()

    ScatterChart(
        points = points.map { week ->
            ScatterPoint(
                x = week.kcalPerDay,
                y = week.weightChangeKg,
                label = "Woche ab ${DateUtils.localDateOfEpochDay(week.weekStart).format(weekFormatter)}" +
                    " · ${week.loggedDays} Tage erfasst",
            )
        },
        xAxisLabel = "Ø Kalorien pro Tag",
        yAxisLabel = "Gewicht pro Woche",
        xUnit = "kcal/Tag",
        yUnit = "kg",
        pointColor = WeightColor,
        // No line under four weeks: a straight line goes through any three points, and drawn there
        // it would claim a finding the data cannot carry.
        fit = summary?.fit?.takeIf { summary.hasEnoughWeeks },
        goalMarker = summary?.goalKcalPerDay,
        markerColor = KcalColor,
    )

    if (summary != null && summary.hasEnoughWeeks) {
        Text(
            summary.relationshipText(),
            style = MaterialTheme.typography.bodyMedium,
        )
        listOfNotNull(summary.slopeText(), summary.maintenanceText()).forEach { line ->
            Text(line, style = MaterialTheme.typography.bodyMedium)
        }
    } else if (points.isNotEmpty()) {
        Text(
            "Zu wenige vollständige Wochen (${points.size}) — wähle Jahr oder Insgesamt.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Text(
        "Ein Punkt ist eine volle Kalenderwoche mit mindestens $MIN_LOGGED_DAYS_PER_WEEK erfassten " +
            "Tagen, hellere Punkte liegen weiter zurück. Die Gewichtsänderung ist Wochenmittel " +
            "gegen Wochenmittel — eine einzelne Woche trägt trotzdem Wasser und Salz.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * A chip that is on or off rather than one of a set. The tick is what tells it apart from the range
 * chips beside it, which look identical when selected but mean "instead of the others".
 */
@Composable
private fun ToggleChip(label: String, selected: Boolean, onToggle: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null) }
        } else {
            null
        },
    )
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
                    // Column headings instead of a label beside every checkbox: three labelled
                    // checkboxes per row leave "Kohlenhydrate" no width to be read in.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.weight(1f))
                        ColumnHeaders.forEach { heading ->
                            Text(
                                heading,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(CheckboxColumnWidth),
                            )
                        }
                    }

                    NutrientRows.forEach { row ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SeriesLabel(row.label, row.actual, modifier = Modifier.weight(1f))
                            listOf(row.goal, row.actual, row.average).forEach { series ->
                                CheckboxCell(series in selected) { onToggle(series, it) }
                            }
                        }
                    }

                    // Gewicht has no Soll to sit beside — the app has no weight goal — and no Ø
                    // either, so it gets a line of its own rather than a mostly empty nutrient row.
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    val weight = DiaryHistorySeries.WEIGHT
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SeriesLabel(weight.label, weight, modifier = Modifier.weight(1f))
                        // In the Ist column: it is a measured value like the nutrients' Ist.
                        Spacer(Modifier.width(CheckboxColumnWidth))
                        CheckboxCell(weight in selected) { onToggle(weight, it) }
                        Spacer(Modifier.width(CheckboxColumnWidth))
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

/**
 * The three checkbox columns. "Ø" rather than "Durchschnitt": the heading has to fit a checkbox's
 * width, and the same mark labels the line in the chart.
 */
private val ColumnHeaders = listOf("Soll", "Ist", "Ø")

private val CheckboxColumnWidth = 48.dp

/** A checkbox in its column, so the boxes line up under their heading down the whole table. */
@Composable
private fun CheckboxCell(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(modifier = Modifier.width(CheckboxColumnWidth), contentAlignment = Alignment.Center) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}
