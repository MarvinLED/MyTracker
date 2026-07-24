package com.example.prokject2_tracker.analyse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.example.prokject2_tracker.core.metrics.MetricPoint

/**
 * Minimal hand-rolled line chart; swap for a real charting library if the Analyse domain grows.
 *
 * [zeroBased] controls the Y-axis floor: `true` (default) always starts at 0.0. Pass `false` for
 * series like body weight where day-to-day variation is narrow relative to the absolute value — a
 * zero-based axis would flatten the line to near-invisibility; the floor instead becomes
 * `min(values)` minus 10% padding of the value range.
 */
@Composable
fun SimpleLineChart(points: List<MetricPoint>, modifier: Modifier = Modifier, zeroBased: Boolean = true) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        if (points.size < 2) return@Canvas
        val maxValue = points.maxOf { it.value }.coerceAtLeast(1.0)
        val minValue = if (zeroBased) {
            0.0
        } else {
            val rawMin = points.minOf { it.value }
            rawMin - (maxValue - rawMin) * 0.1
        }
        val valueRange = (maxValue - minValue).let { if (it > 0) it else 1.0 }
        val minEpochDay = points.minOf { it.epochDay }
        val maxEpochDay = points.maxOf { it.epochDay }
        val dayRange = (maxEpochDay - minEpochDay).coerceAtLeast(1)

        fun xFor(epochDay: Long) = size.width * (epochDay - minEpochDay).toFloat() / dayRange
        fun yFor(value: Double) = size.height - (size.height * ((value - minValue) / valueRange)).toFloat()

        val sorted = points.sortedBy { it.epochDay }
        for (i in 0 until sorted.size - 1) {
            drawLine(
                color = lineColor,
                start = Offset(xFor(sorted[i].epochDay), yFor(sorted[i].value)),
                end = Offset(xFor(sorted[i + 1].epochDay), yFor(sorted[i + 1].value)),
                strokeWidth = 4f,
            )
        }
    }
}
