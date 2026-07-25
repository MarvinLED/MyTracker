package com.example.prokject2_tracker.weight

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import com.example.prokject2_tracker.core.datastore.WeightUnit
import com.example.prokject2_tracker.core.ui.ChartLine
import com.example.prokject2_tracker.core.ui.DatedLineChart
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.formatDecimal
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import com.example.prokject2_tracker.ui.theme.AppDomain
import com.example.prokject2_tracker.ui.theme.topAppBarColors
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeightViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN) }
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.editingEpochDay, uiState.editingDisplayValue) {
        inputText = uiState.editingDisplayValue?.formatDecimal(1).orEmpty()
    }

    val unitLabel = when (uiState.weightUnit) {
        WeightUnit.KG -> "kg"
        WeightUnit.LB -> "lb"
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.WEIGHT.topAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                title = { Text("Gewicht") },
                actions = {
                    IconButton(onClick = viewModel::resetToToday) {
                        Icon(Icons.Filled.Today, contentDescription = "Heute")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                DateUtils.localDateOfEpochDay(uiState.editingEpochDay).format(dateFormatter),
                style = MaterialTheme.typography.titleMedium,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Gewicht ($unitLabel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        inputText.toLocaleDoubleOrNull()?.let { viewModel.save(it) }
                    },
                    enabled = inputText.toLocaleDoubleOrNull() != null,
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "Speichern")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Verlauf", style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        WeightChartRange.entries.forEachIndexed { index, range ->
                            SegmentedButton(
                                selected = uiState.chartRange == range,
                                onClick = { viewModel.onChartRangeChange(range) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = WeightChartRange.entries.size,
                                ),
                                label = { Text(range.label(), style = MaterialTheme.typography.labelMedium) },
                            )
                        }
                    }
                    DatedLineChart(
                        lines = listOf(
                            ChartLine(
                                label = "Gewicht",
                                unit = unitLabel,
                                color = AppDomain.WEIGHT.accent(),
                                points = uiState.chartPoints,
                                // Never zero-based: a 78–80 kg range on a 0-axis is a flat line.
                                zeroBased = false,
                            ),
                        ),
                    )
                }
            }

            if (uiState.history.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Noch keine Gewichtseinträge.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(uiState.history, key = { it.entry.id }) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.selectEntry(row.entry) },
                            ) {
                                Text(
                                    DateUtils.localDateOfEpochDay(row.entry.epochDay).format(dateFormatter),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    "${row.displayValue.formatDecimal(1)} $unitLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            IconButton(onClick = { viewModel.delete(row.entry) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Löschen")
                            }
                        }
                    }
                }
            }
        }
    }
}
