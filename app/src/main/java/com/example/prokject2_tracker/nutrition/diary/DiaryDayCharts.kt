package com.example.prokject2_tracker.nutrition.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.prokject2_tracker.core.datastore.Nutrient
import com.example.prokject2_tracker.core.datastore.NutrientGoal
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.fluid.FluidChartLegend
import com.example.prokject2_tracker.fluid.FluidSlice
import com.example.prokject2_tracker.nutrition.NutritionTotals

/**
 * Hues for the three macros, so the same nutrient is the same colour wherever it appears. Slots out
 * of the app's validated chart palette, chosen as a trio: all-pairs CVD ΔE 13.2 and normal-vision
 * ΔE 19.3 against each other, all ≥ 3:1 on the card surface.
 */
private val MacroColors = mapOf(
    Nutrient.PROTEIN to Color(0xFF3987E5), // blau
    Nutrient.CARBS to Color(0xFFC98500), // gelb
    Nutrient.FAT to Color(0xFFD55181), // magenta
)

/** The three macros in the order they are drawn, left to right. */
private val MacroOrder = listOf(Nutrient.CARBS, Nutrient.PROTEIN, Nutrient.FAT)

private val BarShape = RoundedCornerShape(6.dp)

/**
 * How full a macro's bar stands.
 *
 * With a goal it is the share of that goal. Without one there is no target to measure against, so
 * the bar fills relative to the largest of the three macros instead — it then compares the macros
 * with each other, and the missing goal is spelled out in words underneath rather than implied.
 */
fun macroBarFraction(consumed: Double, goal: Double?, peerMax: Double): Float {
    val reference = if (goal != null && goal > 0.0) goal else peerMax
    if (reference <= 0.0) return 0f
    return (consumed / reference).toFloat().coerceIn(0f, 1f)
}

/** Widths of a fluid bar as fractions of its full width; [segments] plus [open] always make 1. */
data class FluidBarWidths(val segments: List<Float>, val open: Float)

/**
 * Splits the fluid bar: the bar's full width is the daily goal, the filled part is what was drunk,
 * and that filled part is divided between the drink types in [amountsMl].
 *
 * Past the goal the bar is simply full and the segments divide the whole width between them — the
 * ratio stays honest even though "how much of the goal" has stopped being the question.
 */
fun fluidBarSegments(amountsMl: List<Double>, goalMl: Double): FluidBarWidths {
    val total = amountsMl.sum()
    val reference = maxOf(goalMl, total)
    if (reference <= 0.0) return FluidBarWidths(amountsMl.map { 0f }, 1f)
    val segments = amountsMl.map { (it / reference).toFloat().coerceIn(0f, 1f) }
    return FluidBarWidths(segments, (1f - segments.sum()).coerceAtLeast(0f))
}

/**
 * The day's macros as three upright bars side by side: Kohlenhydrate, Protein, Fett. Grams, not
 * energy shares — the number underneath each bar is the one the goals are set in.
 */
@Composable
fun MacroBars(totals: NutritionTotals, goals: Map<Nutrient, NutrientGoal>, modifier: Modifier = Modifier) {
    val consumed = totals.byNutrient()
    // Only used when a macro has no goal of its own; see macroBarFraction.
    val peerMax = MacroOrder.maxOf { consumed[it] ?: 0.0 }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MacroOrder.forEach { nutrient ->
            MacroBar(
                nutrient = nutrient,
                consumed = consumed[nutrient] ?: 0.0,
                goal = goals[nutrient]?.value,
                peerMax = peerMax,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MacroBar(
    nutrient: Nutrient,
    consumed: Double,
    goal: Double?,
    peerMax: Double,
    modifier: Modifier = Modifier,
) {
    val fraction = macroBarFraction(consumed, goal, peerMax)
    val color = MacroColors.getValue(nutrient)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            nutrient.label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        // The track is the lowest surface step: against a lighter one the magenta fat fill drops
        // below the 3:1 a filled bar needs for its own edge to be readable.
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(110.dp)
                .clip(BarShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction)
                        .clip(BarShape)
                        .background(color),
                )
            }
        }
        Text(
            if (goal != null) {
                "${consumed.formatCompact()} / ${goal.formatCompact()} ${nutrient.unit}"
            } else {
                "${consumed.formatCompact()} ${nutrient.unit}"
            },
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        // Without this line the bar would look like it was measured against a goal that isn't set.
        if (goal == null) {
            Text(
                "kein Ziel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The day's energy against the calorie goal — the one goal that always has a value. */
@Composable
fun CalorieBar(consumedKcal: Double, goalKcal: Double, modifier: Modifier = Modifier) {
    val fraction = if (goalKcal > 0.0) (consumedKcal / goalKcal).toFloat().coerceIn(0f, 1f) else 0f

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Kalorien", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                "${consumedKcal.formatCompact()} / ${goalKcal.formatCompact()} kcal",
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(BarShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(BarShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

/**
 * The two Flüssigkeiten rings folded into one bar: the width is the daily goal, the filled part is
 * what was drunk, and that part carries the drink types' own colours in their proportions. One bar
 * answers "how far along am I" and "out of what" at once, which two rings side by side did not.
 */
@Composable
fun FluidBalanceBar(
    slices: List<FluidSlice>,
    goalMl: Double,
    modifier: Modifier = Modifier,
) {
    val totalMl = slices.sumOf { it.value }
    val widths = fluidBarSegments(slices.map { it.value }, goalMl)
    val percent = if (goalMl > 0.0) Math.round(totalMl / goalMl * 100.0) else 0L

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Flüssigkeit", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                if (goalMl > 0.0) {
                    "${totalMl.formatCompact()} / ${goalMl.formatCompact()} ml · $percent %"
                } else {
                    "${totalMl.formatCompact()} ml"
                },
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(BarShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            slices.forEachIndexed { index, slice ->
                val width = widths.segments.getOrElse(index) { 0f }
                if (width > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(width)
                            .background(slice.color),
                    )
                }
            }
            if (widths.open > 0f) Spacer(Modifier.weight(widths.open))
        }
        if (slices.isEmpty()) {
            Text(
                "Noch nichts getrunken.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // Identity by label, never by colour alone.
            FluidChartLegend(slices = slices, valueLabel = { "${it.value.formatCompact()} ml" })
        }
    }
}
