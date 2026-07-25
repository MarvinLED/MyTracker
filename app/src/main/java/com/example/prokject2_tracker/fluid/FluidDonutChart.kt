package com.example.prokject2_tracker.fluid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class FluidSlice(val label: String, val value: Double, val color: Color)

/**
 * Ring chart for a part-to-whole split, with the headline value in the hole. Segments are separated
 * by a small gap of the surface colour so neighbouring fills never read as one wedge, and identity
 * is carried by the legend beside it — never by colour alone.
 */
@Composable
fun FluidDonutChart(
    slices: List<FluidSlice>,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.value }
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val gapDegrees = if (slices.size > 1) 2f else 0f

    Box(modifier = modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(132.dp)) {
            val strokeWidth = size.minDimension * 0.22f
            val inset = strokeWidth / 2f
            val arcSize = Size(size.minDimension - strokeWidth, size.minDimension - strokeWidth)
            val topLeft = Offset(inset, inset)

            if (total <= 0.0) {
                drawArc(
                    color = emptyColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )
                return@Canvas
            }

            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = (slice.value / total * 360.0).toFloat()
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    // Never eat the whole segment with the gap — a 1% slice must stay visible.
                    sweepAngle = (sweep - gapDegrees).coerceAtLeast(sweep * 0.35f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerValue,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Legend rows: a colour swatch for identity, all text in text colours rather than the series hue. */
@Composable
fun FluidChartLegend(slices: List<FluidSlice>, valueLabel: (FluidSlice) -> String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        slices.forEach { slice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = slice.color, shape = CircleShape, modifier = Modifier.size(10.dp)) {}
                Spacer(Modifier.width(8.dp))
                Text(
                    slice.label,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    valueLabel(slice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A titled chart block: heading, ring, legend. */
@Composable
fun FluidChartBlock(
    title: String,
    slices: List<FluidSlice>,
    centerValue: String,
    centerLabel: String,
    valueLabel: (FluidSlice) -> String,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        FluidDonutChart(slices = slices, centerValue = centerValue, centerLabel = centerLabel)
        if (slices.isEmpty()) {
            Text(
                emptyText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            FluidChartLegend(slices = slices, valueLabel = valueLabel)
        }
    }
}
