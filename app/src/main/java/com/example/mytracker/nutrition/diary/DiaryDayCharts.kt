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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.compositeOver
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
val NutrientColors = mapOf(
    Nutrient.PROTEIN to Color(0xFF3987E5), // blau
    Nutrient.CARBS to Color(0xFFC98500), // gelb
    Nutrient.FAT to Color(0xFFD55181), // magenta
    Nutrient.SUGAR to Color(0xFFD95926), // orange
    Nutrient.FIBER to Color(0xFF199E70), // aqua
    Nutrient.SALT to Color(0xFF9085E9), // violett
)

/**
 * The two palette slots [NutrientColors] leaves unused. Kalorien is not in that map because the
 * day's calories are their own bar rather than one of the nutrient bars, and Gewicht is not a
 * nutrient at all — but on the Verlauf chart both are lines beside the nutrients and need a hue out
 * of the same eight, or they would collide with one.
 */
val KcalColor = Color(0xFFE66767) // rot
val WeightColor = Color(0xFF0E9A2B) // grün

/** The three macros, left to right — the row that is always visible. */
private val MacroOrder = listOf(Nutrient.CARBS, Nutrient.PROTEIN, Nutrient.FAT)

/** The second row, folded away until the macros are tapped. */
private val MinorNutrientOrder = listOf(Nutrient.SUGAR, Nutrient.FIBER, Nutrient.SALT)

private val BarShape = RoundedCornerShape(6.dp)
private val BarHeight = 20.dp
private val MacroBarHeight = 8.dp
private val MarkerWidth = 2.dp

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

/**
 * The target half of a bar's label: `"150"` for an upper bound alone, `"≥100"` for a lower bound
 * alone, `"100–150"` for both. Null when there is no goal, and the label then shows the plain
 * amount without a target at all.
 */
fun goalTargetLabel(min: Double?, max: Double?): String? = when {
    min != null && max != null -> "${min.formatCompact()}–${max.formatCompact()}"
    max != null -> max.formatCompact()
    min != null -> "≥${min.formatCompact()}"
    else -> null
}

/**
 * Berechnet die Farbe eines Nährwert-Balkens basierend auf dem Zielbereich:
 * - Min + Max: Grün im Range, Gelb davor, Rot über Max
 * - Nur Min: Gelb kurz davor (bei ~90% von min), Grün wenn erreicht
 * - Nur Max: Gradient (grüner je kleiner), Gelb bei Max, Rot wenn über Max
 * - Kein Ziel: Blau
 */
fun nutritionBarColor(
    consumed: Double,
    goal: NutrientGoal?,
    baseColor: Color,
    greenColor: Color = Color(0xFF4CAF50),
    yellowColor: Color = Color(0xFFFFC107),
    redColor: Color = Color(0xFFF44336),
    noGoalColor: Color = Color(0xFF2196F3),
): Color {
    if (goal == null || goal.isEmpty) return noGoalColor

    val min = goal.min
    val max = goal.max

    return when {
        min != null && max != null -> {
            // Min + Max vorhanden
            when {
                consumed >= min && consumed <= max -> greenColor
                consumed < min -> yellowColor
                else -> redColor // consumed > max
            }
        }
        min != null -> {
            // Nur Minimum
            val warningThreshold = min * 0.9
            when {
                consumed >= min -> greenColor
                consumed >= warningThreshold -> yellowColor
                else -> {
                    // Gradient: je weiter weg vom Ziel, desto mehr Rot
                    val progress = (consumed / warningThreshold).coerceIn(0.0, 1.0).toFloat()
                    interpolateColor(redColor, yellowColor, progress)
                }
            }
        }
        max != null -> {
            // Nur Maximum
            when {
                consumed > max -> redColor
                consumed >= max * 0.95 -> yellowColor
                else -> {
                    // Gradient: je kleiner, desto grüner
                    val progress = (consumed / max).coerceIn(0.0, 1.0).toFloat()
                    interpolateColor(greenColor, yellowColor, progress)
                }
            }
        }
        else -> baseColor
    }
}

