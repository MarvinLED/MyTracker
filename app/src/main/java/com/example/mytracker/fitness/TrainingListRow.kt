package com.example.prokject2_tracker.fitness

import com.example.prokject2_tracker.fitness.cardio.CardioSession
import com.example.prokject2_tracker.fitness.strength.StrengthLogEntry
import com.example.prokject2_tracker.fitness.strength.StrengthSet
import java.time.Instant

sealed interface TrainingListRow {
    val epochDay: Long
    val createdAt: Instant

    data class Cardio(val session: CardioSession) : TrainingListRow {
        override val epochDay: Long = session.epochDay
        override val createdAt: Instant = session.createdAt
    }

    data class Strength(val entry: StrengthLogEntry, val sets: List<StrengthSet>) : TrainingListRow {
        override val epochDay: Long = entry.epochDay
        override val createdAt: Instant = entry.createdAt
    }
}
