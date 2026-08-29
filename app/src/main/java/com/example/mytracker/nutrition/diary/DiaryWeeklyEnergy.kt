package com.example.mytracker.nutrition.diary

import com.example.mytracker.core.metrics.EpochDayRange
import com.example.mytracker.core.metrics.LinearFit
import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.core.metrics.linearFit
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.core.util.formatSigned
import com.example.mytracker.weight.BodyWeightEntry
import kotlin.math.roundToInt

/**
 * One week in the Kalorien-Gewicht comparison: what was eaten per day, and what the weight did over
 * that week.
 *
 * A whole calendar week rather than a day, because a day compares nothing: a single weigh-in carries
 * a kilo of water and yesterday's salt, and one day's intake has not had time to show up anywhere.
 * Over a week both of those average out far enough to be read against each other.
 */
data class WeeklyEnergyPoint(
    val weekStart: Long,
    val kcalPerDay: Double,
    /** This week's mean weight minus the week before's. Positive is a gain. */
    val weightChangeKg: Double,
    /** How many days of the week had anything logged — what [kcalPerDay] is the mean of. */
    val loggedDays: Int,
)

/**
 * A week counts only with at least this many logged days. Four is the majority of a week: below it
 * the mean is the mean of a long weekend, and it would be plotted as if it were the whole week's
 * eating.
 */
const val MIN_LOGGED_DAYS_PER_WEEK = 4

/** Under this many weeks nothing is claimed at all — a handful of dots has no shape to read. */
const val MIN_WEEKS_FOR_COMPARISON = 4

/** From this many weeks on, the fit is steady enough to name a maintenance figure from. */
private const val MIN_WEEKS_FOR_MAINTENANCE = 6

/**
 * How far outside the weeks actually eaten the maintenance estimate may fall, as a share of their
 * spread. A crossing well beyond the cloud is arithmetic, not a finding: nothing in the data says
 * what happens at 3000 kcal if nothing near 3000 was ever eaten.
 */
private const val MAINTENANCE_EXTRAPOLATION_LIMIT = 0.1

/**
 * The weeks that can be compared, oldest first.
 *
 * Only calendar weeks lying **entirely** inside [range] are used: a part week's average is not
 * comparable with a full one's, and it would sit in the cloud looking exactly as trustworthy. The
 * first complete week has no predecessor to be measured against and therefore never yields a point.
 *
 * The weight change is mean against mean rather than last-day against last-day. Two single readings
 * differ by whatever water was in them; two weekly means differ by roughly what the body did.
 */
fun weeklyEnergyPoints(
    range: EpochDayRange,
    nutritionTotals: List<DailyNutritionTotals>,
    weights: List<BodyWeightEntry>,
    minLoggedDays: Int = MIN_LOGGED_DAYS_PER_WEEK,
): List<WeeklyEnergyPoint> {
    if (range.endInclusive < range.startInclusive) return emptyList()

    val weeks = completeWeeks(range)
    val complete = weeks.toSet()
    // Both sides read only complete weeks — including the week a change is measured *against*. A
    // mean over the three days a window happens to start with is not a week's weight.
    val kcalByWeek = nutritionTotals
        .groupBy { DateUtils.startOfWeekEpochDay(it.epochDay) }
        .filterKeys { it in complete }
    val weightByWeek = weights
        .groupBy { DateUtils.startOfWeekEpochDay(it.epochDay) }
        .filterKeys { it in complete }
        .mapValues { (_, entries) -> entries.sumOf { it.weightKg } / entries.size }

    return weeks.mapNotNull { weekStart ->
        val days = kcalByWeek[weekStart].orEmpty()
        if (days.size < minLoggedDays) return@mapNotNull null
        val thisWeek = weightByWeek[weekStart] ?: return@mapNotNull null
        val lastWeek = weightByWeek[weekStart - 7] ?: return@mapNotNull null
        WeeklyEnergyPoint(
            weekStart = weekStart,
            kcalPerDay = days.sumOf { it.totals.kcal } / days.size,
            weightChangeKg = thisWeek - lastWeek,
            loggedDays = days.size,
        )
    }
}

/** Every Monday whose whole week fits inside the window, oldest first. */
private fun completeWeeks(range: EpochDayRange): List<Long> {
    val firstMonday = DateUtils.startOfWeekEpochDay(range.startInclusive)
        .let { if (it < range.startInclusive) it + 7 else it }
    return generateSequence(firstMonday) { it + 7 }
        .takeWhile { it + 6 <= range.endInclusive }
        .toList()
}

