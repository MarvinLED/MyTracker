package com.example.mytracker.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mytracker.core.metrics.AxisTicks
import com.example.mytracker.core.metrics.extendedTo
import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.core.metrics.niceAxisTicks
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.formatCompact
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * How a series' stroke is drawn. For telling apart lines that belong together on one hue — a
 * target, what was actually reached, and its average — where a second colour would claim they are
 * unrelated and the palette would run out besides.
 */
enum class ChartLineStyle { SOLID, DASHED, DOTTED }

/** One series on a [DatedLineChart]. [zeroBased] false is for series like body weight — see below. */
data class ChartLine(
    val label: String,
    val unit: String,
    val color: Color,
    val points: List<MetricPoint>,
    val zeroBased: Boolean = true,
    val style: ChartLineStyle = ChartLineStyle.SOLID,
    /**
     * Draws a dot per point. False for series that are not measurements but a level carried across
     * days — a target or a weekly mean — where a dot per day would suggest daily readings that were
     * never taken.
     */
    val markers: Boolean = true,
)

/**
 * Above this many series the overlaid mode stops giving each one a min/max column of its own: past
 * three columns the gutter costs more plot width than the axis labels are worth. The ranges move
 * into the legend instead, so every series still discloses its own scale.
 */
private const val MaxGutterColumns = 3

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
 *
 * [sharedScale] is for the case where the overlaid series measure the same thing — one nutrient's
 * target, intake and average, or systolisch and diastolisch morgens and abends. Then the per-series
 * scales are wrong twice over: the lines can't be read against each other, and the gutter repeats
 * the same kind of number once per line. One scale with round, labelled steps replaces them, which
 * is also the only way to tell where between the ends a point actually sits.
 *
 * Series that do **not** measure the same thing still can't share a scale, so a shared scale is per
 * unit: the lines are grouped by [ChartLine.unit] and each group gets its own axis column, with the
 * unit named on the top label once there is more than one. That is what lets a pulse ride along in
 * a blood-pressure chart — same days, same plot, its own axis — without dragging the mmHg lines
 * onto a scale three times too tall.
 */
