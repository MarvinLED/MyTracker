package com.example.mytracker.nutrition.food

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * "Everything tagged [childTagId] is also [parentTagId]" — vegan ⇒ vegetarisch. Filtering by the
 * parent therefore also turns up items that only carry the child.
 *
 * A join table rather than a single `parentTagId` column on [Tag], because one tag can imply several
 * others at once (vegan ⇒ vegetarisch *and* ⇒ laktosefrei). The implication is deliberately not
 * written into [FoodItemTag]: a food keeps exactly the tags the user gave it, so re-arranging the
 * hierarchy later re-filters history instead of leaving stale labels behind.
 */
@Entity(
    tableName = "tag_implications",
    primaryKeys = ["childTagId", "parentTagId"],
    foreignKeys = [
        ForeignKey(entity = Tag::class, parentColumns = ["id"], childColumns = ["childTagId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Tag::class, parentColumns = ["id"], childColumns = ["parentTagId"], onDelete = ForeignKey.CASCADE),
    ],
    // childTagId is already covered by the primary key's leading column.
    indices = [Index("parentTagId")],
)
data class TagImplication(
    val childTagId: String,
    val parentTagId: String,
)
