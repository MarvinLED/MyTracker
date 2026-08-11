package com.example.prokject2_tracker.nutrition.diary

import com.example.prokject2_tracker.core.datastore.Nutrient
import com.example.prokject2_tracker.core.datastore.NutrientGoal
import com.example.prokject2_tracker.core.metrics.EpochDayRange
import com.example.prokject2_tracker.core.metrics.Granularity
import com.example.prokject2_tracker.core.metrics.MetricAggregation
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.metrics.bucketBy
import com.example.prokject2_tracker.core.ui.ChartLine
import com.example.prokject2_tracker.goals.NutrientGoalChange
import com.example.prokject2_tracker.goals.nutrientGoalTimeline
import com.example.prokject2_tracker.weight.BodyWeightEntry

/**
 * The chart's lines, in [DiaryHistorySeries] order so a series keeps its place in the legend no
 * matter which others are switched on.
 *
 * Coarser granularities average rather than sum, for both Soll and Ist. A week's *total* calories
 * would be seven times the daily target and could not be read against it at all; the mean day can.
 * That also keeps both members of a pair on the same footing, which is the whole point of drawing
 * them together.
 */
fun diaryHistoryLines(
    selected: Set<DiaryHistorySeries>,
    range: EpochDayRange,
    granularity: Granularity,
    nutritionTotals: List<DailyNutritionTotals>,
    weights: List<BodyWeightEntry>,
    goalChanges: List<NutrientGoalChange>,
    currentGoals: Map<Nutrient, NutrientGoal>,
): List<ChartLine> = DiaryHistorySeries.entries
    .filter { it in selected }
    .map { series ->
        val points = when {
            series == DiaryHistorySeries.WEIGHT ->
                weights.map { MetricPoint(it.epochDay, it.weightKg) }

            series.isGoal -> nutrientGoalTimeline(
                range = range,
                changes = goalChanges.filter { it.nutrient == series.nutrient },
                currentGoal = currentGoals[series.nutrient],
            )

            else -> {
                val nutrient = series.nutrient
                nutritionTotals.mapNotNull { day ->
                    day.totals.byNutrient()[nutrient]?.let { MetricPoint(day.epochDay, it) }
                }
            }
        }
        ChartLine(
            label = series.label,
            unit = series.unit,
            color = series.color,
            points = points.bucketBy(granularity, MetricAggregation.AVERAGE),
            // Body weight is the one series whose day-to-day movement is tiny next to its absolute
            // value; anchored at zero it would flatten into a straight edge.
            zeroBased = series != DiaryHistorySeries.WEIGHT,
            dashed = series.dashed,
        )
    }