@Composable
fun DatedLineChart(
    lines: List<ChartLine>,
    modifier: Modifier = Modifier,
    panelHeight: Int = 140,
    overlaid: Boolean = false,
    sharedScale: Boolean = false,
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
                sharedScale = sharedScale,
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
            // Wraps: a dozen series would otherwise run off the side and the ones past the edge
            // would be the only clue to which colour is which.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Not with a shared scale: the gutter's labelled axis already covers every series.
                val showRanges = overlaid && !sharedScale && drawable.size > MaxGutterColumns
                drawable.forEach { LegendEntry(it, scale = if (showRanges) scaleOf(it) else null) }
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
    // A floor rather than a fixed height: with a handful of series the box never changes size, and
    // with a dozen it grows instead of cutting the values off.
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp), contentAlignment = Alignment.CenterStart) {
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
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        lines.forEach { line ->
                            val value = line.points.firstOrNull { it.epochDay == selectedDay }?.value
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // The same mark the legend uses, not a plain dot: two series may
                                // share a hue and be told apart only by the dash.
                                SeriesMark(line)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    value?.withUnit(line.unit) ?: "–",
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

/**
 * A value with its unit. Units written as a quotient carry their own separator — "72/min", never
 * "72 /min" — while a plain unit takes a space.
 */
private fun Double.withUnit(unit: String): String =
    if (unit.startsWith("/")) "${formatCompact()}$unit" else "${formatCompact()} $unit"

/** A series' y bounds. Shared by both modes so a line can never be scaled two different ways. */
private data class LineScale(val min: Double, val max: Double) {
    val range: Double get() = (max - min).let { if (it > 0) it else 1.0 }
}

/**
 * One axis per unit, in the order the units first appear — the first one is the primary, and the
 * grid is drawn at its steps. All of them are stretched to the same number of steps so every
 * column's labels sit on the same grid lines; a second axis whose labels fell between the lines of
 * the first would be unreadable at exactly the moment two axes are worth having.
 */
private fun sharedTicksByUnit(lines: List<ChartLine>): Map<String, AxisTicks> {
    val perUnit = lines.groupBy { it.unit }.mapValues { (_, group) -> sharedTicksOf(group) }
    val steps = perUnit.values.maxOf { it.values.size }
    return perUnit.mapValues { (_, ticks) -> ticks.extendedTo(steps) }
}

/**
 * One scale across every series, snapped to round steps. The floor stays at 0 only if every line
 * asks for it — one non-zero-based series in the set would otherwise be squashed flat.
 */
private fun sharedTicksOf(lines: List<ChartLine>): AxisTicks {
    val values = lines.flatMap { line -> line.points.map { it.value } }
    val rawMax = values.maxOrNull() ?: 1.0
    val rawMin = if (lines.all { it.zeroBased }) 0.0 else values.minOrNull() ?: 0.0
    return niceAxisTicks(min = rawMin, max = rawMax)
}

private fun scaleOf(line: ChartLine): LineScale {
    val rawMax = line.points.maxOf { it.value }
    if (line.zeroBased) return LineScale(0.0, rawMax.coerceAtLeast(1.0))

    val rawMin = line.points.minOf { it.value }
    // A series that never moves — a Soll held all range, a weight logged at one value — has no span
    // to pad a tenth of. Without a band of its own it would be pinned to the bottom edge, reading
    // as the panel's floor rather than as a level. Centred on the value instead.
    if (rawMax <= rawMin) {
        val pad = (abs(rawMax) * 0.1).coerceAtLeast(1.0)
        return LineScale(rawMin - pad, rawMax + pad)
    }
    return LineScale(rawMin - (rawMax - rawMin) * 0.1, rawMax)
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
    sharedScale: Boolean,
    onSelectFraction: (Float) -> Unit,
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val crosshairColor = MaterialTheme.colorScheme.onSurfaceVariant
    val markerRing = MaterialTheme.colorScheme.surfaceContainer
    val series = remember(lines) { lines.map { it to it.points.sortedBy { p -> p.epochDay } } }
    val ticksByUnit = remember(lines, sharedScale) { if (sharedScale) sharedTicksByUnit(lines) else null }
    val scales = remember(lines, ticksByUnit) {
        ticksByUnit?.let { byUnit ->
            lines.map { line -> byUnit.getValue(line.unit).let { LineScale(it.min, it.max) } }
        } ?: lines.map(::scaleOf)
    }
    // Grid at every labelled step when they are labelled, otherwise the recessive baseline/mid/top
    // trio — enough to read a level off, not enough to compete. With two axes the primary one draws
    // it; every axis has the same number of steps, so the other column's labels land on it too.
    val gridFractions = remember(ticksByUnit) {
        ticksByUnit?.values?.firstOrNull()?.let { ticks ->
            ticks.values.map { value -> 1f - ((value - ticks.min) / (ticks.max - ticks.min)).toFloat() }
        } ?: listOf(0f, 0.5f, 1f)
    }

    Row(modifier = Modifier.fillMaxWidth().height(heightDp.dp)) {
        if (ticksByUnit != null) {
            // One column per unit, not per line: with a shared scale a column per line would be the
            // same numbers again. Evenly spaced, which is what makes the labels line up with the grid.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ticksByUnit.forEach { (unit, ticks) ->
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        ticks.values.reversed().forEachIndexed { index, value ->
                            Text(
                                // The topmost label names the unit once there are two axes to tell
                                // apart. On its own row rather than as a caption, because a caption
                                // would be one more row in one column and pull its labels off the
                                // grid the other column is still on.
                                if (index == 0 && ticksByUnit.size > 1) {
                                    value.withUnit(unit)
                                } else {
                                    value.formatCompact()
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
        } else if (lines.size <= MaxGutterColumns) {
            // Past a few series the columns would leave no plot to speak of, so the ranges move to
            // the legend instead — see [MaxGutterColumns].
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
        }

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

            gridFractions.forEach { fraction ->
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

                drawSeries(line, sorted, ::xFor, ::yFor)

                if (line.markers && sorted.size <= 40) {
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

            drawSeries(line, sorted, ::xFor, ::yFor)

            // Markers only when they'd stay distinguishable; a 365-day series would be a solid band.
            if (line.markers && sorted.size <= 40) {
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

/**
 * The whole series as one stroked path rather than a segment per pair of points. A patterned stroke
 * needs it: applied per segment the pattern restarts at every point, which at a daily resolution
 * renders as a solid line again. Round joins keep the solid case looking as it did when it was
 * drawn as round-capped segments.
 */
private fun DrawScope.drawSeries(
    line: ChartLine,
    sorted: List<MetricPoint>,
    xFor: (Long) -> Float,
    yFor: (Double) -> Float,
) {
    if (sorted.size < 2) return
    val path = Path().apply {
        moveTo(xFor(sorted.first().epochDay), yFor(sorted.first().value))
        sorted.drop(1).forEach { lineTo(xFor(it.epochDay), yFor(it.value)) }
    }
    drawPath(
        path = path,
        color = line.color,
        style = Stroke(
            width = 4f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = when (line.style) {
                ChartLineStyle.SOLID -> null
                ChartLineStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                ChartLineStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(2f, 8f))
            },
        ),
    )
}

/**
 * A series' legend mark, patterned like its stroke. Series that share a hue with the one they
 * belong to are told apart by the pattern alone, so a plain dot for all of them would make them
 * identical in the legend.
 */
@Composable
private fun SeriesMark(line: ChartLine) {
    when (line.style) {
        ChartLineStyle.SOLID -> Box(modifier = Modifier.size(10.dp).background(line.color, CircleShape))
        ChartLineStyle.DASHED -> Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(2) {
                Box(modifier = Modifier.size(width = 5.dp, height = 3.dp).background(line.color, DashShape))
            }
        }
        ChartLineStyle.DOTTED -> Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) {
                Box(modifier = Modifier.size(3.dp).background(line.color, CircleShape))
            }
        }
    }
}

private val DashShape = RoundedCornerShape(1.dp)

/** [scale] non-null adds the series' own y range, for when the overlaid gutter is not drawing it. */
@Composable
private fun LegendEntry(line: ChartLine, scale: LineScale?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SeriesMark(line)
        Spacer(Modifier.width(6.dp))
        val range = scale?.let { " ${it.min.formatCompact()}–${it.max.formatCompact()}" }.orEmpty()
        Text("${line.label} (${line.unit})$range", style = MaterialTheme.typography.labelMedium)
    }
}
