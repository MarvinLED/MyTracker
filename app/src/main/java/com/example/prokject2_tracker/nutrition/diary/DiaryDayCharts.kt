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
 * Hues per nutrient, so the same one is the same colour wherever it appears. Slots out of the app's
 * validated chart palette. The macros were chosen as a trio (all-pairs CVD ΔE 13.2 and
 * normal-vision ΔE 19.3, all ≥ 3:1 on the card surface); the second row takes three of the palette's
 * remaining slots, and since the two rows sit above each other the six are all distinct as a set.
 */
private val NutrientColors = mapOf(
    Nutrient.PROTEIN to Color(0xFF3987E5), // blau
    Nutrient.CARBS to Color(0xFFC98500), // gelb
    Nutrient.FAT to Color(0xFFD55181), // magenta
    Nutrient.SUGAR to Color(0xFFD95926), // orange
    Nutrient.FIBER to Color(0xFF199E70), // aqua
    Nutrient.SALT to Color(0xFF9085E9), // violett
)

/** The three macros, left to right — the row that is always visible. */
private val MacroOrder = listOf(Nutrient.CARBS, Nutrient.PROTEIN, Nutrient.FAT)

/** The second row, folded away until the macros are tapped. */
private val MinorNutrientOrder = listOf(Nutrient.SUGAR, Nutrient.FIBER, Nutrient.SALT)

private val BarShape = RoundedCornerShape(6.dp)
private val BarHeight = 20.dp
private val MacroBarHeight = 8.dp

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
 * The day's macros as three small bars side by side: Kohlenhydrate, Protein, Fett. Grams, not energy
 * shares — the number under each bar is the one the goals are set in.
 *
 * Deliberately compact: the three together take about as much room as the single calorie bar below
 * them, which is the right weight for a supporting detail next to the day's headline number.
 *
 * Tapping them unfolds a second row — Zucker, Ballaststoffe, Salz. Those are the follow-up question
 * to the macros, so they get the same shape but only on request.
 */
@Composable
fun MacroBars(totals: NutritionTotals, goals: Map<Nutrient, NutrientGoal>, modifier: Modifier = Modifier) {
    val consumed = totals.byNutrient()
    var showMore by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                // Spoken by the screen reader in place of "double tap to activate", so the folded
                // row is discoverable without seeing the bars change.
                onClickLabel = if (showMore) {
                    "Zucker, Ballaststoffe und Salz ausblenden"
                } else {
                    "Zucker, Ballaststoffe und Salz anzeigen"
                },
            ) { showMore = !showMore },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NutrientBarRow(nutrients = MacroOrder, consumed = consumed, goals = goals)
        if (showMore) {
            NutrientBarRow(nutrients = MinorNutrientOrder, consumed = consumed, goals = goals)
        }
    }
}

/** Three bars side by side, each a third of the width. */
@Composable
private fun NutrientBarRow(
    nutrients: List<Nutrient>,
    consumed: Map<Nutrient, Double>,
    goals: Map<Nutrient, NutrientGoal>,
) {
    // Only used for the nutrients in this row that have no goal of their own; see macroBarFraction.
    // Per row, not across both: 6 g of salt beside 250 g of carbs would never leave the floor.
    val peerMax = nutrients.maxOf { consumed[it] ?: 0.0 }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        nutrients.forEach { nutrient ->
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            nutrient.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Bar(
            fraction = macroBarFraction(consumed, goal, peerMax),
            color = NutrientColors.getValue(nutrient),
            height = MacroBarHeight,
        )
        Text(
            // A third of the width has no room for spaces around the slash. Without a goal there is
            // no "/ 250" part at all — the missing target is what the plain "120 g" says.
            if (goal != null) {
                "${consumed.formatCompact()}/${goal.formatCompact()} ${nutrient.unit}"
            } else {
                "${consumed.formatCompact()} ${nutrient.unit}"
            },
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
        modifier = modifier,
    )
}

/** One labelled bar: name on the left, value on the right, the bar underneath. */
@Composable
private fun ValueBar(
    label: String,
    value: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
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
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
        Bar(fraction = fraction, color = color, height = height)
    }
}

/**
 * The plain bar itself. The track is the lowest surface step: against a lighter one the magenta fat
 * fill drops below the 3:1 a filled bar needs for its own edge to be readable.
 */
@Composable
private fun Bar(fraction: Float, color: Color, height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
                .height(BarHeight)
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
