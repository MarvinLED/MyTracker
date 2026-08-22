package com.example.prokject2_tracker.nutrition.food

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Join row attaching a [Tag] to a [FoodItem]. Rezepte get tags indirectly, via their ingredients' foods. */
@Entity(
    tableName = "food_item_tags",
    primaryKeys = ["foodItemId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = FoodItem::class, parentColumns = ["id"], childColumns = ["foodItemId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Tag::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("foodItemId"), Index("tagId")],
)
data class FoodItemTag(
    val foodItemId: String,
    val tagId: String,
)
