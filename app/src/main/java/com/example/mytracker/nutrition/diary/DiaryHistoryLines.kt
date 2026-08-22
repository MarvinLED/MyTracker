package com.example.mytracker.nutrition.diary

import com.example.mytracker.core.datastore.Nutrient
import com.example.mytracker.core.datastore.NutrientGoal
import com.example.mytracker.core.metrics.EpochDayRange
import com.example.mytracker.core.metrics.Granularity
import com.example.mytracker.core.metrics.MetricAggregation
import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.core.metrics.bucketBy
import com.example.mytracker.core.ui.ChartLine
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.goals.NutrientGoalChange
import com.example.mytracker.goals.nutrientGoalTimeline
import com.example.mytracker.weight.BodyWeightEntry

/**
 * The chart's lines, in [DiaryHistorySeries] order so a series keeps its place in the legend no
 * matter which others are switched on.
 *
 * Coarser granularities average rather than sum, for both Soll and Ist. A week's *total* calories
 * would be seven times the daily target and could not be read against it at all; the mean day can.
 * That also keeps both members of a pair on the same footing, which is the whole point of drawing
 * them together.
 *
 * An Ist point is only drawn for a day that had a Soll above zero — see [goalDays]. Gewicht is
 * exempt: it is not a nutrient and has no target to be conditional on.
 */
fun diaryHistoryLines(
    selected: Set<DiaryHistorySeries>,
    range: EpochDayRange,
    granularity: Granularity,
    nutritionTotals: List<DailyNutritionTotals>,
    weights: List<BodyWeightEntry>,
    goalChanges: List<NutrientGoalChange>,
    currentGoals: Map<Nutrient, NutrientGoal>,
): List<ChartLine> {
    // One timeline per nutrient, not per line: a nutrient's Soll, Ist and Ø all read the same one.
    val timelines = mutableMapOf<Nutrient, List<MetricPoint>>()
    fun goalTimeline(nutrient: Nutrient): List<MetricPoint> = timelines.getOrPut(nutrient) {
        nutrientGoalTimeline(
            range = range,
            changes = goalChanges.filter { it.nutrient == nutrient },
            currentGoal = currentGoals[nutrient],
        )
    }

    return DiaryHistorySeries.entries
        .filter { it in selected }
        .map { series ->
            fun actualPoints(): List<MetricPoint> {
                val nutrient = series.nutrient ?: return emptyList()
                val days = goalDays(goalTimeline(nutrient))
                return nutritionTotals.mapNotNull { day ->
                    if (day.epochDay !in days) return@mapNotNull null
                    day.totals.byNutrient()[nutrient]?.let { MetricPoint(day.epochDay, it) }
                }
            }

            val points = when {
                series == DiaryHistorySeries.WEIGHT ->
                    weights.map { MetricPoint(it.epochDay, it.weightKg) }

                series.isGoal -> goalTimeline(series.nutrient!!)

                series.kind == DiaryHistorySeriesKind.AVERAGE -> weeklyAverages(range, actualPoints())

                else -> actualPoints()
            }
            ChartLine(
                label = series.label,
                unit = series.unit,
                color = series.color,
                points = points.bucketBy(granularity, MetricAggregation.AVERAGE),
                // No series here is anchored at zero. What the Verlauf is read for is movement —
                // whether the Ist tracks its Soll, whether either drifted — and every one of these
                // metrics lives far above zero, so a floor at zero spends most of the panel on
                // empty space and squeezes the actual variation into the top band.
                zeroBased = false,
                style = series.style,
                markers = series.showsMarkers,
            )
        }
}

/**
 * The days an Ist value may be drawn on: those the nutrient had a target above zero for.
 *
 * The Ist line exists to be read against its Soll. On a day with no target — the nutrient was not
 * tracked yet, or the goal was cleared — an intake point has nothing to be measured against and
 * reads as a shortfall or an excess that was never defined. A Soll of exactly zero says the same
 * thing: the app writes no goal it means as "eat nothing", so a zero there is an unset target.
 */
private fun goalDays(timeline: List<MetricPoint>): Set<Long> =
    timeline.mapNotNullTo(mutableSetOf()) { point -> point.epochDay.takeIf { point.value > 0.0 } }

/**
 * The Ist values as one figure per calendar week, repeated on every day of that week: a step that
 * holds while the week does and changes on Monday. Repeating it rather than plotting one point per
 * week is what makes it readable against the daily Ist line — a weekly point joined to the next
 * would slope across days whose average it never was.
 *
 * Only logged days count towards the mean, and a week without a single logged day gets no points
 * at all: a zero there would read as "ate nothing", not "did not log". Days the Ist line skips for
 * want of a Soll are skipped here too — the Ø is the mean of what is drawn.
 */
private fun weeklyAverages(range: EpochDayRange, daily: List<MetricPoint>): List<MetricPoint> {
    if (daily.isEmpty()) return emptyList()
    val meanByWeek = daily
        .groupBy { DateUtils.startOfWeekEpochDay(it.epochDay) }
        .mapValues { (_, points) -> points.sumOf { it.value } / points.size }

    return (range.startInclusive..range.endInclusive).mapNotNull { day ->
        meanByWeek[DateUtils.startOfWeekEpochDay(day)]?.let { MetricPoint(day, it) }
    }
}
