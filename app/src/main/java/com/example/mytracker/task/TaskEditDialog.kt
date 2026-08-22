package com.example.mytracker.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mytracker.core.util.DateUtils
import java.time.DayOfWeek
import java.time.Instant

private const val MILLIS_PER_DAY = 86_400_000L

/** The rhythm names, in the order the chips offer them — plainest first. */
private fun TaskRecurrence.chipLabel(): String = when (this) {
    TaskRecurrence.ONCE -> "Einmalig"
    TaskRecurrence.EVERY_N_DAYS -> "Alle N Tage"
    TaskRecurrence.EVERY_N_WEEKS -> "Alle N Wochen"
    TaskRecurrence.WEEKDAYS -> "Wochentage"
    TaskRecurrence.DAY_OF_MONTH -> "Monatlich"
}

/**
 * The form's own state. Fields belonging to rhythms other than the selected one are kept rather
 * than cleared, so flipping between "alle 3 Tage" and "alle 3 Wochen" to compare does not lose the
 * 3 — only the selected [recurrence] decides what is read back out.
 */
private data class TaskDraft(
    val name: String,
    val recurrence: TaskRecurrence,
    val intervalText: String,
    val weekdayMask: Int,
    val dayOfMonthText: String,
    val startEpochDay: Long,
) {
    val intervalCount: Int get() = intervalText.toIntOrNull()?.coerceIn(1, 999) ?: 1
    val dayOfMonth: Int get() = dayOfMonthText.toIntOrNull()?.coerceIn(1, 31) ?: 1

    /** The draft as the entity would be stored, for the live "next due" preview and for saving. */
    fun toTask(id: String, createdAt: Instant): Task = Task(
        id = id,
        name = name.trim(),
        recurrence = recurrence,
        intervalCount = intervalCount,
        weekdayMask = weekdayMask,
        dayOfMonth = dayOfMonth,
        startEpochDay = startEpochDay,
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}

private fun Task.toDraft() = TaskDraft(
    name = name,
    recurrence = recurrence,
    intervalText = intervalCount.toString(),
    weekdayMask = weekdayMask,
    dayOfMonthText = dayOfMonth.toString(),
    startEpochDay = startEpochDay,
)

/**
 * Creates or edits a task. [existing] null means "new" — the only difference between the two modes,
 * since the rhythm is fully rewritable afterwards.
 */
@Composable
fun TaskEditDialog(
    existing: Task?,
    today: Long,
    onConfirm: (name: String, recurrence: TaskRecurrence, startEpochDay: Long, intervalCount: Int, weekdayMask: Int, dayOfMonth: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember {
        mutableStateOf(
            existing?.toDraft() ?: TaskDraft(
                name = "",
                recurrence = TaskRecurrence.ONCE,
                intervalText = "3",
                // Today's weekday pre-ticked: the common "jeden Montag" is then one tap, and an
                // empty mask would make the rhythm unable to fire at all.
                weekdayMask = DateUtils.localDateOfEpochDay(today).dayOfWeek.bit(),
                dayOfMonthText = "1",
                startEpochDay = today,
            ),
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }

    // Built from the draft on every keystroke, so the rule can be checked against the calendar
    // before saving: a rhythm that answers "nie" is exactly the one worth catching here.
    val preview = draft.toTask(id = "preview", createdAt = Instant.EPOCH)
    val nextDue = preview.nextDueOnOrAfter(today)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Aufgabe hinzufügen" else "Aufgabe bearbeiten") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    "Rhythmus",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TaskRecurrence.entries.forEach { candidate ->
                        FilterChip(
                            selected = draft.recurrence == candidate,
                            onClick = { draft = draft.copy(recurrence = candidate) },
                            label = { Text(candidate.chipLabel()) },
                        )
                    }
                }

                when (draft.recurrence) {
                    TaskRecurrence.ONCE -> Unit
                    TaskRecurrence.EVERY_N_DAYS -> IntervalField(
                        value = draft.intervalText,
                        onValueChange = { draft = draft.copy(intervalText = it) },
                        unit = "Tage",
                    )
                    TaskRecurrence.EVERY_N_WEEKS -> IntervalField(
                        value = draft.intervalText,
                        onValueChange = { draft = draft.copy(intervalText = it) },
                        unit = "Wochen",
                    )
                    TaskRecurrence.WEEKDAYS -> WeekdayPicker(
                        mask = draft.weekdayMask,
                        onToggle = { day -> draft = draft.copy(weekdayMask = draft.weekdayMask xor day.bit()) },
                    )
                    TaskRecurrence.DAY_OF_MONTH -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IntervalField(
                            value = draft.intervalText,
                            onValueChange = { draft = draft.copy(intervalText = it) },
                            unit = "Monate",
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = draft.dayOfMonthText,
                                onValueChange = { draft = draft.copy(dayOfMonthText = it.filter(Char::isDigit).take(2)) },
                                label = { Text("Tag") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(96.dp),
                            )
                            Text(
                                "des Monats",
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                // The one-off's date *is* its due date; for the rhythms it is the anchor they are
                // counted from, which is why the label changes with the mode.
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    val prefix = if (draft.recurrence == TaskRecurrence.ONCE) "Fällig am" else "Ab"
                    Text("$prefix ${formatEpochDay(draft.startEpochDay)}")
                }

                Text(
                    nextDue?.let { "Nächste Fälligkeit: ${formatEpochDay(it)}" }
                        ?: "Mit dieser Einstellung wird die Aufgabe nie fällig.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (nextDue == null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        draft.name,
                        draft.recurrence,
                        draft.startEpochDay,
                        draft.intervalCount,
                        draft.weekdayMask,
                        draft.dayOfMonth,
                    )
                },
                // A rule that can never fire is not worth saving — that is the empty weekday mask,
                // and a one-off whose date has passed unticked is still perfectly valid (it shows
                // up as overdue), so only `nextDue` from the *start* day is checked.
                enabled = draft.name.isNotBlank() && preview.nextDueOnOrAfter(draft.startEpochDay) != null,
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )

    if (showDatePicker) {
        TaskDatePickerDialog(
            initialEpochDay = draft.startEpochDay,
            onPick = { draft = draft.copy(startEpochDay = it) },
            onDismiss = { showDatePicker = false },
        )
    }
}

/**
 * Same epoch-day/millis handling as the training screens: the picker speaks UTC millis, and a day
 * is exactly [MILLIS_PER_DAY] of them, so the conversion is a plain multiply either way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDatePickerDialog(
    initialEpochDay: Long,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpochDay * MILLIS_PER_DAY)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onPick(it / MILLIS_PER_DAY) }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun IntervalField(value: String, onValueChange: (String) -> Unit, unit: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Alle", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.filter(Char::isDigit).take(3)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(88.dp).padding(horizontal = 8.dp),
        )
        Text(unit, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WeekdayPicker(mask: Int, onToggle: (DayOfWeek) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DayOfWeek.entries.forEach { day ->
            FilterChip(
                selected = mask.hasWeekday(day),
                onClick = { onToggle(day) },
                label = { Text(day.shortLabel()) },
            )
        }
    }
}
