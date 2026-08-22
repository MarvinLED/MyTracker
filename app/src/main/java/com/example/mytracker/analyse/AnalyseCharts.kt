package com.example.prokject2_tracker.analyse

import androidx.compose.ui.graphics.Color
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.ui.ChartLine

/**
 * A metric series as the Analyse view model produces it. [color] is a placeholder the screen
 * overrides with a palette slot — the view model has no access to the theme.
 */
data class ChartSeries(
    val points: List<MetricPoint>,
    val label: String,
    val unit: String,
    val color: Color,
)

/**
 * Hands the series to the shared [com.example.prokject2_tracker.core.ui.DatedLineChart].
 *
 * Body weight is the one series that must not be drawn on a zero-based axis: a range of 78–80 kg on
 * an axis starting at 0 is a flat line, which is the opposite of what the Analyse view is for.
 */
fun ChartSeries.toChartLine(color: Color): ChartLine = ChartLine(
    label = label,
    unit = unit,
    color = color,
    points = points,
    zeroBased = unit != "kg" && unit != "lb",
)
