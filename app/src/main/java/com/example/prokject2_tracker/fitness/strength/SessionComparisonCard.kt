package com.example.prokject2_tracker.fitness.strength

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.formatCompact
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormatter = DateTimeFormatter.ofPattern("EEE, d. MMMM", Locale.GERMAN)
private val shortDayFormatter = DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN)

/**
 * The top block: which day is being edited, and how it compares to the session before it.
 * "Frühere Einheiten" is collapsed by default so the entry panel stays above the fold — expanded it
 * answers "how much did I do in the last few workouts" beyond the single previous session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionComparisonCard(
    state: StrengthExerciseDetailUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
    onSelectDay: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEarlier by remember { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousDay) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Vorheriger Tag")
                }
                TextButton(onClick = onPickDate, modifier = Modifier.weight(1f)) {
                    Text(
                        DateUtils.localDateOfEpochDay(state.selectedEpochDay).format(dayFormatter),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                IconButton(onClick = onNextDay) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Nächster Tag")
                }
            }

            val previousLabel = state.previousSession
                ?.let { DateUtils.formatDaysSince(DateUtils.daysBetweenEpochDays(it.epochDay, state.selectedEpochDay)) }
                ?: "—"
            Row {
                Text("", modifier = Modifier.weight(1.1f))
                ColumnHeader("Letztes ($previousLabel)", Modifier.weight(1f))
                ColumnHeader("Dieses", Modifier.weight(1f))
            }
            StatRow(
                label = "Max",
                previous = state.previousSession?.maxWeightKg?.let { weightLabel(it) },
                current = state.currentSession?.maxWeightKg?.let { weightLabel(it) },
            )
            StatRow(
                label = "Volumen",
                previous = state.previousSession?.let { "${it.volumeKg.formatCompact()} kg" },
                current = state.currentSession?.let { "${it.volumeKg.formatCompact()} kg" },
            )
            StatRow(
                label = "Sätze",
                previous = state.previousSession?.setCount?.toString(),
                current = state.currentSession?.setCount?.toString(),
            )
            state.volumeDeltaKg?.let { delta ->
                Row {
                    Text(
                        "Δ Volumen",
                        modifier = Modifier.weight(1.1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("", modifier = Modifier.weight(1f))
                    Text(
                        // Sign first, colour only as reinforcement — the same reasoning the goal
                        // bars use, so the number still reads correctly without colour vision.
                        (if (delta >= 0) "+" else "−") + "${kotlin.math.abs(delta).formatCompact()} kg",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (delta >= 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SummaryLine("Zuletzt", state.previousSession)
            SummaryLine("Dieses", state.currentSession)

            if (state.recentSessions.isNotEmpty()) {
                TextButton(onClick = { showEarlier = !showEarlier }) {
                    Icon(
                        if (showEarlier) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                    )
                    Text("Frühere Einheiten")
                }
                if (showEarlier) {
                    state.recentSessions.forEach { session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectDay(session.epochDay) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                DateUtils.localDateOfEpochDay(session.epochDay).format(shortDayFormatter),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                formatSetSummary(session.sets),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "${session.volumeKg.formatCompact()} kg",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnHeader(text: String, modifier: Modifier) {
    Text(
        text,
        modifier = modifier,
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatRow(label: String, previous: String?, current: String?) {
    Row {
        Text(
            label,
            modifier = Modifier.weight(1.1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(previous ?: "—", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
        Text(current ?: "—", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SummaryLine(label: String, session: SessionStats?) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            session?.let { formatSetSummary(it.sets) } ?: "noch keine Sätze",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
