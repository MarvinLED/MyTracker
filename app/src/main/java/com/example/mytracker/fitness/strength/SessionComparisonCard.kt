package com.example.mytracker.fitness.strength

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ArrowDownward
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.formatDecimal
import com.example.mytracker.ui.theme.cautionColor
import com.example.mytracker.ui.theme.statusColor
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormatter = DateTimeFormatter.ofPattern("EEE, d. MMMM", Locale.GERMAN)
private val BannerShape = RoundedCornerShape(10.dp)

/**
 * The top block: which day is being edited, and how it compares to the sessions before it. It opens
 * with the verdict — did this session beat the last one's volume — because that is the question the
 * screen exists to answer; the list below it is the evidence.
 *
 * The evidence is one row per session rather than a metrics table. The table read down the wrong
 * axis: it put "Max" next to "Volumen" next to "Sätze" when what a lifter compares is one training
 * against the training before it. A row per session says the same numbers along the axis they are
 * actually read on, and leaves room to say per session whether it was a step up.
 *
 * This block is the one that never folds away, so the verdict is on screen whatever else is
 * collapsed. Only the sessions past the first three wait behind "Frühere Einheiten".
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
    // A set rather than a single open row: two dates can be wanted at once, and closing one row
    // because another was opened would be the card second-guessing what was asked for.
    var openDates by remember { mutableStateOf(emptySet<Long>()) }

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

            // Above the volume verdict, because it outranks it: beating the last session is the
            // week's business, beating every session there has ever been is the year's.
            state.topSetRecord?.let { TopSetRecordBanner(it) }

            VolumeTargetBanner(volumeTarget(state.currentSession, state.previousSession))

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            val rows = state.sessionRows
            val earlier = rows.drop(PINNED_SESSION_ROWS)

            fun toggleDate(epochDay: Long) {
                openDates = if (epochDay in openDates) openDates - epochDay else openDates + epochDay
            }

            rows.take(PINNED_SESSION_ROWS).forEach { row ->
                SessionRowItem(
                    row = row,
                    isSelectedDay = row.epochDay == state.selectedEpochDay,
                    showDate = row.epochDay in openDates,
                    onToggleDate = { toggleDate(row.epochDay) },
                    onSelectDay = onSelectDay,
                )
            }

            if (earlier.isNotEmpty()) {
                if (showEarlier) {
                    earlier.forEach { row ->
                        SessionRowItem(
                            row = row,
                            isSelectedDay = false,
                            showDate = row.epochDay in openDates,
                            onToggleDate = { toggleDate(row.epochDay) },
                            onSelectDay = onSelectDay,
                        )
                    }
                }
                // Below the rows once they are out: the handle that closes a list belongs at the end
                // of it, where the thumb already is after reading through.
                TextButton(onClick = { showEarlier = !showEarlier }) {
                    Icon(
                        if (showEarlier) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                    )
                    Text(if (showEarlier) "Einklappen" else "Frühere Einheiten")
                }
            }
        }
    }
}

/**
 * One session: what it was, how heavy it got, and whether that beat the session before it.
 *
 * The heaviest set carries the verdict because it is the number a lifter steers by between sessions
 * — but never by colour alone: the arrow's direction says the same thing, and the two weights are
 * both on screen to be compared directly.
 */
@Composable
private fun SessionRowItem(
    row: SessionRow,
    isSelectedDay: Boolean,
    showDate: Boolean,
    onToggleDate: () -> Unit,
    onSelectDay: (Long) -> Unit,
) {
    val trendColor = when (row.trend) {
        MaxWeightTrend.IMPROVED -> statusColor(isMet = true)
        MaxWeightTrend.DECLINED -> cautionColor()
        // A held top set and a day with nothing to compare both stay neutral.
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleDate)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelectedDay) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    row.setSummary ?: "noch keine Sätze",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (row.trend) {
                        MaxWeightTrend.IMPROVED -> Icon(
                            Icons.Filled.ArrowUpward,
                            contentDescription = "schwerer als davor",
                            tint = trendColor,
                            modifier = Modifier.size(16.dp),
                        )
                        MaxWeightTrend.DECLINED -> Icon(
                            Icons.Filled.ArrowDownward,
                            contentDescription = "leichter als davor",
                            tint = trendColor,
                            modifier = Modifier.size(16.dp),
                        )
                        else -> Unit
                    }
                    Text(
                        row.maxWeightText ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = trendColor,
                    )
                }
                Text(
                    row.volumeText.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(visible = showDate) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    row.dateText,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Tapping a row used to jump straight to that day; now that a tap reveals the date,
                // the jump needs somewhere visible to live rather than staying an unmarked gesture.
                if (!isSelectedDay && row.hasSession) {
                    TextButton(onClick = { onSelectDay(row.epochDay) }) { Text("Zu diesem Tag") }
                }
            }
        }
    }
}

/**
 * The rarest thing this screen has to say, so it gets to say it loudest: this exercise has never
 * been lifted heavier. Shown only on the day it happened — a record is news once, and a band that
 * stayed up for weeks would turn into wallpaper.
 */
@Composable
private fun TopSetRecordBanner(record: TopSetRecord) {
    val accent = statusColor(isMet = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BannerShape)
            .background(accent.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A trophy rather than an icon: this is the one moment on the screen that is allowed to
        // celebrate. The wording carries it too, so nothing rests on the emoji being rendered.
        Text("🏆", style = MaterialTheme.typography.titleLarge)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Neuer Rekord: ${record.weightKg.formatDecimal(2)} kg",
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
            Text(
                record.previousKg
                    ?.let { "Bestmarke geschlagen — davor ${it.formatDecimal(2)} kg" }
                    ?: "Deine erste Bestmarke bei dieser Übung",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
