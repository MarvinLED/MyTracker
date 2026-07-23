package com.example.prokject2_tracker.fitness.cardio

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Rowing
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.ui.graphics.vector.ImageVector

fun CardioActivityType.label(): String = when (this) {
    CardioActivityType.RUNNING -> "Laufen"
    CardioActivityType.CYCLING -> "Radfahren"
    CardioActivityType.SWIMMING -> "Schwimmen"
    CardioActivityType.WALKING -> "Gehen"
    CardioActivityType.HIKING -> "Wandern"
    CardioActivityType.ROWING -> "Rudern"
    CardioActivityType.OTHER -> "Sonstiges"
}

fun CardioActivityType.icon(): ImageVector = when (this) {
    CardioActivityType.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
    CardioActivityType.CYCLING -> Icons.AutoMirrored.Filled.DirectionsBike
    CardioActivityType.SWIMMING -> Icons.Filled.Pool
    CardioActivityType.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
    CardioActivityType.HIKING -> Icons.Filled.Hiking
    CardioActivityType.ROWING -> Icons.Filled.Rowing
    CardioActivityType.OTHER -> Icons.Filled.SelfImprovement
}
