package com.example.prokject2_tracker.nutrition.food

import com.example.prokject2_tracker.core.util.AppLocale

/**
 * A food's price is entered for whatever amount is handiest — "0,89 € pro 100 g" for loose goods,
 * "2,49 € pro Packung" for anything sold as a piece — and stored exactly that way (see
 * [FoodItem.price] / [FoodItem.priceUnitName]). Comparing two foods needs a common base, so
 * [pricePer100] converts on demand rather than storing a second number that would go stale the
 * moment a unit's gram amount is corrected.
 */

/** "2,49 €" — always two decimals, since that is how a price reads. */
fun formatEuro(value: Double): String = String.format(AppLocale, "%.2f €", value)

/** What a price refers to: "100 g"/"100 ml" for the base unit, else the named unit ("Packung"). */
fun priceBasisLabel(priceUnitName: String?, baseUnit: BaseUnit): String =
    priceUnitName ?: "100 ${baseUnit.label()}"

/**
 * What 100 base units cost, given a [price] for [basisBaseUnits] g/ml — null basis meaning the
 * price is already per 100. Null when no price is recorded, or when the basis amount is missing or
 * zero, which would make the conversion a guess rather than a number.
 */
fun pricePer100(price: Double?, basisBaseUnits: Double?): Double? {
    val value = price ?: return null
    if (basisBaseUnits == null) return value
    if (basisBaseUnits <= 0.0) return null
    return value * 100.0 / basisBaseUnits
}

/** "2,49 € / Packung" as shown in the Lebensmittel list, or null when the food has no price. */
fun FoodItem.formatPrice(): String? =
    price?.let { "${formatEuro(it)} / ${priceBasisLabel(priceUnitName, baseUnit)}" }
