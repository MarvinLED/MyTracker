package com.example.mytracker.measurement

import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.formatDecimal
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Body measurements are a tape measure reading; the whole area is in cm. */
const val MEASUREMENT_UNIT = "cm"

/** How many decimals a measurement is shown with — a tape measure reads to the millimetre. */
const val MEASUREMENT_DECIMALS = 1

/** How many past sessions the Einträge list holds before it stops being a list and becomes a log. */
const val MEASUREMENT_HISTORY_LIMIT = 30

private val historyDateFormatter = DateTimeFormatter.ofPattern("EEE, d. MMM yyyy", Locale.GERMAN)

/** One site's value inside a logged session, and how far it moved since that site was last measured. */
data class MeasurementValue(
    val siteId: String,
    val siteName: String,
    val valueCm: Double,
    /** Against the same site's previous session; null for its first ever measurement. */
    val deltaCm: Double? = null,
) {
    val valueText: String get() = "${valueCm.formatDecimal(MEASUREMENT_DECIMALS)} $MEASUREMENT_UNIT"

    val deltaText: String? get() = deltaCm?.takeIf { it != 0.0 }?.signedCm()
}

/**
 * A change with its sign spelled out — "+1,5 cm", "−0,5 cm" — and a real minus sign rather than a
 * hyphen, since these sit next to plus signs and have to look like their opposite.
 *
 * Deliberately not coloured anywhere it is shown. Whether a centimetre more is good depends entirely
 * on the spot: on an upper arm it is the goal, on a waist it is the opposite, and nothing in a
 * [BodySite] says which kind it is. Green on both would be wrong half the time.
 */
fun Double.signedCm(): String {
    val magnitude = kotlin.math.abs(this).formatDecimal(MEASUREMENT_DECIMALS)
    return if (this > 0) "+$magnitude $MEASUREMENT_UNIT" else "−$magnitude $MEASUREMENT_UNIT"
}

/**
 * One measuring session: everything logged on one day. The day is the unit of editing, not the
 * single value — a session is measured in one go with one tape, and correcting it means opening
 * that day again, not hunting down each site's row.
 */
data class MeasurementDayRow(
    val epochDay: Long,
    val dateText: String,
    val values: List<MeasurementValue>,
) {
    val summary: String get() = values.joinToString(" · ") { "${it.siteName} ${it.valueText}" }
}

/**
 * The logged sessions, newest first. Sites are ordered as the library orders them, so the same two
 * measurements read in the same order in every row.
 */
fun measurementDayRows(
    measurements: List<BodyMeasurement>,
    sites: List<BodySite>,
    limit: Int = MEASUREMENT_HISTORY_LIMIT,
): List<MeasurementDayRow> {
    val siteOrder = sites.withIndex().associate { (index, site) -> site.id to index }
    val siteNames = sites.associate { it.id to it.name }
    // Each measurement's predecessor within its own site — the change is per spot, so the row above
    // it in the same session says nothing about it.
    val previousValue = mutableMapOf<String, Double>()
    val deltas = mutableMapOf<String, Double>()
    measurements.sortedBy { it.epochDay }.forEach { entry ->
        previousValue[entry.bodySiteId]?.let { deltas[entry.id] = entry.valueCm - it }
        previousValue[entry.bodySiteId] = entry.valueCm
    }
    return measurements
        .groupBy { it.epochDay }
        .entries
        .sortedByDescending { it.key }
        .take(limit)
        .map { (day, entries) ->
            MeasurementDayRow(
                epochDay = day,
                dateText = DateUtils.localDateOfEpochDay(day).format(historyDateFormatter),
                values = entries
                    // A measurement whose site is gone can't be named, and cascading delete means it
                    // should not exist — but ordering must not depend on that being true.
                    .filter { it.bodySiteId in siteNames }
                    .sortedBy { siteOrder[it.bodySiteId] ?: Int.MAX_VALUE }
                    .map {
                        MeasurementValue(
                            siteId = it.bodySiteId,
                            siteName = siteNames.getValue(it.bodySiteId),
                            valueCm = it.valueCm,
                            deltaCm = deltas[it.id],
                        )
                    },
            )
        }
        .filter { it.values.isNotEmpty() }
}

/**
 * The editor's rows for [editingEpochDay], with each field's starting text.
 *
 * The prefill rule turns on whether that day was already measured:
 *
 * - **A day that has entries** is being *corrected*. Each field opens on what that day actually
 *   holds, and a site not measured that day opens **empty** — filling it from some other day's
 *   value and saving would invent a measurement that was never taken.
 * - **A fresh day** is being *logged*. Every field opens on that site's last value *before* that
 *   day, so an unchanged spot is confirmed rather than typed, which is the point of the panel.
 *
 * "Before that day", not "most recent overall": with a freely chosen date the editor can be pointed
 * at a gap between two sessions, and prefilling a June entry from an August one would carry a value
 * backwards in time.
 *
 * [drafts] — what the user has actually typed — wins over both, so a field never resets under the
 * cursor when the underlying data changes.
 */
fun measurementRows(
    sites: List<BodySite>,
    measurements: List<BodyMeasurement>,
    editingEpochDay: Long,
    drafts: Map<String, String>,
): List<MeasurementRow> {
    val bySite = measurements.groupBy { it.bodySiteId }
    val onEditingDay = measurements.filter { it.epochDay == editingEpochDay }.associateBy { it.bodySiteId }
    val isExistingDay = onEditingDay.isNotEmpty()

    return sites.map { site ->
        val reference = bySite[site.id]
            ?.filter { it.epochDay < editingEpochDay }
            ?.maxByOrNull { it.epochDay }
        val onDay = onEditingDay[site.id]
        val prefill = if (isExistingDay) onDay?.valueCm else reference?.valueCm
        MeasurementRow(
            site = site,
            draft = drafts[site.id] ?: prefill?.formatDecimal(MEASUREMENT_DECIMALS).orEmpty(),
            referenceValueCm = reference?.valueCm,
            referenceEpochDay = reference?.epochDay,
            savedValueCm = onDay?.valueCm,
        )
    }
}

/**
 * Two sites read against each other over time, as a percentage — Taille zu Hüfte being the one
 * everybody means, but the sites are the user's own, so which two is theirs to say.
 *
 * A percentage rather than a bare quotient because the chart's axis rounds to one decimal: 0,82 and
 * 0,85 would both land on "0,8", which is precisely the difference the number exists to show. As
 * 82 % and 85 % they are two readable steps apart.
 *
 * Only days where **both** sites were measured produce a point. Carrying the last known hip forward
 * onto a day only the waist was measured would draw a ratio that was never true.
 */
fun ratioPoints(
    measurements: List<BodyMeasurement>,
    numeratorSiteId: String,
    denominatorSiteId: String,
): List<MetricPoint> {
    val numerators = measurements.filter { it.bodySiteId == numeratorSiteId }.associateBy { it.epochDay }
    val denominators = measurements.filter { it.bodySiteId == denominatorSiteId }.associateBy { it.epochDay }
    return numerators.keys
        .intersect(denominators.keys)
        .sorted()
        .mapNotNull { day ->
            val denominator = denominators.getValue(day).valueCm
            if (denominator <= 0.0) return@mapNotNull null
            MetricPoint(day, numerators.getValue(day).valueCm / denominator * 100.0)
        }
}
