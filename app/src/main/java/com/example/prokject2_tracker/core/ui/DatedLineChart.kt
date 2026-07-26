package com.example.prokject2_tracker.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.formatCompact
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One series on a [DatedLineChart]. [zeroBased] false is for series like body weight — see below. */
data class ChartLine(
    val label: String,
    val unit: String,
    val color: Color,
    val points: List<MetricPoint>,
    val zeroBased: Boolean = true,
)

/**
 * A line chart over dates, with a real date axis and a draggable crosshair that reads out the exact
 * values.
 *
 * By default several series are drawn as **stacked panels sharing one x-axis**: each series keeps
 * its own honest scale, and the shared crosshair still answers "what were both on the 14th?" —
 * which is the actual reason to compare them.
 *
 * [overlaid] draws them in one plot area instead, each still on its own scale. The usual objection
 * to that — the reader can't tell which line belongs to which axis, so the crossings look meaningful
 * when they aren't — is answered by giving **every** series its own colour-matched min/max labels in
 * the left gutter, one column per metric. Use it when the shapes are meant to be read against each
 * other directly; keep the stacked default when each series is read on its own.
 *
 * [zeroBased] per line controls the y floor: true anchors at 0, false uses min minus 10 % of the
 * range, for series like body weight where day-to-day variation is tiny next to the absolute value
 * and a zero-based axis would flatten the line to a straight edge.
 */
@Composable
fun DatedLineChart(
    lines: List<ChartLine>,
    modifier: Modifier = Modifier,
    panelHeight: Int = 140,
    overlaid: Boolean = false,
) {
    val drawable = lines.filter { it.points.size >= 2 }
    if (drawable.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(panelHeight.dp), contentAlignment = Alignment.Center) {
            Text(
                "Nicht genug Datenpunkte im Zeitraum.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    // One x-scale for every panel, so the crosshair lines up across them.
    val allDays = drawable.flatMap { line -> line.points.map { it.epochDay } }
    val minDay = allDays.min()
    val maxDay = allDays.max()
    val dayRange = (maxDay - minDay).coerceAtLeast(1)

    var selectedDay by remember(minDay, maxDay) { mutableStateOf<Long?>(null) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SelectionReadout(lines = drawable, selectedDay = selectedDay)

        // Snap to the nearest day that actually has a point, so the readout never invents a value
        // for a gap in the data.
        fun snap(candidates: List<MetricPoint>, fraction: Float): Long? {
            val target = minDay + Math.round(fraction * dayRange)
            return candidates.minByOrNull { kotlin.math.abs(it.epochDay - target) }?.epochDay
        }

        if (overlaid) {
            OverlaidPanel(
                lines = drawable,
                minDay = minDay,
                dayRange = dayRange,
                selectedDay = selectedDay,
                heightDp = panelHeight,
                onSelectFraction = { fraction ->
                    selectedDay = snap(drawable.flatMap { it.points }, fraction)
                },
            )
        } else {
            drawable.forEach { line ->
                LinePanel(
                    line = line,
                    minDay = minDay,
                    dayRange = dayRange,
                    selectedDay = selectedDay,
                    heightDp = panelHeight,
                    showSeriesLabel = drawable.size > 1,
                    onSelectFraction = { fraction -> selectedDay = snap(line.points, fraction) },
                )
            }
        }

        DateAxisLabels(minDay = minDay, maxDay = maxDay)

        if (drawable.size >= 2) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                drawable.forEach { LegendEntry(it) }
            }
        }
    }
}

/**
 * The exact values at the crosshair. Sits above the panels at a fixed height so selecting a point
 * never shifts the chart under the finger.
 */
