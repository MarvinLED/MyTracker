package com.example.prokject2_tracker.core.metrics

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
