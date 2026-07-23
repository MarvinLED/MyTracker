package com.example.prokject2_tracker.fluid

fun FluidType.label(): String = when (this) {
    FluidType.WATER -> "Wasser"
    FluidType.COFFEE -> "Kaffee"
    FluidType.TEA -> "Tee"
    FluidType.JUICE -> "Saft"
    FluidType.SODA -> "Limonade"
    FluidType.OTHER -> "Sonstiges"
}

/** One-tap quick-add amount per type, tuned to a typical serving/glass/cup. */
fun FluidType.defaultQuickAddMl(): Double = when (this) {
    FluidType.WATER -> 250.0
    FluidType.COFFEE -> 125.0
    FluidType.TEA -> 200.0
    FluidType.JUICE -> 200.0
    FluidType.SODA -> 330.0
    FluidType.OTHER -> 200.0
}
