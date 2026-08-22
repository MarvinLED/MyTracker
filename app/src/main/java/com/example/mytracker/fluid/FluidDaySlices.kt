package com.example.mytracker.fluid

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * One slice per drink type consumed on a day, ordered by the library's own order so a type keeps its
 * colour across days. Types past the palette's eight slots (or logged under a type that has since
 * been deleted from the library) fold into one "Sonstige" slice rather than repeating a hue.
 *
 * Shared by the Flüssigkeiten ring and the Tagebuch's fluid bar: a second copy of this mapping would
 * sooner or later hand the same drink two different colours on two screens.
 */
@Composable
fun fluidDistributionSlices(entries: List<FluidEntry>, types: List<FluidType>): List<FluidSlice> {
    val palette = fluidPalette()
    val otherColor = MaterialTheme.colorScheme.onSurfaceVariant
    val totalsByType = entries.groupBy { it.fluidTypeId }
        .mapValues { (_, typeEntries) -> typeEntries.sumOf { it.amountMl } }

    val named = mutableListOf<Pair<Int, FluidSlice>>()
    var otherTotal = 0.0
    totalsByType.forEach { (typeId, total) ->
        val index = types.indexOfFirst { it.id == typeId }
        val type = types.getOrNull(index)
        if (type == null || index >= palette.size) {
            otherTotal += total
        } else {
            named += index to FluidSlice(label = type.name, value = total, color = type.chartColor(index))
        }
    }
    val ordered = named.sortedBy { (index, _) -> index }.map { (_, slice) -> slice }
    return if (otherTotal > 0.0) ordered + FluidSlice("Sonstige", otherTotal, otherColor) else ordered
}
