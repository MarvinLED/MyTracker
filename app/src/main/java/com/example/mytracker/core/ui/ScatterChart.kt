package com.example.mytracker.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mytracker.core.metrics.AxisTicks
import com.example.mytracker.core.metrics.LinearFit
import com.example.mytracker.core.metrics.niceAxisTicks
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.core.util.formatSigned

/**
 * One dot. [label] names what it stands for — a week, a session — and is what the readout shows when
 * it is tapped, since a pair of numbers alone never says which one it was.
 */
data class ScatterPoint(val x: Double, val y: Double, val label: String)

/**
 * Two measurements plotted against **each other** rather than against time.
 *
 * A time chart answers "what happened when", and two series on one are compared by eye across their
 * own scales — which is exactly where a relationship gets invented that isn't there. Dropping the
 * time axis puts the relationship itself on screen: each dot is one period, and whether the cloud
 * slopes is the whole answer.
 *
 * [fit] is drawn across the x range when the caller has one, [goalMarker] as an upright line — a
 * value on the x axis worth reading the cloud against, such as the target that produced it.
 *
 * Dots fade with age, oldest faintest, so the drift the scatter would otherwise hide — the same
 * intake landing differently now than three months ago — stays visible. [points] must therefore be
 * oldest first.
 */
@Composable
fun ScatterChart(
    points: List<ScatterPoint>,
    xAxisLabel: String,
    yAxisLabel: String,
    xUnit: String,
    yUnit: String,
    pointColor: Color,
    modifier: Modifier = Modifier,
    fit: LinearFit? = null,
    goalMarker: Double? = null,
    goalMarkerLabel: String = "Ziel",
    markerColor: Color = pointColor,
    height: Int = 260,
) {
    if (points.isEmpty()) {
        EmptyScatterPanel(modifier = modifier, heightDp = height)
        return
    }

    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val zeroLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val markerRing = MaterialTheme.colorScheme.surfaceContainer

    // The upright marker belongs inside the axis: a target drawn hard against the frame, or clipped
    // away entirely, is the one thing the reader came to compare against.
    val xTicks = remember(points, goalMarker) {
        val values = points.map { it.x } + listOfNotNull(goalMarker)
        niceAxisTicks(min = values.min(), max = values.max(), targetSteps = 4)
    }
    // Zero is always on the y axis: it is "nothing changed", the line every dot is read against.
    val yTicks = remember(points) {
        val values = points.map { it.y }
        niceAxisTicks(min = minOf(values.min(), 0.0), max = maxOf(values.max(), 0.0), targetSteps = 4)
    }

    var selected by remember(points) { mutableStateOf<ScatterPoint?>(null) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SelectionReadout(
            selected = selected,
            xUnit = xUnit,
            yUnit = yUnit,
            pointColor = pointColor,
        )

        Row(modifier = Modifier.fillMaxWidth().height(height.dp)) {
            AxisColumn(ticks = yTicks)
            Spacer(Modifier.width(6.dp))

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(points, xTicks, yTicks) {
                        detectTapGestures { offset ->
                            // Nearest dot in the panel's own geometry, so a tap between two of them
                            // picks the one that looks nearer rather than the one that is nearer in
                            // kilocalories.
                            selected = points.minByOrNull { point ->
                                val dx = xTicks.fractionOf(point.x) * size.width - offset.x
                                val dy = (1f - yTicks.fractionOf(point.y)) * size.height - offset.y
                                dx * dx + dy * dy
                            }
                        }
                    },
            ) {
                fun xFor(value: Double) = size.width * xTicks.fractionOf(value)
                fun yFor(value: Double) = size.height * (1f - yTicks.fractionOf(value))

                yTicks.values.forEach { value ->
                    val y = yFor(value)
                    val isZero = value == 0.0
                    drawLine(
                        color = if (isZero) zeroLineColor else gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = if (isZero) 2f else 1f,
                    )
                }

                goalMarker?.let { value ->
                    val x = xFor(value)
                    drawLine(
                        color = markerColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    )
                }

                fit?.let { line ->
                    drawLine(
                        color = pointColor.copy(alpha = 0.75f),
                        start = Offset(xFor(xTicks.min), yFor(line.yAt(xTicks.min))),
                        end = Offset(xFor(xTicks.max), yFor(line.yAt(xTicks.max))),
                        strokeWidth = 4f,
                    )
                }

                points.forEachIndexed { index, point ->
                    // Oldest at a third of the alpha, newest full: enough of a ramp to see the order
                    // without the early weeks disappearing.
                    val age = if (points.size == 1) 1f else index.toFloat() / (points.size - 1)
                    drawCircle(
                        color = pointColor.copy(alpha = 0.35f + 0.65f * age),
                        radius = 7f,
                        center = Offset(xFor(point.x), yFor(point.y)),
                    )
                }

                selected?.let { point ->
                    val center = Offset(xFor(point.x), yFor(point.y))
                    drawCircle(color = pointColor, radius = 9f, center = center)
                    drawCircle(color = markerRing, radius = 9f, center = center, style = Stroke(width = 3f))
                }
            }
        }

        // The x labels sit under the plot only, not under the y gutter, or every one of them would
        // be drawn a gutter's width to the left of the value it names.
        Row(modifier = Modifier.fillMaxWidth()) {
            AxisColumnSpacer(ticks = yTicks)
            Spacer(Modifier.width(6.dp))
            Row(modifier = Modifier.weight(1f)) {
                val labels = listOf(xTicks.min, (xTicks.min + xTicks.max) / 2, xTicks.max)
                labels.forEachIndexed { index, value ->
                    Text(
                        value.formatCompact(),
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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "↑ $yAxisLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "→ $xAxisLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            goalMarker?.let {
                Text(
                    "┆ $goalMarkerLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = markerColor,
                )
            }
        }
    }
}

/** Where a value sits along its axis, 0 at the first tick and 1 at the last. */
private fun AxisTicks.fractionOf(value: Double): Float {
    val span = max - min
    if (span <= 0.0) return 0.5f
    return ((value - min) / span).toFloat()
}

/** The y axis' labels, evenly spaced because [niceAxisTicks] steps are. */
@Composable
private fun AxisColumn(ticks: AxisTicks) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ticks.values.reversed().forEach { value ->
            Text(
                value.formatCompact(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The same column with nothing in it, to line the x labels up under the plot area. */
@Composable
private fun AxisColumnSpacer(ticks: AxisTicks) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            ticks.values.maxByOrNull { it.formatCompact().length }?.formatCompact().orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Transparent,
        )
    }
}

/**
 * The tapped dot in words. Fixed floor like the line chart's readout, so selecting one never shifts
 * the plot under the finger.
 */
@Composable
private fun SelectionReadout(
    selected: ScatterPoint?,
    xUnit: String,
    yUnit: String,
    pointColor: Color,
) {
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp), contentAlignment = Alignment.CenterStart) {
        if (selected == null) {
            Text(
                "Tippe einen Punkt für seine Werte.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(selected.label, style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${selected.x.formatCompact()} $xUnit · ${selected.y.formatSigned()} $yUnit",
                        style = MaterialTheme.typography.labelMedium,
                        color = pointColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyScatterPanel(modifier: Modifier, heightDp: Int) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Box(
        modifier = modifier.fillMaxWidth().height(heightDp.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            listOf(0f, 0.5f, 1f).forEach { fraction ->
                val y = size.height * fraction
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
        }
        Text(
            "Keine vollständigen Wochen im Zeitraum.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
