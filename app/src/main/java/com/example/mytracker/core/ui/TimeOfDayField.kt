package com.example.mytracker.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.mytracker.core.util.formatMinuteOfDay

/**
 * A labelled clock time, tapped rather than typed: the wheel is faster than a keyboard for "23:10",
 * and it cannot produce a 25:70. Times are minutes since midnight throughout — see
 * [com.example.mytracker.core.util.TimeOfDay].
 *
 * [value] null shows [emptyLabel] instead of a time, which is what makes an optional time (when did
 * you last eat) distinguishable from midnight.
 */
@Composable
fun TimeOfDayField(
    label: String,
    value: Int?,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    emptyLabel: String = "—",
    defaultMinuteOfDay: Int = 22 * 60,
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { showPicker = true }) {
            Text(
                value?.let { formatMinuteOfDay(it) } ?: emptyLabel,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }

    if (showPicker) {
        TimeOfDayPickerDialog(
            title = label,
            initialMinuteOfDay = value ?: defaultMinuteOfDay,
            onConfirm = {
                onValueChange(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeOfDayPickerDialog(
    title: String,
    initialMinuteOfDay: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
