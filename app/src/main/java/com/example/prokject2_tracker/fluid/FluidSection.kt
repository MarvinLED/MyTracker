package com.example.prokject2_tracker.fluid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.prokject2_tracker.core.util.formatCompact

@Composable
fun FluidSection(
    totalMl: Double,
    goalMl: Double,
    entries: List<FluidEntry>,
    onQuickAdd: (FluidType, Double) -> Unit,
    onDelete: (FluidEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${totalMl.formatCompact()} / ${goalMl.formatCompact()} ml",
                style = MaterialTheme.typography.titleMedium,
            )
            val progress = if (goalMl > 0) (totalMl / goalMl).toFloat().coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FluidType.entries.forEach { type ->
                    val amount = type.defaultQuickAddMl()
                    AssistChip(
                        onClick = { onQuickAdd(type, amount) },
                        label = { Text("${type.label()} +${amount.formatCompact()}") },
                    )
                }
            }

            entries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${entry.type.label()}: ${entry.amountMl.formatCompact()} ml",
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onDelete(entry) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Löschen")
                    }
                }
            }
        }
    }
}
