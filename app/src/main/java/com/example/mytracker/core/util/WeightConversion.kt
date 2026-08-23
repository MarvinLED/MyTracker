package com.example.mytracker.core.util

import com.example.mytracker.core.datastore.WeightUnit

private const val KG_PER_LB = 0.45359237

/** Converts a kilogram value to pounds. */
fun Double.kgToLb(): Double = this / KG_PER_LB

/** Converts a pound value to kilograms. */
fun Double.lbToKg(): Double = this * KG_PER_LB

/** A stored kilogram value in whichever unit the user reads weights in. */
fun Double.toWeightUnit(unit: WeightUnit): Double = when (unit) {
    WeightUnit.KG -> this
    WeightUnit.LB -> kgToLb()
}

/** What that unit is called on a label or an axis. */
fun WeightUnit.label(): String = when (this) {
    WeightUnit.KG -> "kg"
    WeightUnit.LB -> "lb"
}
