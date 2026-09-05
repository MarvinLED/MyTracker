package com.example.mytracker.nutrition.library

import com.example.mytracker.nutrition.food.Tag

/**
 * One tap further along the tag button's cycle: "Alle" (null), then every tag in library order, then
 * back to "Alle".
 *
 * A tag that has meanwhile been deleted, and anything else the cycle does not know, starts the cycle
 * from the beginning rather than getting stuck. With no tags at all there is nothing to cycle to and
 * the button stays on "Alle".
 */
fun nextTagId(current: String?, tags: List<Tag>): String? {
    if (tags.isEmpty()) return null
    val cycle: List<String?> = listOf(null) + tags.map { it.id }
    val index = cycle.indexOf(current)
    if (index == -1) return cycle.first()
    return cycle[(index + 1) % cycle.size]
}
