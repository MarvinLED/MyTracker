package com.example.prokject2_tracker.fitness.strength

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.ui.theme.statusColor
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormatter = DateTimeFormatter.ofPattern("EEE, d. MMMM", Locale.GERMAN)
private val shortDayFormatter = DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN)
private val BannerShape = RoundedCornerShape(10.dp)

/**
 * The top block: which day is being edited, and how it compares to the session before it. It opens
 * with the verdict — did this session beat the last one's volume — because that is the question the
 * screen exists to answer; the table below it is the evidence.
 *
 * This block is the one that never folds away, so the verdict is on screen whatever else is
 * collapsed. "Frühere Einheiten" inside it is collapsed by default — expanded it answers "how much
 * did I do in the last few workouts" beyond the single previous session.
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

            VolumeTargetBanner(volumeTarget(state.currentSession, state.previousSession))

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
            // No "Δ Volumen" row here: the banner above already states the difference, and saying
            // it twice made the one line that matters compete with itself.

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

/**
 * The verdict, in the one spot that is on screen whatever else is collapsed: did this session beat
 * the last one? Icon shape, wording and a tinted band all say the same thing, so it survives both
 * a glance and colour-blind reading.
 */
@Composable
private fun VolumeTargetBanner(target: VolumeTarget) {
    val reached = target.status == VolumeTargetStatus.REACHED
    val accent = when (target.status) {
        VolumeTargetStatus.REACHED -> statusColor(isMet = true)
        VolumeTargetStatus.MISSED -> statusColor(isMet = false)
        // Nothing has been decided yet, so neither hue would be honest.
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BannerShape)
            .background(accent.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (reached) Icons.Filled.Check else Icons.Filled.ArrowUpward,
            contentDescription = if (reached) "Volumenziel erreicht" else "Volumenziel noch nicht erreicht",
            tint = accent,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                target.headline,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
            Text(
                target.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
