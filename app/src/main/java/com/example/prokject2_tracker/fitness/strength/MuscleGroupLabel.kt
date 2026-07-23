package com.example.prokject2_tracker.fitness.strength

fun MuscleGroup.label(): String = when (this) {
    MuscleGroup.CHEST -> "Brust"
    MuscleGroup.BACK -> "Rücken"
    MuscleGroup.LEGS -> "Beine"
    MuscleGroup.SHOULDERS -> "Schultern"
    MuscleGroup.ARMS -> "Arme"
    MuscleGroup.CORE -> "Rumpf"
    MuscleGroup.FULL_BODY -> "Ganzkörper"
    MuscleGroup.OTHER -> "Sonstiges"
}
