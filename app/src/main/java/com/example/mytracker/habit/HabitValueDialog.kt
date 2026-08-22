package com.example.mytracker.habit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.core.util.toLocaleDoubleOrNull

/**
 * Dialog for logging today's value on a COUNT/DURATION habit. A blank field un-logs/clears the
 * day's entry. Kept out of HabitScreen.kt so another screen can reuse it without importing the
 * whole screen.
 */
@Composable
fun HabitValueDialog(
    habit: Habit,
    initialValue: Double?,
    onConfirm: (Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue?.let { it.formatCompact() } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(habit.name) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(if (habit.type == HabitType.DURATION) "Minuten" else "Anzahl") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toLocaleDoubleOrNull()) }) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
