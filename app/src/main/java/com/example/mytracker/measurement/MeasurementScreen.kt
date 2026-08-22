package com.example.mytracker.measurement

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
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
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Body measurements are a tape measure reading; the whole screen is in cm. */
const val MEASUREMENT_UNIT = "cm"

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
                .verticalScroll(rememberScrollState())
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
                Text("Einträge hinzufügen", modifier = Modifier.padding(start = 8.dp))
            }

            AnimatedVisibility(visible = uiState.isAddExpanded) {
                AddPanel(
                    rows = uiState.rows,
                    canSave = uiState.canSave,
                    onDraftChange = viewModel::onDraftChange,
                    onShowHint = { hintTarget = it },
                    onSave = viewModel::save,
                )
            }

            ChartCard(
                uiState = uiState,
                onChartRangeChange = viewModel::onChartRangeChange,
                onToggleSite = viewModel::toggleSiteVisibility,
            )
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
 * The expandable logging form: one row per site, name left, field right. Every field starts on the
 * site's last value, so a session is "open, glance, save" and only the spots that actually changed
 * need typing.
 */
@Composable
private fun AddPanel(
    rows: List<MeasurementRow>,
    canSave: Boolean,
    onDraftChange: (String, String) -> Unit,
    onShowHint: (BodySite) -> Unit,
    onSave: () -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Wird für heute gespeichert. Vorbelegt ist der jeweils letzte Wert.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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
                        row.lastEpochDay?.let { day ->
                            Text(
                                "zuletzt ${DateUtils.localDateOfEpochDay(day).format(dateFormatter)}",
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
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Speichern") }
        }
    }
}

/**
 * All sites in one plot area ([DatedLineChart]'s overlaid mode), each on its own scale — a Taille
 * around 85 cm and an Oberarm around 35 cm would otherwise squash each other. The chips exist
 * because the overlay puts one min/max column per visible series in the left gutter: past a handful
 * of sites that gutter eats the chart, so hiding what you're not comparing right now is part of
 * reading it.
 */
@Composable
private fun ChartCard(
    uiState: MeasurementUiState,
    onChartRangeChange: (ChartRange) -> Unit,
    onToggleSite: (String) -> Unit,
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

            if (uiState.chartableSites.isEmpty()) {
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
                uiState.chartableSites.forEach { series ->
                    FilterChip(
                        selected = series.siteId !in uiState.hiddenSiteIds,
                        onClick = { onToggleSite(series.siteId) },
                        label = { Text(series.name) },
                    )
                }
            }

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
                },
                overlaid = true,
                panelHeight = 240,
            )
        }
    }
}
