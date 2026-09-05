package com.example.mytracker.measurement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.core.ui.ConfirmDeleteDialog
import com.example.mytracker.core.metrics.ChartRange
import com.example.mytracker.core.metrics.label
import com.example.mytracker.core.ui.ChartLine
import com.example.mytracker.core.ui.DatedLineChart
import com.example.mytracker.core.ui.AppFilterChip
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.fluid.fluidPalette
import androidx.compose.foundation.layout.Box
import com.example.mytracker.ui.theme.AppDomain
import com.example.mytracker.ui.theme.topAppBarColors
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * A measurement can only have been taken by now. A future date would put a point past the end of
 * every chart in the app and quietly become "the last value" every field prefills from.
 */
@OptIn(ExperimentalMaterial3Api::class)
private object PastDatesOnly : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis / MILLIS_PER_DAY <= DateUtils.todayEpochDay()

    override fun isSelectableYear(year: Int): Boolean =
        year <= DateUtils.localDateOfEpochDay(DateUtils.todayEpochDay()).year
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementScreen(
    onOpenDrawer: () -> Unit,
    onOpenSiteManagement: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeasurementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var hintTarget by remember { mutableStateOf<BodySite?>(null) }
    var deleteTarget by remember { mutableStateOf<MeasurementDayRow?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // The Einträge list sits at the bottom and the editor at the top, so opening a session for
    // correction happens off-screen from where it was tapped. Without this, tapping a row looks
    // like nothing happened at all.
    LaunchedEffect(uiState.editingEpochDay) {
        if (uiState.isAddExpanded) scrollState.animateScrollTo(0)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.MEASUREMENT.topAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                title = { Text("Maße") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Mehr")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Körperstellen verwalten") },
                            onClick = {
                                showMenu = false
                                onOpenSiteManagement()
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!uiState.hasSites) {
                EmptyLibraryNotice(onOpenSiteManagement = onOpenSiteManagement)
                return@Column
            }

            Button(
                onClick = viewModel::toggleAddExpanded,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    if (uiState.isAddExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
                Text(
                    if (uiState.isEditingExisting) "Eintrag bearbeiten" else "Einträge hinzufügen",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            AnimatedVisibility(visible = uiState.isAddExpanded) {
                AddPanel(
                    uiState = uiState,
                    onDraftChange = viewModel::onDraftChange,
                    onShowHint = { hintTarget = it },
                    onSave = viewModel::save,
                    onResetToToday = viewModel::resetToToday,
                    onPickDate = { showDatePicker = true },
                )
            }

            ChartCard(
                uiState = uiState,
                onChartRangeChange = viewModel::onChartRangeChange,
                onToggleSite = viewModel::toggleSiteVisibility,
                onToggleWeight = viewModel::toggleWeightShown,
                onRatioNumeratorChange = viewModel::onRatioNumeratorChange,
                onRatioDenominatorChange = viewModel::onRatioDenominatorChange,
            )

            HistoryCard(
                history = uiState.history,
                editingEpochDay = uiState.editingEpochDay.takeIf { uiState.isAddExpanded },
                onEditDay = viewModel::editDay,
                onDeleteDay = { deleteTarget = it },
            )
        }
    }

    deleteTarget?.let { row ->
        ConfirmDeleteDialog(
            title = "Eintrag löschen?",
            // Named rather than counted: "3 Werte" does not tell you whether the one you meant to
            // keep is among them.
            text = "${row.dateText} — ${row.summary} werden gelöscht.",
            onConfirm = { viewModel.deleteDay(row.epochDay) },
            onDismiss = { deleteTarget = null },
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.editingEpochDay * MILLIS_PER_DAY,
            selectableDates = PastDatesOnly,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.editDay(it / MILLIS_PER_DAY) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") } },
        ) {
            DatePicker(state = pickerState)
        }
    }

    hintTarget?.let { site ->
        AlertDialog(
            onDismissRequest = { hintTarget = null },
            confirmButton = { TextButton(onClick = { hintTarget = null }) { Text("OK") } },
            title = { Text(site.name) },
            text = {
                Text(
                    site.measuringHint
                        ?: "Für diese Körperstelle ist kein Messhinweis hinterlegt. " +
                        "Du kannst ihn unter \"Körperstellen verwalten\" ergänzen.",
                )
            },
        )
    }
}

@Composable
private fun EmptyLibraryNotice(onOpenSiteManagement: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Noch keine Körperstellen angelegt.", style = MaterialTheme.typography.titleSmall)
            Text(
                "Lege zuerst an, was du messen willst — z.B. \"Oberarm links\" — samt Hinweis, " +
                    "wie genau du dort misst.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onOpenSiteManagement) { Text("Körperstellen verwalten") }
        }
    }
}

/**
 * The one form for both logging and correcting: one row per site, name left, field right.
 *
 * Which of the two it is comes from the day it is pointed at. A fresh day starts every field on the
 * site's last value, so a session is "open, glance, save" and only the spots that actually changed
 * need typing. A day that already holds measurements starts on *those*, and a field cleared there
 * deletes that value — see [measurementRows].
 */
@Composable
private fun AddPanel(
    uiState: MeasurementUiState,
    onDraftChange: (String, String) -> Unit,
    onShowHint: (BodySite) -> Unit,
    onSave: () -> Unit,
    onResetToToday: () -> Unit,
    onPickDate: () -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN) }
    val editingDateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN) }
    val rows = uiState.rows
    val editingDate = DateUtils.localDateOfEpochDay(uiState.editingEpochDay).format(editingDateFormatter)
    val isToday = uiState.editingEpochDay == DateUtils.todayEpochDay()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // The date is stated outright, never implied by "heute": the same form writes to a day
            // chosen minutes ago from the list below, and a wrong date is invisible once saved.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    // Tappable, because a session measured yesterday and typed in today has no
                    // other way in: without it the editor can only ever write to today or correct a
                    // day that already exists.
                    Row(
                        modifier = Modifier.clickable(onClick = onPickDate),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (isToday) "Wird für heute gespeichert" else "Wird für $editingDate gespeichert",
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = TextDecoration.Underline,
                        )
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = "Datum wählen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp).size(18.dp),
                        )
                    }
                    Text(
                        if (uiState.isEditingExisting) {
                            "Vorbelegt ist der gespeicherte Wert. Feld leeren löscht ihn."
                        } else {
                            "Vorbelegt ist der jeweils letzte Wert."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!isToday) {
                    TextButton(onClick = onResetToToday) { Text("Heute") }
                }
            }

            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The name is the hint's affordance: underlined and marked with an info icon,
                    // because "tap the label" is not otherwise discoverable.
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onShowHint(row.site) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                row.site.name,
                                style = MaterialTheme.typography.bodyLarge,
                                textDecoration = TextDecoration.Underline,
                            )
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "Messhinweis anzeigen",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                        row.referenceEpochDay?.let { day ->
                            val since = DateUtils.localDateOfEpochDay(day).format(dateFormatter)
                            // The change updates as the field is typed in, which is the moment it
                            // is worth knowing — "is that more or less than last time" is the
                            // question being answered with the tape still in hand.
                            val change = row.deltaCm?.takeIf { it != 0.0 }?.signedCm()
                            Text(
                                if (change == null) "zuletzt $since" else "zuletzt $since · $change",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = row.draft,
                        onValueChange = { onDraftChange(row.site.id, it) },
                        label = { Text(MEASUREMENT_UNIT) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = if (index == rows.lastIndex) ImeAction.Done else ImeAction.Next,
                        ),
                        modifier = Modifier.width(120.dp),
                    )
                }
            }

            Button(
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Speichern") }
        }
    }
}

