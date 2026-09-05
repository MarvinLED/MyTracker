package com.example.mytracker.core.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * The one shape every "wirklich löschen?" in this app has.
 *
 * Three rules it exists to keep the same everywhere:
 *
 * - **[title] names the thing.** "Eintrag löschen?" is not answerable — by the time the dialog is up
 *   the user can no longer see which row they hit.
 * - **[text] says what else goes with it**, and only when something actually does. A dialog that
 *   asks the same empty question every time is one people learn to tap through without reading,
 *   which is exactly the accident it was meant to prevent.
 * - **The confirming button is the dangerous one**, so it wears the error colour while "Abbrechen"
 *   stays plain. Tapping outside cancels, which is the safe way round.
 */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    text: String? = null,
    confirmLabel: String = "Löschen",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = text?.let { { Text(it) } },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onConfirm()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
