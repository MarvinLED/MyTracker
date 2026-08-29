package com.example.mytracker.core.metrics

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * A y axis snapped to round numbers. [values] are the labelled steps from bottom to top; [min] and
 * [max] are the padded bounds the plot should use, so the outermost ticks sit exactly on the edges.
 */
data class AxisTicks(val values: List<Double>) {
    val min: Double get() = values.first()
    val max: Double get() = values.last()
}

/**
 * The same axis stretched upwards to [count] labelled steps, for a chart that draws two axes beside
 * one plot. Their labels are spread evenly over one shared height, so they line up with each other —
 * and with the grid — only if both carry the same number of steps. Extending adds headroom at the
 * top and keeps the round step; it never moves a point or narrows the axis.
 */
fun AxisTicks.extendedTo(count: Int): AxisTicks {
    if (values.size < 2 || values.size >= count) return this
    val step = values[1] - values[0]
    return AxisTicks(List(count) { values.first() + it * step })
}

/**
 * Round steps covering [min]..[max], roughly [targetSteps] of them. "Roughly", because a step of
 * 1, 2, 2.5 or 5 times a power of ten is what makes a value readable off a grid line — 372,4 spaced
 * ticks are arithmetically correct and useless. The bounds are widened to the next tick in each
 * direction, never narrowed, so no point can fall outside the axis.
 */
fun niceAxisTicks(min: Double, max: Double, targetSteps: Int = 5): AxisTicks {
    val steps = targetSteps.coerceAtLeast(1)
    // A flat series still needs an axis with two ends; give it one scaled to the value itself.
    val span = (max - min).let { if (it > 0) it else abs(max).coerceAtLeast(1.0) }

    val rawStep = span / steps
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    // Snap to 1, 2, 5 or 10 times the magnitude — the multiples people count in. The thresholds sit
    // between them rather than on them, so a raw step of 1,2 rounds down to 1 instead of up to 2 and
    // the axis keeps roughly the step count that was asked for.
    val step = magnitude * when (rawStep / magnitude) {
        in 0.0..1.5 -> 1.0
        in 1.5..3.0 -> 2.0
        in 3.0..7.0 -> 5.0
        else -> 10.0
    }

    val first = floor(min / step) * step
    val count = ceil((max - first) / step).roundToInt().coerceAtLeast(1)
    // Built from the index rather than by repeated addition: summing a step like 0.1 a dozen times
    // drifts, and the drift shows up in the labels.
    return AxisTicks(List(count + 1) { first + it * step })
}

/**
 * A logarithmic y axis, where equal distances are equal **ratios**. That is the whole reason to want
 * one: two series with nothing in common but their shape become readable against each other, since a
 * tenth more is the same rise whether it is 200 kcal or 0,6 g Salz.
 *
 * [min] and [max] are the bounds the plot maps onto and [values] the labelled steps *inside* them —
 * unlike [AxisTicks], whose outermost steps are its bounds. The bounds hug the data instead of
 * snapping out to the enclosing decades: a series moving between 2000 and 2500 kcal would otherwise
 * be pressed into a fifth of the panel somewhere between 1000 and 10000.
 *
 * The steps are therefore **not** evenly spaced — 2 sits a third of the way from 1 to 10 — so their
 * labels have to be placed at their own heights rather than distributed down the gutter.
 */
data class LogAxis(val min: Double, val max: Double, val values: List<Double>) {
    /**
     * Where [value] sits between the bounds: 0 at the floor, 1 at the top.
     *
     * Zero and negative values have no place on a log axis at all. They are pinned to the floor
     * rather than dropped, because a hole in the line reads as "nothing logged" while a dive to the
     * bottom edge reads as what it is — and the crosshair still names the real value.
     */
    fun fractionOf(value: Double): Float {
        val span = log10(max) - log10(min)
        if (span <= 0.0) return 0f
        return ((log10(value.coerceAtLeast(min)) - log10(min)) / span).toFloat().coerceIn(0f, 1f)
    }
}

/** A little air above and below the data, multiplicative because that is what "a little" means here. */
private const val LogAxisPadding = 1.05

/** The mantissas a round step is built from, as on any log paper. */
private val LogMantissas = listOf(1.0, 2.0, 5.0)

/**
 * The logarithmic axis covering [values], or null when nothing in them is above zero — a log axis
 * has no answer at all for a series that never left the floor.
 */
fun logAxis(values: List<Double>, targetSteps: Int = 5): LogAxis? {
    val positive = values.filter { it > 0.0 }
    val top = positive.maxOrNull() ?: return null
    val min = positive.min() / LogAxisPadding
    val max = top * LogAxisPadding
    return LogAxis(min = min, max = max, values = logSteps(min, max, targetSteps))
}

/**
 * The labelled steps between the bounds: the round numbers that fall inside them, thinned to whole
 * decades and then to every second or third one as the span grows, so six orders of magnitude still
 * carry a readable handful of labels.
 *
 * Under a decade of span there is no round step to be had — nothing between 2000 and 2500 is a power
 * of ten times 1, 2 or 5 — and the window is labelled the way a linear axis would be. Over so short
 * a span the two scales are nearly the same line anyway.
 */
private fun logSteps(min: Double, max: Double, targetSteps: Int): List<Double> {
    val exponents = floor(log10(min)).toInt()..ceil(log10(max)).toInt()
    val candidates = buildList {
        add(exponents.flatMap { exponent -> LogMantissas.map { it * 10.0.pow(exponent) } })
        (1..4).forEach { stride ->
            add(exponents.filter { (it - exponents.first) % stride == 0 }.map { 10.0.pow(it) })
        }
    }
    val maxSteps = targetSteps + 4
    return candidates
        .map { steps -> steps.filter { it in min..max } }
        .firstOrNull { it.size in 2..maxSteps }
        ?: niceAxisTicks(min, max, targetSteps).values.filter { it > 0.0 && it in min..max }
}