/**
 * All sites in one plot area ([DatedLineChart]'s overlaid mode) on **one** shared cm scale.
 *
 * Per-series scales flattered every line: each one filled the panel, so a waist that moved 2 cm and
 * an upper arm that moved 2 mm drew the same dramatic slope, and the two could not be read against
 * each other at all. One scale in centimetres is the honest picture — a small change looks small —
 * and it also replaces the gutter's column-per-series with a single labelled axis, which is what
 * makes it possible to read a value off the grid rather than only off the crosshair.
 *
 * The chips stay: on one scale, sites that sit far apart squeeze each other's detail, so switching
 * off what you're not comparing right now is still part of reading the chart.
 */
@Composable
private fun ChartCard(
    uiState: MeasurementUiState,
    onChartRangeChange: (ChartRange) -> Unit,
    onToggleSite: (String) -> Unit,
    onToggleWeight: () -> Unit,
    onRatioNumeratorChange: (String?) -> Unit,
    onRatioDenominatorChange: (String?) -> Unit,
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

            // The chips and the plot stay put even with nothing switched on and nothing measured.
            // The chart used to be replaced by a line of text, which pulled the rest of the screen
            // up and took the chips that switch a series back on with it — the reader lost the
            // control they needed to undo what they had just done.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                uiState.chartableSites.forEach { series ->
                    AppFilterChip(
                        selected = series.siteId !in uiState.hiddenSiteIds,
                        label = series.name,
                        onClick = { onToggleSite(series.siteId) },
                        color = palette[series.paletteIndex.mod(palette.size)],
                    )
                }
            }

            ExtraSeriesControls(
                uiState = uiState,
                onToggleWeight = onToggleWeight,
                onRatioNumeratorChange = onRatioNumeratorChange,
                onRatioDenominatorChange = onRatioDenominatorChange,
            )

            DatedLineChart(
                lines = uiState.series.map { series ->
                    ChartLine(
                        label = series.name,
                        unit = MEASUREMENT_UNIT,
                        color = palette[series.paletteIndex.mod(palette.size)],
                        points = series.points,
                        // Never zero-based: a 34–36 cm arm on a 0-axis is a flat line.
                        zeroBased = false,
                    )
                    // The two riders each get their own axis, which is what a distinct unit buys
                    // here (see DatedLineChart's sharedScale) — kilograms and centimetres on one
                    // scale would flatten whichever of them runs smaller.
                } + listOfNotNull(
                    uiState.weightSeries.takeIf { it.isNotEmpty() }?.let { points ->
                        ChartLine(
                            label = "Gewicht",
                            unit = uiState.weightUnitLabel,
                            color = AppDomain.WEIGHT.accent(),
                            points = points,
                            zeroBased = false,
                        )
                    },
                    uiState.ratioLabel?.takeIf { uiState.ratioSeries.isNotEmpty() }?.let { label ->
                        ChartLine(
                            label = label,
                            unit = "%",
                            color = palette[palette.lastIndex],
                            points = uiState.ratioSeries,
                            zeroBased = false,
                        )
                    },
                ),
                overlaid = true,
                sharedScale = true,
                panelHeight = 240,
            )
        }
    }
}

