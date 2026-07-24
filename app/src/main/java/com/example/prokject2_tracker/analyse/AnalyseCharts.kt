package com.example.prokject2_tracker.analyse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.util.formatCompact

data class ChartSeries(
    val points: List<MetricPoint>,
    val label: String,
    val unit: String,
    val color: Color,
)

/**
 * Line chart for the Analyse comparison view. [primary] is always drawn (0-based Y-axis, min/max
 * labeled top/bottom-left); if [secondary] is present it's overlaid with its own independent
 * 0-based scale (labeled top/bottom-right) rather than sharing [primary]'s axis, since the two
 * series can have entirely different units (e.g. kg vs bpm).
 */
@Composable
fun ComparisonLineChart(primary: ChartSeries, secondary: ChartSeries? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            if (primary.points.size >= 2) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val allEpochDays = primary.points.map { it.epochDay } + secondary?.points.orEmpty().map { it.epochDay }
                    val minEpochDay = allEpochDays.min()
                    val dayRange = (allEpochDays.max() - minEpochDay).coerceAtLeast(1)
                    fun xFor(epochDay: Long) = size.width * (epochDay - minEpochDay).toFloat() / dayRange

                    fun drawSeries(series: ChartSeries) {
                        if (series.points.size < 2) return
                        val maxValue = series.points.maxOf { it.value }.coerceAtLeast(1.0)
                        fun yFor(value: Double) = size.height - (size.height * (value / maxValue)).toFloat()
                        val sorted = series.points.sortedBy { it.epochDay }
                        for (i in 0 until sorted.size - 1) {
                            drawLine(
                                color = series.color,
                                start = Offset(xFor(sorted[i].epochDay), yFor(sorted[i].value)),
                                end = Offset(xFor(sorted[i + 1].epochDay), yFor(sorted[i + 1].value)),
                                strokeWidth = 4f,
                            )
                        }
                    }
                    drawSeries(primary)
                    secondary?.let { drawSeries(it) }
                }
                val primaryMax = primary.points.maxOf { it.value }.coerceAtLeast(1.0)
                Text(
                    primaryMax.formatCompact(),
                    color = primary.color,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.TopStart),
                )
                Text(
                    "0",
                    color = primary.color,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
                if (secondary != null && secondary.points.size >= 2) {
                    val secondaryMax = secondary.points.maxOf { it.value }.coerceAtLeast(1.0)
                    Text(
                        secondaryMax.formatCompact(),
                        color = secondary.color,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                    Text(
                        "0",
                        color = secondary.color,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }
            } else {
                Text(
                    "Nicht genug Datenpunkte im Zeitraum.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            ChartLegendEntry(primary)
            secondary?.let {
                Box(modifier = Modifier.width(16.dp))
                ChartLegendEntry(it)
            }
        }
    }
}

@Composable
private fun ChartLegendEntry(series: ChartSeries) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(series.color, CircleShape))
        Box(modifier = Modifier.width(6.dp))
        Text("${series.label} (${series.unit})", style = MaterialTheme.typography.labelMedium)
    }
}
