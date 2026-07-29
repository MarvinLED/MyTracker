package com.example.prokject2_tracker.nutrition.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

/** The three macros in the order they are drawn, top to bottom. */
private val MacroOrder = listOf(Nutrient.CARBS, Nutrient.PROTEIN, Nutrient.FAT)

private val BarShape = RoundedCornerShape(6.dp)
private val BarHeight = 14.dp

/**
 * How full a macro's bar runs.
 *
 * With a goal it is the share of that goal. Without one there is no target to measure against, so
 * the bar fills relative to the largest of the three macros instead — it then compares the macros
 * with each other, and the missing goal is spelled out in words beside it rather than implied.
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
 * The day's macros as three bars: Kohlenhydrate, Protein, Fett. Grams, not energy shares — the
 * number beside each bar is the one the goals are set in.
 */
@Composable
fun MacroBars(totals: NutritionTotals, goals: Map<Nutrient, NutrientGoal>, modifier: Modifier = Modifier) {
    val consumed = totals.byNutrient()
    // Only used when a macro has no goal of its own; see macroBarFraction.
    val peerMax = MacroOrder.maxOf { consumed[it] ?: 0.0 }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MacroOrder.forEach { nutrient ->
            val amount = consumed[nutrient] ?: 0.0
            val goal = goals[nutrient]?.value
            ValueBar(
                label = nutrient.label,
                // Without a goal there is no "/ 250 g" to show, and the note says why the bar is
                // still filled — otherwise it would look measured against a target that isn't set.
                value = if (goal != null) {
                    "${amount.formatCompact()} / ${goal.formatCompact()} ${nutrient.unit}"
                } else {
                    "${amount.formatCompact()} ${nutrient.unit}"
                },
                note = if (goal == null) "kein Ziel" else null,
                fraction = macroBarFraction(amount, goal, peerMax),
                color = MacroColors.getValue(nutrient),
            )
        }
    }
}

/** The day's energy against the calorie goal — the one goal that always has a value. */
@Composable
fun CalorieBar(consumedKcal: Double, goalKcal: Double, modifier: Modifier = Modifier) {
    ValueBar(
        label = "Kalorien",
        value = "${consumedKcal.formatCompact()} / ${goalKcal.formatCompact()} kcal",
        fraction = if (goalKcal > 0.0) (consumedKcal / goalKcal).toFloat().coerceIn(0f, 1f) else 0f,
        color = MaterialTheme.colorScheme.primary,
        // Calories are the headline of the day, so this bar is drawn heavier than the macros above.
        height = 20.dp,
        modifier = modifier,
    )
}

/**
 * One labelled bar: name on the left, value on the right, the bar underneath. The shared shape for
 * everything on this screen that is "x out of y", so the macros and the calories read as one family.
 */
@Composable
private fun ValueBar(
    label: String,
    value: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    note: String? = null,
    height: Dp = BarHeight,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            note?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
        // The track is the lowest surface step: against a lighter one the magenta fat fill drops
        // below the 3:1 a filled bar needs for its own edge to be readable.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(BarShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(BarShape)
                        .background(color),
                )
            }
        }
    }
}

/**
 * The two Flüssigkeiten rings folded into one bar: the width is the daily goal, the filled part is
 * what was drunk, and that part carries the drink types' own colours in their proportions. One bar
 * answers "how far along am I" and "out of what" at once, which two rings side by side did not.
 *
 * Which drink is which colour is a follow-up question, not the headline, so the legend stays folded
 * away until the bar is tapped.
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
    var showLegend by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .clickable(
                    enabled = slices.isNotEmpty(),
                    // Spoken by the screen reader in place of "double tap to activate", so the
                    // hidden legend is discoverable without seeing the bar change.
                    onClickLabel = if (showLegend) "Getränke ausblenden" else "Getränke anzeigen",
                ) { showLegend = !showLegend },
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
        when {
            slices.isEmpty() -> Text(
                "Noch nichts getrunken.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Identity by label, never by colour alone — which is exactly what the legend is for.
            showLegend -> FluidChartLegend(
                slices = slices,
                valueLabel = { "${it.value.formatCompact()} ml" },
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