/**
 * The two series that are not body sites: body weight, and one site divided by another.
 *
 * Both answer questions the cm lines cannot. A waist that shrank while the scale did nothing is a
 * different result than one that shrank along with five kilos, and a ratio holds still when both
 * spots grow together — which is the case a tape measure alone reads as progress everywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtraSeriesControls(
    uiState: MeasurementUiState,
    onToggleWeight: () -> Unit,
    onRatioNumeratorChange: (String?) -> Unit,
    onRatioDenominatorChange: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Körpergewicht einblenden",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(checked = uiState.isWeightShown, onCheckedChange = { onToggleWeight() })
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Verhältnis",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SitePicker(
                sites = uiState.chartableSites,
                selectedSiteId = uiState.ratioNumeratorSiteId,
                onSelect = onRatioNumeratorChange,
            )
            Text("/", style = MaterialTheme.typography.bodyMedium)
            SitePicker(
                sites = uiState.chartableSites,
                selectedSiteId = uiState.ratioDenominatorSiteId,
                onSelect = onRatioDenominatorChange,
            )
        }
        Text(
            // Which two spots is the user's call: the sites are theirs, and nothing in one says
            // whether it is a waist. Taille/Hüfte is named because it is the one with a meaning
            // attached, not because the app can find it.
            "Zwei Körperstellen im Verhältnis, in Prozent — z.B. Taille / Hüfte. " +
                "Nur an Tagen, an denen beide gemessen wurden.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One end of the Verhältnis. "—" is a real choice: it is how the line is switched off again. */
@Composable
private fun SitePicker(
    sites: List<MeasurementSeries>,
    selectedSiteId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(sites.firstOrNull { it.siteId == selectedSiteId }?.name ?: "—")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("—") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            sites.forEach { series ->
                DropdownMenuItem(
                    text = { Text(series.name) },
                    onClick = {
                        onSelect(series.siteId)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * The logged sessions: a day per row, tap to correct it, with the values named so a row can be
 * recognised without opening it.
 *
 * A day rather than a single value, because that is how measuring happens — tape in hand, several
 * spots in one go — and it is the unit the editor writes in. The row that is currently open is
 * marked, so a correction in progress can be traced back to what it will overwrite.
 */
@Composable
private fun HistoryCard(
    history: List<MeasurementDayRow>,
    editingEpochDay: Long?,
    onEditDay: (Long) -> Unit,
    onDeleteDay: (MeasurementDayRow) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Einträge", style = MaterialTheme.typography.titleSmall)

            if (history.isEmpty()) {
                Text(
                    "Noch keine Messungen erfasst.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            history.forEach { row ->
                val isOpen = row.epochDay == editingEpochDay
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onEditDay(row.epochDay) }
                            .padding(vertical = 6.dp),
                    ) {
                        Text(
                            row.dateText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isOpen) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Text(
                            row.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onDeleteDay(row) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eintrag löschen")
                    }
                }
            }
        }
    }
}
