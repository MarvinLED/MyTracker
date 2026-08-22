package com.example.mytracker.core.util

private const val KG_PER_LB = 0.45359237

/** Converts a kilogram value to pounds. */
fun Double.kgToLb(): Double = this / KG_PER_LB

/** Converts a pound value to kilograms. */
fun Double.lbToKg(): Double = this * KG_PER_LB
