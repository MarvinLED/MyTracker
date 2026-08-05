package com.example.prokject2_tracker.nutrition.food

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Defines a parent-child relationship between tags. For example, Vegan is a child of Vegetarisch. */
@Entity(
    tableName = "tag_hierarchy",
    primaryKeys = ["parentTagId", "childTagId"],
    foreignKeys = [
        ForeignKey(entity = Tag::class, parentColumns = ["id"], childColumns = ["parentTagId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Tag::class, parentColumns = ["id"], childColumns = ["childTagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("parentTagId"), Index("childTagId")],
)
data class TagHierarchy(
    val parentTagId: String,
    val childTagId: String,
)
