package com.example.mytracker.bloodpressure

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.core.metrics.ChartRange
import com.example.mytracker.core.metrics.label
import com.example.mytracker.core.ui.ChartLine
import com.example.mytracker.core.ui.DatedLineChart
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.fluid.fluidPalette
import com.example.mytracker.ui.theme.AppDomain
import com.example.mytracker.ui.theme.topAppBarColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Blood pressure is always in mmHg — no unit choice anywhere on this screen. */
const val BLOOD_PRESSURE_UNIT = "mmHg"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodPressureScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BloodPressureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.BLOOD_PRESSURE.topAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                title = { Text("Blutdruck") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = viewModel::toggleAddExpanded,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    if (uiState.isAddExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
                Text("Eintrag hinzufügen", modifier = Modifier.padding(start = 8.dp))
            }

            AnimatedVisibility(visible = uiState.isAddExpanded) {
                AddPanel(
                    uiState = uiState,
                    onDateChange = viewModel::onDateChange,
                    onTimeOfDayChange = viewModel::onTimeOfDayChange,
                    onSystolicChange = viewModel::onSystolicChange,
                    onDiastolicChange = viewModel::onDiastolicChange,
                    onCommentChange = viewModel::onCommentChange,
                    onSave = viewModel::save,
                )
            }

            ChartCard(
                uiState = uiState,
                onChartRangeChange = viewModel::onChartRangeChange,
                onToggleSeries = viewModel::toggleSeriesVisibility,
            )

            HistoryCard(rows = uiState.history, onDelete = viewModel::delete)
        }
    }
}

/**
 * The logging form: day, time of day, the two fixed values, and a comment. Both value fields open on
 * the last reading *at or before* the selected day and matching its time of day — so switching to
 * "Abends" offers last evening's numbers, and picking a day that already holds a reading shows that
 * reading instead of another day's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPanel(
    uiState: BloodPressureUiState,
    onDateChange: (Long) -> Unit,
    onTimeOfDayChange: (BloodPressureTimeOfDay) -> Unit,
    onSystolicChange: (String) -> Unit,
    onDiastolicChange: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN) }
    val fullDateFormatter = remember { DateTimeFormatter.ofPattern("EEE, d. MMMM yyyy", Locale.GERMAN) }
    var showDatePicker by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                Text(
                    DateUtils.localDateOfEpochDay(uiState.epochDay).format(fullDateFormatter),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                BloodPressureTimeOfDay.entries.forEachIndexed { index, timeOfDay ->
                    SegmentedButton(
                        selected = uiState.timeOfDay == timeOfDay,
                        onClick = { onTimeOfDayChange(timeOfDay) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = BloodPressureTimeOfDay.entries.size,
                        ),
                        label = { Text(timeOfDay.label()) },
                    )
                }
            }

            uiState.prefilledFrom?.let { source ->
                Text(
                    if (uiState.isEditingExisting) {
                        "Für diesen Zeitpunkt ist bereits eine Messung gespeichert. " +
                            "Speichern überschreibt sie."
                    } else {
                        "Vorbelegt mit der Messung vom " +
                            "${DateUtils.localDateOfEpochDay(source.epochDay).format(dateFormatter)} " +
                            "(${source.timeOfDay.label().lowercase()})."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.isEditingExisting) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = uiState.systolicDraft,
                    onValueChange = onSystolicChange,
                    label = { Text("Systolisch") },
                    suffix = { Text(BLOOD_PRESSURE_UNIT) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = uiState.diastolicDraft,
                    onValueChange = onDiastolicChange,
                    label = { Text("Diastolisch") },
                    suffix = { Text(BLOOD_PRESSURE_UNIT) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = uiState.commentDraft,
                onValueChange = onCommentChange,
                label = { Text("Kommentar (optional)") },
                placeholder = { Text("z.B. direkt nach dem Aufstehen") },
                minLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Speichern") }
        }
    }

    if (showDatePicker) {
        LogDatePickerDialog(
            epochDay = uiState.epochDay,
            onPick = {
                onDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

/**
 * The calendar for the logged day. Future days are not selectable — a blood-pressure reading is
 * something that was measured, so a date after today can only be a slip.
 *
 * [DatePickerState] works in UTC midnights while the app stores local epoch days, so both ends are
 * converted explicitly rather than by dividing millis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogDatePickerDialog(epochDay: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val today = DateUtils.todayEpochDay()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = epochDay.epochDayToUtcMillis(),
        selectableDates = remember(today) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis.utcMillisToEpochDay() <= today

                override fun isSelectableYear(year: Int): Boolean =
                    year <= DateUtils.localDateOfEpochDay(today).year
            }
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onPick(it.utcMillisToEpochDay()) } },
                enabled = state.selectedDateMillis != null,
            ) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    ) {
        DatePicker(state = state)
    }
}

private fun Long.epochDayToUtcMillis(): Long =
    LocalDate.ofEpochDay(this).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.utcMillisToEpochDay(): Long =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()

/**
 * All four series in one plot area, each on its own scale, with chips to hide the ones you're not
 * comparing — the same arrangement as the Maße chart, for the same reason: the overlay puts one
 * min/max column per visible series in the left gutter.
 */
@Composable
private fun ChartCard(
    uiState: BloodPressureUiState,
    onChartRangeChange: (ChartRange) -> Unit,
    onToggleSeries: (String) -> Unit,
) {
    val palette = fluidPalette()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Verlauf", style = MaterialTheme.typography.titleSmall)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ChartRange.entries.forEachIndexed { index, range ->
                    SegmentedButton(
                        selected = uiState.chartRange == range,
                        onClick = { onChartRangeChange(range) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ChartRange.entries.size),
                        label = { Text(range.label(), style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }

            if (uiState.chartableSeries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Noch keine Messungen erfasst.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                uiState.chartableSeries.forEach { series ->
                    FilterChip(
                        selected = series.key !in uiState.hiddenSeriesKeys,
                        onClick = { onToggleSeries(series.key) },
                        label = { Text(series.label) },
                    )
                }
            }

            DatedLineChart(
                lines = uiState.series.map { series ->
                    ChartLine(
                        label = series.label,
                        unit = BLOOD_PRESSURE_UNIT,
                        color = palette[series.paletteIndex.mod(palette.size)],
                        points = series.points,
                        // Never zero-based: a 118–138 mmHg band on a 0-axis is a flat line.
                        zeroBased = false,
                    )
                },
                overlaid = true,
                panelHeight = 240,
            )
        }
    }
}

/**
 * The last readings with their comments. The chart can only show numbers, so without this the
 * comment would be write-only — and the comment is what explains an outlier.
 */
@Composable
private fun HistoryCard(rows: List<BloodPressureHistoryRow>, onDelete: (BloodPressureEntry) -> Unit) {
    if (rows.isEmpty()) return
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, d. MMM", Locale.GERMAN) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Letzte Messungen", style = MaterialTheme.typography.titleSmall)
            rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${DateUtils.localDateOfEpochDay(row.entry.epochDay).format(dateFormatter)} · " +
                                row.entry.timeOfDay.label(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(row.values, style = MaterialTheme.typography.bodyLarge)
                        row.entry.comment?.let { comment ->
                            Text(comment, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    IconButton(onClick = { onDelete(row.entry) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Löschen")
                    }
                }
            }
        }
    }
}