/** Interpoliert zwischen zwei Farben. [progress] sollte zwischen 0 und 1 liegen. */
private fun interpolateColor(start: Color, end: Color, progress: Float): Color {
    val p = progress.coerceIn(0f, 1f).toDouble()
    return Color(
        red = (start.red * (1.0 - p) + end.red * p).toFloat(),
        green = (start.green * (1.0 - p) + end.green * p).toFloat(),
        blue = (start.blue * (1.0 - p) + end.blue * p).toFloat(),
        alpha = (start.alpha * (1.0 - p) + end.alpha * p).toFloat(),
    )
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
                goal = goals[nutrient],
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
    goal: NutrientGoal?,
    peerMax: Double,
    modifier: Modifier = Modifier,
) {
    val target = goalTargetLabel(goal?.min, goal?.max)
    val baseColor = NutrientColors.getValue(nutrient)
    val barColor = nutritionBarColor(consumed, goal, baseColor)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            nutrient.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Bar(
            fraction = macroBarFraction(consumed, goal?.barTarget, peerMax),
            color = barColor,
            height = MacroBarHeight,
            markerFraction = goal?.minMarkerFraction,
        )
        Text(
            // A third of the width has no room for spaces around the slash. Without a goal there is
            // no "/ 250" part at all — the missing target is what the plain "120 g" says.
            if (target != null) {
                "${consumed.formatCompact()}/$target ${nutrient.unit}"
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
fun CalorieBar(consumedKcal: Double, goal: NutrientGoal, modifier: Modifier = Modifier) {
    val target = goalTargetLabel(goal.min, goal.max)
    val barColor = nutritionBarColor(consumedKcal, goal, MaterialTheme.colorScheme.primary)

    ValueBar(
        label = "Kalorien",
        value = if (target != null) {
            "${consumedKcal.formatCompact()} / $target kcal"
        } else {
            "${consumedKcal.formatCompact()} kcal"
        },
        fraction = goal.fractionOf(consumedKcal),
        color = barColor,
        markerFraction = goal.minMarkerFraction,
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
    markerFraction: Float? = null,
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
        Bar(fraction = fraction, color = color, height = height, markerFraction = markerFraction)
    }
}

/**
 * The plain bar itself. The track is the lowest surface step: against a lighter one the magenta fat
 * fill drops below the 3:1 a filled bar needs for its own edge to be readable.
 *
 * [markerFraction] draws a line across the bar where the goal's lower bound sits, so a goal with
 * both bounds shows at a glance whether the day is under, inside or over its range.
 */
@Composable
private fun Bar(
    fraction: Float,
    color: Color,
    height: Dp,
    modifier: Modifier = Modifier,
    markerFraction: Float? = null,
) {
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
        markerFraction?.let { marker ->
            // Drawn as a gap in the bar rather than a coloured tick: a tick would be one more hue
            // to tell apart, and the notch reads the same whichever fill it lands on.
            Box(modifier = Modifier.fillMaxWidth(marker), contentAlignment = Alignment.CenterEnd) {
                Box(
                    modifier = Modifier
                        .width(MarkerWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface),
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
 * away until the bar is tapped — together with [expandedContent], which is where the caller puts
 * what one does about the answer (the Schnellauswahl for logging a drink).
 */
@Composable
fun FluidBalanceBar(
    slices: List<FluidSlice>,
    goalMl: Double,
    modifier: Modifier = Modifier,
    expandedContent: @Composable () -> Unit = {},
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
                    // Tappable even with an empty bar: what unfolds is not only the legend but the
                    // Schnellauswahl, and a day one hasn't drunk anything on is exactly when that
                    // is wanted.
                    // The label is spoken by the screen reader in place of "double tap to
                    // activate", so the folded-away section is discoverable without seeing the bar
                    // change.
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
        if (showLegend) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            expandedContent()
        }
    }
}
