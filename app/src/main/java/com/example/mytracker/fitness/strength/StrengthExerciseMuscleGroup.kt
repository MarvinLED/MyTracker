package com.example.mytracker.fitness.strength

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Join row attaching a [MuscleGroup] to a [StrengthExercise] — an exercise can target several. */
@Entity(
    tableName = "strength_exercise_muscle_groups",
    primaryKeys = ["exerciseId", "muscleGroupId"],
    foreignKeys = [
        ForeignKey(entity = StrengthExercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MuscleGroup::class, parentColumns = ["id"], childColumns = ["muscleGroupId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("exerciseId"), Index("muscleGroupId")],
)
data class StrengthExerciseMuscleGroup(
    val exerciseId: String,
    val muscleGroupId: String,
)
