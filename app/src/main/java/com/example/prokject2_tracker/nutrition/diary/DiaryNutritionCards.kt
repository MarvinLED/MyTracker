package com.example.prokject2_tracker.nutrition.diary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.prokject2_tracker.core.datastore.Nutrient
import com.example.prokject2_tracker.core.datastore.NutrientGoal
import com.example.prokject2_tracker.core.datastore.NutrientGoalType
import com.example.prokject2_tracker.core.datastore.label
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.fluid.FluidChartBlock
import com.example.prokject2_tracker.fluid.FluidSlice
import com.example.prokject2_tracker.nutrition.NutritionMath
import com.example.prokject2_tracker.nutrition.NutritionTotals

/**
 * Hues for the three macros, reused by the ring and by their progress bars so the same nutrient is
 * the same colour wherever it appears. Slots out of the app's validated chart palette, chosen as a
 * trio: all-pairs CVD ΔE 13.2 and normal-vision ΔE 19.3 against each other, all ≥ 3:1 on the card
 * surface. The remaining nutrients have no ring to appear in, so their bars take the theme primary.
 */
private val MacroColors = mapOf(
    Nutrient.PROTEIN to Color(0xFF3987E5), // blau
    Nutrient.CARBS to Color(0xFFC98500), // gelb
    Nutrient.FAT to Color(0xFFD55181), // magenta
)

/**
 * The day's macro split by *energy*, not by grams: a gram of fat carries 9 kcal against a gram of
 * protein's 4, so a gram-based ring would show fat as less than half of what it actually contributes.
 */
@Composable
fun MacroEnergyRing(totals: NutritionTotals, modifier: Modifier = Modifier) {
    val shares = NutritionMath.macroEnergyShare(totals)
    val slices = listOf(Nutrient.PROTEIN, Nutrient.CARBS, Nutrient.FAT).mapNotNull { nutrient ->
        val energy = shares[nutrient]?.takeIf { it > 0.0 } ?: return@mapNotNull null
        FluidSlice(label = nutrient.label, value = energy, color = MacroColors.getValue(nutrient))
    }
    val macroKcal = shares.values.sum()
    val dominant = shares.maxByOrNull { it.value }

    FluidChartBlock(
        title = "Makros",
        slices = slices,
        // Deliberately *not* a kcal figure: the macros' energy can differ from the day's logged kcal
        // (rounding, alcohol, fibre, or simply incomplete macro data on a food), and two kcal numbers
        // that disagree on one screen read as a bug. The share is what the ring is here to show.
        centerValue = dominant
            ?.let { "${(it.value / macroKcal * 100.0).formatCompact()} %" }
            .orEmpty(),
        centerLabel = dominant?.key?.label.orEmpty(),
        valueLabel = { slice ->
            val percent = if (macroKcal > 0.0) slice.value / macroKcal * 100.0 else 0.0
            "${percent.formatCompact()} %"
        },
        emptyText = "Noch nichts geloggt.",
        modifier = modifier,
    )
}

/**
 * One bar per nutrient the user set a goal for. The bar reads differently per goal type, which is the
 * whole point of the type: a "höchstens" goal that has been blown is the only state drawn as a
 * problem, and it carries an icon and a word as well as a colour.
 */
@Composable
fun NutrientGoalBars(
    totals: NutritionTotals,
    goals: Map<Nutrient, NutrientGoal>,
    modifier: Modifier = Modifier,
) {
    if (goals.isEmpty()) return
    val consumed = totals.byNutrient()

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ziele heute", style = MaterialTheme.typography.titleSmall)
            Nutrient.entries.forEach { nutrient ->
                val goal = goals[nutrient] ?: return@forEach
                NutrientGoalBar(
                    nutrient = nutrient,
                    goal = goal,
                    consumed = consumed[nutrient] ?: 0.0,
                )
            }
        }
    }
}

@Composable
private fun NutrientGoalBar(nutrient: Nutrient, goal: NutrientGoal, consumed: Double) {
    val exceeded = goal.isExceededBy(consumed)
    val met = goal.isMetBy(consumed)
    val barColor = when {
        exceeded -> MaterialTheme.colorScheme.error
        else -> MacroColors[nutrient] ?: MaterialTheme.colorScheme.primary
    }

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = barColor,
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.size(10.dp),
            ) {}
            Spacer(Modifier.width(8.dp))
            Text(nutrient.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            // The goal type goes in the target itself ("0 / mind. 100 g"), so the status line below
            // doesn't have to repeat it as "noch 100 g bis mindestens 100 g".
            val target = when (goal.type) {
                NutrientGoalType.MIN -> "mind. ${goal.value.formatCompact()}"
                NutrientGoalType.MAX -> "max. ${goal.value.formatCompact()}"
                NutrientGoalType.EXACT -> goal.value.formatCompact()
            }
            Text(
                "${consumed.formatCompact()} / $target ${nutrient.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The track is the *lowest* surface step, not the highest: against a lighter track the
        // magenta fat fill only reached 2.83:1, below the 3:1 a filled bar needs for its own edge to
        // be readable. On this step the weakest fill clears 3.91:1.
        LinearProgressIndicator(
            progress = { goal.fractionOf(consumed) },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
        // State in words, never colour alone: "höchstens 50 g" and "mindestens 50 g" fill the same
        // bar the same way, and only the label says whether that is good news.
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (exceeded) {
                Icon(
                    Icons.Filled.PriorityHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp),
                )
            } else if (met) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.size(14.dp),
                )
            }
            if (exceeded || met) Spacer(Modifier.width(4.dp))
            Text(
                statusText(nutrient, goal, consumed, exceeded, met),
                style = MaterialTheme.typography.bodySmall,
                color = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun statusText(
    nutrient: Nutrient,
    goal: NutrientGoal,
    consumed: Double,
    exceeded: Boolean,
    met: Boolean,
): String {
    val remaining = goal.value - consumed
    val unit = nutrient.unit
    return when {
        exceeded -> "${(-remaining).formatCompact()} $unit über dem Höchstwert"
        met -> when (goal.type) {
            NutrientGoalType.MAX -> "im Rahmen — noch ${remaining.formatCompact()} $unit frei"
            else -> "erreicht"
        }
        goal.type == NutrientGoalType.MAX -> "noch ${remaining.formatCompact()} $unit frei"
        else -> "noch ${remaining.formatCompact()} $unit"
    }
}
