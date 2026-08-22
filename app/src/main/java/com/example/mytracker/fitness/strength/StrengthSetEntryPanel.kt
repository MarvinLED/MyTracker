package com.example.prokject2_tracker.fitness.strength

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.prokject2_tracker.core.util.formatDecimal
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull

/** The ± steps, in the order they appear. Chosen to cover plate maths and micro-loading in one row. */
private val WEIGHT_STEPS = listOf(-5.0, -1.0, -0.25, 0.25, 1.0, 5.0)

/**
 * The fast-entry block. Everything here is a tap: no keyboard is needed to log a set, because
 * typing two numbers per set is exactly what took too long. Every tap persists immediately —
 * there is no Speichern button, so a training session is never lost half-entered.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StrengthSetEntryPanel(
    state: StrengthExerciseDetailUiState,
    onAdjustWeight: (Double) -> Unit,
    onSetWeight: (Double) -> Unit,
    onToggleBodyweight: () -> Unit,
    onAdjustReps: (Int) -> Unit,
    onCommitSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onResumeAt: (Int) -> Unit,
    onUndoRemoval: () -> Unit,
    onNoteChange: (String) -> Unit,
    onNoteCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showWeightDialog by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { showWeightDialog = true }) {
                    Text(
                        stepperWeightLabel(state.weightKg, state.isBodyweight),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                FilterChip(
                    selected = state.isBodyweight,
                    onClick = onToggleBodyweight,
                    label = { Text("KG") },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WEIGHT_STEPS.forEach { step ->
                    // Enabled in bodyweight mode too: there they step the *added* weight, which is
                    // the whole point of a weighted pull-up.
                    FilledTonalButton(
                        onClick = { onAdjustWeight(step) },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 8.dp),
                    ) {
                        Text(
                            (if (step > 0) "+" else "−") + kotlin.math.abs(step).formatDecimal(2),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { onAdjustReps(-1) }) { Text("−") }
                // The widest, tallest target on the screen: it is pressed once per set, mid-workout.
                Button(
                    onClick = onCommitSet,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                ) {
                    Text("${state.reps} Wdh. eintragen", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = { onAdjustReps(1) }) { Text("+") }
            }

            val sets = state.currentSession?.sets.orEmpty()
            if (sets.isNotEmpty() || state.canUndoRemoval) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    sets.forEachIndexed { index, set ->
                        InputChip(
                            selected = false,
                            // Tapping the body restores this set's weight and reps into the steppers —
                            // the usual move when dropping back to a lighter weight.
                            onClick = { onResumeAt(index) },
                            label = { Text("${set.reps} × ${weightLabel(set.weightKg, set.isBodyweight)}") },
                            trailingIcon = {
                                // InputChip routes its whole surface to onClick, so the × needs its
                                // own clickable rather than a trailing-icon callback.
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Satz ${index + 1} entfernen",
                                    modifier = Modifier
                                        .size(InputChipDefaults.AvatarSize)
                                        .clickable { onRemoveSet(index) },
                                )
                            },
                        )
                    }
                    if (state.canUndoRemoval) {
                        AssistChip(
                            onClick = onUndoRemoval,
                            label = { Text("Rückgängig") },
                            leadingIcon = { Icon(Icons.Filled.Undo, contentDescription = null) },
                        )
                    }
                }
            }

            if (showNote || state.note.isNotBlank()) {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    label = { Text("Notiz") },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = onNoteCommit) { Text("Notiz speichern") }
            } else {
                TextButton(onClick = { showNote = true }) { Text("Notiz hinzufügen") }
            }
        }
    }

    if (showWeightDialog) {
        WeightInputDialog(
            initialKg = state.weightKg,
            isBodyweight = state.isBodyweight,
            onConfirm = {
                onSetWeight(it)
                showWeightDialog = false
            },
            onDismiss = { showWeightDialog = false },
        )
    }
}

/**
 * What the steppers currently mean: a plain weight, "Körpergewicht", or "Körpergewicht + 10 kg" once
 * something is hanging off the belt.
 */
private fun stepperWeightLabel(weightKg: Double, isBodyweight: Boolean): String = when {
    !isBodyweight -> "${weightKg.formatDecimal(2)} kg"
    weightKg <= 0.0 -> "Körpergewicht"
    else -> "Körpergewicht + ${weightKg.formatDecimal(2)} kg"
}

@Composable
private fun WeightInputDialog(
    initialKg: Double,
    isBodyweight: Boolean,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialKg.formatDecimal(2)) }
    val parsed = text.toLocaleDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isBodyweight) "Zusatzgewicht" else "Gewicht") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(parsed!!) }, enabled = parsed != null && parsed >= 0.0) {
                Text("Übernehmen")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
