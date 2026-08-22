package com.example.mytracker.fluid

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * The symbol a quick-add button carries. Deliberately a closed set rather than free text: the point
 * of the buttons is that a glance tells them apart, and three shapes stay tellable apart where an
 * arbitrary icon library would not.
 *
 * [ML_100] is the odd one out — it names its own amount, so it always logs exactly 100 ml while the
 * other two carry whatever amount the user gave that button.
 */
enum class FluidQuickAddSymbol { GLASS, BOTTLE, ML_100 }

fun FluidQuickAddSymbol.label(): String = when (this) {
    FluidQuickAddSymbol.GLASS -> "Glas"
    FluidQuickAddSymbol.BOTTLE -> "Flasche"
    FluidQuickAddSymbol.ML_100 -> "100 ml"
}

/** What a new button of this symbol starts at; only a suggestion for [FluidQuickAddSymbol.GLASS]
 * and [FluidQuickAddSymbol.BOTTLE], but binding for [FluidQuickAddSymbol.ML_100]. */
fun FluidQuickAddSymbol.defaultAmountMl(): Double = when (this) {
    FluidQuickAddSymbol.GLASS -> 250.0
    FluidQuickAddSymbol.BOTTLE -> 500.0
    FluidQuickAddSymbol.ML_100 -> FluidQuickAdd100Ml
}

/** The one amount a "100" button may ever log — the symbol would otherwise lie about what it does. */
const val FluidQuickAdd100Ml: Double = 100.0

/** How many buttons fit next to each other before the row wraps. */
const val FluidQuickAddsPerRow: Int = 4

/** At most two rows, as the Tagebuch's fluid area is a shortcut, not a second Flüssigkeiten screen. */
const val FluidQuickAddLimit: Int = FluidQuickAddsPerRow * 2

/**
 * One configured shortcut in the Tagebuch's fluid area: tapping it logs [amountMl] of its drink type
 * for the shown day, with no dialog in between.
 *
 * The button takes its colour from the type it points at (the same colour that type has in the
 * charts), so the row of buttons and the bar above it name the same drinks — which is why the type
 * is referenced by id and cascades: a shortcut to a deleted drink type has nothing left to log or to
 * colour itself with.
 */
@Entity(
    tableName = "fluid_quick_adds",
    indices = [Index("fluidTypeId")],
    foreignKeys = [
        ForeignKey(
            entity = FluidType::class,
            parentColumns = ["id"],
            childColumns = ["fluidTypeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FluidQuickAdd(
    @PrimaryKey val id: String,
    val fluidTypeId: String,
    val symbol: FluidQuickAddSymbol,
    val amountMl: Double,
    val sortOrder: Int,
    val createdAt: Instant,
)

/**
 * The buttons split into the rows they are drawn in, capped at [FluidQuickAddLimit]. Split out as a
 * pure function so the cap and the wrap are testable without a screen.
 */
fun <T> fluidQuickAddRows(items: List<T>): List<List<T>> =
    items.take(FluidQuickAddLimit).chunked(FluidQuickAddsPerRow)