/**
 * The whole reading of the Kalorien-Gewicht comparison: the weeks, the line through them, and the
 * two calorie figures worth naming beside it.
 *
 * [maintenanceKcal] is where the fitted line crosses "weight held" — an estimate of what this body
 * actually maintains on, which is the number a target is worth checking against. [goalKcalPerDay] is
 * what the Tagebuch was aiming at over the same window, so the two can be read side by side.
 */
data class WeeklyEnergySummary(
    val points: List<WeeklyEnergyPoint>,
    val fit: LinearFit?,
    val maintenanceKcal: Double?,
    val goalKcalPerDay: Double?,
) {
    val hasEnoughWeeks: Boolean get() = points.size >= MIN_WEEKS_FOR_COMPARISON
}

/**
 * Folds the weeks into that reading. [goalTimeline] is the Kalorien-Soll per day — the same series
 * the Verlauf draws as its Soll line — averaged over the window.
 */
fun weeklyEnergySummary(
    points: List<WeeklyEnergyPoint>,
    goalTimeline: List<MetricPoint>,
): WeeklyEnergySummary {
    val fit = linearFit(points.map { it.kcalPerDay to it.weightChangeKg })
    return WeeklyEnergySummary(
        points = points,
        fit = fit,
        maintenanceKcal = fit?.let { maintenanceKcal(points, it) },
        goalKcalPerDay = goalTimeline.takeIf { it.isNotEmpty() }?.let { it.sumOf { p -> p.value } / it.size },
    )
}

/**
 * The intake at which the line says the weight holds — or null when that crossing is not something
 * the data supports: too few weeks, a line running the wrong way (more food, less weight), or a
 * crossing outside the range that was actually eaten.
 */
private fun maintenanceKcal(points: List<WeeklyEnergyPoint>, fit: LinearFit): Double? {
    if (points.size < MIN_WEEKS_FOR_MAINTENANCE || fit.slope <= 0.0) return null
    val crossing = fit.xAtZero() ?: return null
    val eaten = points.map { it.kcalPerDay }
    val low = eaten.min()
    val high = eaten.max()
    val slack = (high - low) * MAINTENANCE_EXTRAPOLATION_LIMIT
    return crossing.takeIf { it in (low - slack)..(high + slack) }
}

/** The step the slope is quoted in — a portion size people recognise, not one kilocalorie. */
private const val KCAL_STEP = 500

/** How much of the scatter the line explains, in words. A bare r² tells most readers nothing. */
private fun strengthLabel(rSquared: Double): String = when {
    rSquared < 0.2 -> "schwach"
    rSquared < 0.5 -> "mittel"
    else -> "stark"
}

/** "18 Wochen · Zusammenhang mittel (r² = 0,4)" — how much there is, and how much it is worth. */
fun WeeklyEnergySummary.relationshipText(): String {
    val weeks = "${points.size} ${if (points.size == 1) "Woche" else "Wochen"}"
    val line = fit ?: return weeks
    return "$weeks · Zusammenhang ${strengthLabel(line.rSquared)} (r² = ${line.rSquared.formatCompact()})"
}

/**
 * The slope in the units it is felt in: what 500 kcal a day more did to a week. Kilograms per
 * kilocalorie is the same number and unreadable.
 */
fun WeeklyEnergySummary.slopeText(): String? {
    val line = fit ?: return null
    return "Je $KCAL_STEP kcal/Tag mehr: ${(line.slope * KCAL_STEP).formatSigned()} kg pro Woche"
}

/**
 * The two calorie figures side by side — what holds the weight, and what was being aimed at. Either
 * may be missing: without enough weeks there is no estimate, and without a Kalorienziel no target.
 */
fun WeeklyEnergySummary.maintenanceText(): String? = listOfNotNull(
    // Rounded to ten: an estimate from a handful of weeks does not know its own last digit.
    maintenanceKcal?.let { "Gewicht gehalten bei ~${(it / 10).roundToInt() * 10} kcal/Tag" },
    goalKcalPerDay?.let { "dein Ziel: ${it.roundToInt()} kcal/Tag" },
).joinToString(" · ").ifEmpty { null }
