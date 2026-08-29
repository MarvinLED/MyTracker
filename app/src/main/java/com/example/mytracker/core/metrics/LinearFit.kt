package com.example.mytracker.core.metrics

/**
 * The straight line through a cloud of points, least squares, plus how much of the scatter it
 * actually explains.
 *
 * [rSquared] is the part that keeps the line honest: a fit is drawn through any cloud whatsoever,
 * and without a number for how well it holds, four points of noise look exactly like a law of
 * nature. Between 0 (the line says nothing the mean does not) and 1 (every point sits on it).
 */
data class LinearFit(val slope: Double, val intercept: Double, val rSquared: Double) {
    fun yAt(x: Double): Double = intercept + slope * x

    /**
     * Where the line crosses y = 0, or null for a line too flat to cross anywhere meaningful.
     *
     * For the weekly energy chart this is the interesting point of the whole fit: the intake at
     * which the weight neither rose nor fell.
     */
    fun xAtZero(): Double? = if (slope == 0.0) null else -intercept / slope
}

/** Below this many points a "trend" is a line through noise, so none is offered. */
private const val MinFitPoints = 3

/**
 * The least-squares fit through [points] as (x, y) pairs, or null when there is nothing to fit:
 * too few points, or no spread in x at all — a vertical cloud has every slope and no slope.
 */
fun linearFit(points: List<Pair<Double, Double>>): LinearFit? {
    if (points.size < MinFitPoints) return null
    val n = points.size
    val meanX = points.sumOf { it.first } / n
    val meanY = points.sumOf { it.second } / n
    val varianceX = points.sumOf { (it.first - meanX) * (it.first - meanX) }
    if (varianceX <= 0.0) return null

    val covariance = points.sumOf { (it.first - meanX) * (it.second - meanY) }
    val slope = covariance / varianceX
    val intercept = meanY - slope * meanX

    val totalVarianceY = points.sumOf { (it.second - meanY) * (it.second - meanY) }
    // A y that never moves is explained perfectly by a flat line — and the ratio below would divide
    // by zero saying so.
    val rSquared = if (totalVarianceY <= 0.0) {
        1.0
    } else {
        val residuals = points.sumOf { (x, y) -> (y - (intercept + slope * x)).let { it * it } }
        (1.0 - residuals / totalVarianceY).coerceIn(0.0, 1.0)
    }
    return LinearFit(slope = slope, intercept = intercept, rSquared = rSquared)
}
