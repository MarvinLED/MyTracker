package com.example.prokject2_tracker.nutrition.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.prokject2_tracker.core.util.formatCompact

/**
 * "Lebensmittel wählen" — search plus results, shared by the Rezept editor and the Tagebuch entry
 * editor so both behave the same.
 *
 * The list is sized with `heightIn(max = …)`, not a fixed height: a fixed one is also a *minimum*,
 * which left the dialog padded out with dead space whenever few foods matched, while still refusing
 * to grow when many did.
 */
@Composable
fun FoodPickerDialog(
    query: String,
    results: List<FoodItem>,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onPick: (FoodItem) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
        title = { Text("Lebensmittel wählen") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Suche") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (results.isEmpty()) {
                    Text(
                        "Keine Lebensmittel gefunden.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                        items(results, key = { it.id }) { food ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(food) }
                                    .padding(vertical = 12.dp),
                            ) {
                                Text(food.brand?.let { "${food.name} ($it)" } ?: food.name)
                                Text(
                                    "${food.kcalPer100.formatCompact()} kcal / 100 ${food.baseUnit.label()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}
