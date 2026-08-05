package com.example.prokject2_tracker.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.prokject2_tracker.core.ui.TimeOfDayField
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.formatDuration
import com.example.prokject2_tracker.core.util.formatMinuteOfDay
import com.example.prokject2_tracker.ui.theme.AppDomain
import com.example.prokject2_tracker.ui.theme.statusColor
import com.example.prokject2_tracker.ui.theme.topAppBarColors
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val dayFormatter = DateTimeFormatter.ofPattern("EEE, d. MMMM", Locale.GERMAN)
private val shortDayFormatter = DateTimeFormatter.ofPattern("EEE, d. MMM", Locale.GERMAN)

/**
 * One night per screenful: the form always edits the night that ended on the selected day, so paging
 * the date is how you both review and correct. Everything below the form is that night in context —
 * how it stands against the goals, and the nights before it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    onOpenDrawer: () -> Unit,
    onOpenTagManagement: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SleepViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showMorningPopup by remember { mutableStateOf(false) }
    var wakeTimeMinutes by remember { mutableStateOf(uiState.endMinuteOfDay ?: 7 * 60) }

    // Show morning popup if opening between 4 AM - 12 noon and it's the today/next night
    LaunchedEffect(uiState.epochDay) {
        val now = LocalTime.now()
        val isInMorningHours = now >= LocalTime.of(4, 0) && now < LocalTime.of(12, 0)
        val todayEpochDay = DateUtils.todayEpochDay()
        val showingTodayOrTomorrow = uiState.epochDay == todayEpochDay || uiState.epochDay == todayEpochDay + 1

        if (isInMorningHours && showingTodayOrTomorrow && uiState.isExistingNight) {
            showMorningPopup = true
            wakeTimeMinutes = uiState.endMinuteOfDay ?: 7 * 60
        }
    }

    // Morning wake-time popup
    if (showMorningPopup) {
        var tempWakeTime by remember { mutableStateOf(wakeTimeMinutes) }

        AlertDialog(
            onDismissRequest = { showMorningPopup = false },
            title = { Text("Wann bist du heute aufgestanden?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TimeOfDayField(
                        label = "Aufwach-Zeit",
                        value = tempWakeTime,
                        onValueChange = { tempWakeTime = it },
                        defaultMinuteOfDay = 7 * 60,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEndChange(tempWakeTime)
                        showMorningPopup = false
                    }
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showMorningPopup = false }
                ) {
                    Text("Überspringen")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.SLEEP.topAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
                title = { Text("Schlaf") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Mehr")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Tags verwalten") },
                            onClick = {
                                showMenu = false
                                onOpenTagManagement()
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
        ) {
            item(key = "form") {
                NightForm(
                    state = uiState,
                    onPreviousDay = viewModel::goToPreviousDay,
                    onNextDay = viewModel::goToNextDay,
                    onStartChange = viewModel::onStartChange,
                    onEndChange = viewModel::onEndChange,
                    onLastMealChange = viewModel::onLastMealChange,
                    onClearLastMeal = viewModel::clearLastMeal,
                    onFitnessChange = viewModel::onFitnessChange,
                    onTagToggle = viewModel::onTagToggle,
                    onTagInputChange = viewModel::onTagInputChange,
                    onAddTag = viewModel::addTagFromInput,
                    onSave = viewModel::save,
                    onModeChange = viewModel::onModeChange,
                    onDidNotSleepChange = viewModel::onDidNotSleepChange,
                )
            }
            if (uiState.goalStatuses.isNotEmpty()) {
                item(key = "goals") { GoalCard(statuses = uiState.goalStatuses) }
            }
            if (uiState.history.isNotEmpty()) {
                item(key = "history-title") {
                    Text("Frühere Nächte", style = MaterialTheme.typography.titleSmall)
                }
                items(uiState.history, key = { it.entry.id }) { row ->
                    HistoryRow(row = row, onDelete = { viewModel.deleteNight(row.entry) })
                }
            }
        }
    }
}

@Composable
private fun NightForm(
    state: SleepUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
    onLastMealChange: (Int) -> Unit,
    onClearLastMeal: () -> Unit,
    onFitnessChange: (Int) -> Unit,
    onTagToggle: (String) -> Unit,
    onTagInputChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onSave: () -> Unit,
    onModeChange: (SleepFormMode) -> Unit,
    onDidNotSleepChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Mode chips: Nacht vs. Mittagsschlaf
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.mode == SleepFormMode.NIGHT,
                    onClick = { onModeChange(SleepFormMode.NIGHT) },
                    label = { Text("Nacht") },
                )
                FilterChip(
                    selected = state.mode == SleepFormMode.NAP,
                    onClick = { onModeChange(SleepFormMode.NAP) },
                    label = { Text("Mittagsschlaf") },
                )
            }

            // "Nacht auf Mittwoch": the entry belongs to the morning, and saying so is what stops
            // the date from being read as the evening you went to bed.
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousDay) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Vorherige Nacht")
                }
                Text(
                    if (state.mode == SleepFormMode.NAP) {
                        "Mittagsschlaf ${DateUtils.localDateOfEpochDay(state.epochDay).format(shortDayFormatter)}"
                    } else {
                        "Nacht auf ${DateUtils.localDateOfEpochDay(state.epochDay).format(dayFormatter)}"
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = onNextDay) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Nächste Nacht")
                }
            }

            if (state.mode == SleepFormMode.NIGHT) {
                // "Nicht geschlafen" checkbox - toggle between times and checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = state.didNotSleep,
                        onCheckedChange = onDidNotSleepChange,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Nicht geschlafen", style = MaterialTheme.typography.bodyMedium)
                }

                if (!state.didNotSleep) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
                        TimeOfDayField(
                            label = "Eingeschlafen",
                            value = state.startMinuteOfDay,
                            onValueChange = onStartChange,
                            defaultMinuteOfDay = 23 * 60,
                        )
                        TimeOfDayField(
                            label = "Aufgewacht",
                            value = state.endMinuteOfDay,
                            onValueChange = onEndChange,
                            defaultMinuteOfDay = 7 * 60,
                        )
                        state.durationMinutes?.let { duration ->
                            Column {
                                Text(
                                    "Dauer",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(formatDuration(duration), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            } else {
                // Nap form: just start and end time
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
                    TimeOfDayField(
                        label = "Start",
                        value = state.startMinuteOfDay,
                        onValueChange = onStartChange,
                        defaultMinuteOfDay = 13 * 60,
                    )
                    TimeOfDayField(
                        label = "Ende",
                        value = state.endMinuteOfDay,
                        onValueChange = onEndChange,
                        defaultMinuteOfDay = 14 * 60,
                    )
                    state.durationMinutes?.let { duration ->
                        Column {
                            Text(
                                "Dauer",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(formatDuration(duration), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                TimeOfDayField(
                    label = "Zuletzt gegessen",
                    value = state.lastMealMinuteOfDay,
                    onValueChange = onLastMealChange,
                    emptyLabel = "nicht erfasst",
                    defaultMinuteOfDay = 20 * 60,
                )
                if (state.lastMealMinuteOfDay != null) {
                    IconButton(onClick = onClearLastMeal) {
                        Icon(Icons.Filled.Close, contentDescription = "Essenszeit entfernen")
                    }
                }
                // The gap is the point of recording the meal at all, so it is spelled out rather
                // than left to be worked out from two clock times.
                state.minutesBetweenLastMealAndSleep?.let { gap ->
                    Text(
                        "${formatDuration(gap)} vor dem Einschlafen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }

            // The slider opens on the last rating given, so a morning that feels like the one before
            // needs no touch at all. The number is spelled out because the handle alone does not say
            // whether it sits on 7 or 8.
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Fitness am Morgen",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${state.morningFitness}/$MAX_MORNING_FITNESS",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Slider(
                    value = state.morningFitness.toFloat(),
                    onValueChange = { onFitnessChange(it.roundToInt()) },
                    valueRange = MIN_MORNING_FITNESS.toFloat()..MAX_MORNING_FITNESS.toFloat(),
                    // The steps are the stops *between* the ends, so the handle snaps to whole ratings.
                    steps = MAX_MORNING_FITNESS - MIN_MORNING_FITNESS - 1,
                )
            }

            Text("Tags", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.allTags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.allTags.forEach { tag ->
                        FilterChip(
                            selected = tag.id in state.selectedTagIds,
                            onClick = { onTagToggle(tag.id) },
                            label = { Text(tag.name) },
                            leadingIcon = if (tag.id in state.selectedTagIds) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.tagInput,
                    onValueChange = onTagInputChange,
                    label = { Text("Tag hinzufügen (z.B. heiß)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAddTag() }),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onAddTag) {
                    Icon(Icons.Filled.Add, contentDescription = "Tag anlegen")
                }
            }

            // Only show "update" button if there are actual changes, and "save" button for new entries
            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        state.isExistingNight && state.hasChanges -> "Nacht aktualisieren"
                        state.isExistingNight && !state.hasChanges -> "Keine Änderungen"
                        else -> "Nacht speichern"
                    }
                )
            }
        }
    }
}

/** The night in the form against the goals — the same two rows the Tagesziele screen shows. */
@Composable
private fun GoalCard(statuses: List<SleepGoalStatus>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Ziele", style = MaterialTheme.typography.titleSmall)
            statuses.forEach { status ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(status.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        status.valueText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        if (status.isMet) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = if (status.isMet) "Ziel erreicht" else "Ziel nicht erreicht",
                        tint = statusColor(isMet = status.isMet),
                        modifier = Modifier.padding(start = 8.dp).size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(row: SleepHistoryRow, onDelete: () -> Unit) {
    val entry = row.entry
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        DateUtils.localDateOfEpochDay(entry.epochDay).format(shortDayFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        if (entry.didNotSleep) {
                            "Nicht geschlafen"
                        } else {
                            val start = entry.startMinuteOfDay
                            val end = entry.endMinuteOfDay
                            val duration = entry.durationMinutes
                            if (start != null && end != null && duration != null) {
                                "${formatMinuteOfDay(start)}–${formatMinuteOfDay(end)} · " +
                                    formatDuration(duration) +
                                    (entry.morningFitness?.let { " · Fitness $it/$MAX_MORNING_FITNESS" } ?: "") +
                                    (entry.lastMealMinuteOfDay?.let { " · gegessen ${formatMinuteOfDay(it)}" } ?: "")
                            } else {
                                "Fehlerhafte Eingabe"
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Nacht löschen")
                }
            }
            if (row.tagNames.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    row.tagNames.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
