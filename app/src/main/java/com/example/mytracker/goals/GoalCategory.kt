package com.example.mytracker.goals

/**
 * The sections of the Ziele screen, in the order they are shown. Doubles as the filter's options,
 * so the headings and the filter entries can never drift apart.
 */
enum class GoalCategory(val label: String) {
    NUTRITION("Ernährung"),
    SLEEP("Schlaf"),
    FLUID("Flüssigkeiten"),
    FITNESS("Fitness"),
}
