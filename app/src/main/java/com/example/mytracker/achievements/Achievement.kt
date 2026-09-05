package com.example.mytracker.achievements

/**
 * One earned thing on the wall.
 *
 * Everything here is **derived from the logged data** rather than stored: nothing is awarded, only
 * noticed. That is what lets the wall be full on the day the screen first opens, and what makes a
 * restored backup bring every mark back with it.
 */
data class Achievement(
    val id: String,
    /** What it is about — an exercise, a habit, a goal. */
    val title: String,
    /** The mark itself, ready to read: "102,5 kg", "14 Tage". */
    val value: String,
    /** When it was set, and what it displaced. Null when neither applies. */
    val detail: String? = null,
    /** Full for a record, and how far to the next rung for a milestone. */
    val fraction: Float? = null,
    /** "noch 12 bis 100 Tage" — only ever set for a milestone that still has a rung above it. */
    val nextLabel: String? = null,
    /** True when this mark was not on the wall the last time it was open. */
    val isNew: Boolean = false,
) {
    /**
     * What "already seen" is remembered by. The value is part of it on purpose: a beaten record
     * keeps its id, so keying on the id alone would let a new personal best slip past unannounced.
     */
    val seenKey: String get() = "$id:$value"
}

data class AchievementSection(val title: String, val items: List<Achievement>)

data class AchievementsUiState(
    /** The figure's five attributes, each with the best it has ever been. */
    val attributes: List<AttributeLevel> = emptyList(),
    /** What the last settled day earned, all attributes together. */
    val lastBookedPoints: Double = 0.0,
    /** Everything the figure has earned the right to wear. Permanent — see [AvatarItem]. */
    val unlockedItems: Set<AvatarItem> = emptySet(),
    /** The pieces earned since the wall was last open, so they can be pointed at. */
    val newItems: Set<AvatarItem> = emptySet(),
    /** The closest locked piece, so there is always a visible next thing. */
    val nextUnlock: AvatarItemProgress? = null,
    val sections: List<AchievementSection> = emptyList(),
    /** False until the first collection has arrived, so an empty wall is not claimed too early. */
    val loaded: Boolean = false,
) {
    /** Nothing earned and nothing recorded — a genuinely blank slate, not just a quiet month. */
    val isEmpty: Boolean get() = sections.isEmpty() && attributes.all { it.record == 0 }
}