@Composable
private fun SelectionReadout(lines: List<ChartLine>, selectedDay: Long?) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, d. MMM yyyy", Locale.GERMAN) }
    Box(modifier = Modifier.fillMaxWidth().height(38.dp), contentAlignment = Alignment.CenterStart) {
        if (selectedDay == null) {
            Text(
                "Tippe oder zieh im Diagramm für genaue Werte.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(
                        DateUtils.localDateOfEpochDay(selectedDay).format(dateFormatter),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        lines.forEach { line ->
                            val value = line.points.firstOrNull { it.epochDay == selectedDay }?.value
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(line.color, CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    value?.let { "${it.formatCompact()} ${line.unit}" } ?: "–",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A series' y bounds. Shared by both modes so a line can never be scaled two different ways. */
private data class LineScale(val min: Double, val max: Double) {
    val range: Double get() = (max - min).let { if (it > 0) it else 1.0 }
}

private fun scaleOf(line: ChartLine): LineScale {
    val rawMax = line.points.maxOf { it.value }
    val max = if (line.zeroBased) rawMax.coerceAtLeast(1.0) else rawMax
    val min = if (line.zeroBased) 0.0 else {
        val rawMin = line.points.minOf { it.value }
        rawMin - (max - rawMin) * 0.1
    }
    return LineScale(min, max)
}

/**
 * Every series in one plot area, each mapped through its own [LineScale]. The left gutter carries
 * one min/max column per metric in that metric's colour — without it the overlay would be exactly
 * the "which line is this axis for?" trap the stacked mode exists to avoid.
 */
@Composable
private fun OverlaidPanel(
    lines: List<ChartLine>,
    minDay: Long,
    dayRange: Long,
    selectedDay: Long?,
    heightDp: Int,
    onSelectFraction: (Float) -> Unit,
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val crosshairColor = MaterialTheme.colorScheme.onSurfaceVariant
    val markerRing = MaterialTheme.colorScheme.surfaceContainer
    val series = remember(lines) { lines.map { it to it.points.sortedBy { p -> p.epochDay } } }
    val scales = remember(lines) { lines.map(::scaleOf) }

    Row(modifier = Modifier.fillMaxWidth().height(heightDp.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            lines.forEachIndexed { index, line ->
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        scales[index].max.formatCompact(),
                        style = MaterialTheme.typography.labelSmall,
                        color = line.color,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        scales[index].min.formatCompact(),
                        style = MaterialTheme.typography.labelSmall,
                        color = line.color,
                    )
                }
            }
        }
        Spacer(Modifier.width(6.dp))

        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(minDay, dayRange, series) {
                    detectTapGestures { offset ->
                        onSelectFraction((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(minDay, dayRange, series) {
                    detectHorizontalDragGestures { change, _ ->
                        onSelectFraction((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                    }
                },
        ) {
            fun xFor(epochDay: Long) = size.width * (epochDay - minDay).toFloat() / dayRange

            listOf(0f, 0.5f, 1f).forEach { fraction ->
                val y = size.height * fraction
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }

            selectedDay?.let { day ->
                val x = xFor(day)
                drawLine(crosshairColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
            }

            series.forEachIndexed { index, (line, sorted) ->
                val scale = scales[index]
                fun yFor(value: Double) =
                    size.height - (size.height * ((value - scale.min) / scale.range)).toFloat()

                for (i in 0 until sorted.size - 1) {
                    drawLine(
                        color = line.color,
                        start = Offset(xFor(sorted[i].epochDay), yFor(sorted[i].value)),
                        end = Offset(xFor(sorted[i + 1].epochDay), yFor(sorted[i + 1].value)),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round,
                    )
                }
                if (sorted.size <= 40) {
                    sorted.forEach { point ->
                        drawCircle(line.color, radius = 5f, center = Offset(xFor(point.epochDay), yFor(point.value)))
                    }
                }
                selectedDay?.let { day ->
                    sorted.firstOrNull { it.epochDay == day }?.let { point ->
                        val center = Offset(xFor(point.epochDay), yFor(point.value))
                        drawCircle(color = line.color, radius = 8f, center = center)
                        drawCircle(color = markerRing, radius = 8f, center = center, style = Stroke(width = 3f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LinePanel(
    line: ChartLine,
    minDay: Long,
    dayRange: Long,
    selectedDay: Long?,
    heightDp: Int,
    showSeriesLabel: Boolean,
    onSelectFraction: (Float) -> Unit,
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val crosshairColor = MaterialTheme.colorScheme.onSurfaceVariant
    val markerRing = MaterialTheme.colorScheme.surfaceContainer
    val sorted = remember(line.points) { line.points.sortedBy { it.epochDay } }

    val scale = remember(line) { scaleOf(line) }
    val maxValue = scale.max
    val minValue = scale.min
    val valueRange = scale.range

    Box(modifier = Modifier.fillMaxWidth().height(heightDp.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .pointerInput(minDay, dayRange, sorted) {
                    detectTapGestures { offset ->
                        onSelectFraction((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(minDay, dayRange, sorted) {
                    detectHorizontalDragGestures { change, _ ->
                        onSelectFraction((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                    }
                },
        ) {
            fun xFor(epochDay: Long) = size.width * (epochDay - minDay).toFloat() / dayRange
            fun yFor(value: Double) = size.height - (size.height * ((value - minValue) / valueRange)).toFloat()

            // Recessive baseline and mid grid line: enough to read a level off, not enough to compete.
            listOf(0f, 0.5f, 1f).forEach { fraction ->
                val y = size.height * fraction
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }

            selectedDay?.let { day ->
                val x = xFor(day)
                drawLine(crosshairColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
            }

            for (i in 0 until sorted.size - 1) {
                drawLine(
                    color = line.color,
                    start = Offset(xFor(sorted[i].epochDay), yFor(sorted[i].value)),
                    end = Offset(xFor(sorted[i + 1].epochDay), yFor(sorted[i + 1].value)),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round,
                )
            }

            // Markers only when they'd stay distinguishable; a 365-day series would be a solid band.
            if (sorted.size <= 40) {
                sorted.forEach { point ->
                    drawCircle(
                        color = line.color,
                        radius = 5f,
                        center = Offset(xFor(point.epochDay), yFor(point.value)),
                    )
                }
            }

            // The selected point gets a surface-coloured ring so it stays visible on top of the line.
            selectedDay?.let { day ->
                sorted.firstOrNull { it.epochDay == day }?.let { point ->
                    val center = Offset(xFor(point.epochDay), yFor(point.value))
                    drawCircle(color = line.color, radius = 8f, center = center)
                    drawCircle(color = markerRing, radius = 8f, center = center, style = Stroke(width = 3f))
                }
            }
        }

        Text(
            maxValue.formatCompact(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Text(
            minValue.formatCompact(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomStart),
        )
        if (showSeriesLabel) {
            Text(
                "${line.label} (${line.unit})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

/**
 * Dates under the axis instead of a bare index. The format follows the span — day and month for a
 * few weeks, month and year once a year's worth is on screen — and only three labels are drawn, so
 * they never collide on a phone's width.
 */
@Composable
private fun DateAxisLabels(minDay: Long, maxDay: Long) {
    val span = maxDay - minDay
    val formatter = remember(span) {
        DateTimeFormatter.ofPattern(if (span > 120) "MMM yy" else "d. MMM", Locale.GERMAN)
    }
    val labels = listOf(minDay, minDay + span / 2, maxDay)
        .map { DateUtils.localDateOfEpochDay(it).format(formatter) }

    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = when (index) {
                    0 -> TextAlign.Start
                    labels.lastIndex -> TextAlign.End
                    else -> TextAlign.Center
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LegendEntry(line: ChartLine) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(line.color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text("${line.label} (${line.unit})", style = MaterialTheme.typography.labelMedium)
    }
}
