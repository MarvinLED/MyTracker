package com.example.mytracker.achievements

/** Where on the figure a piece of equipment sits. One item per slot is drawn — the highest earned. */
enum class AvatarSlot { KOPF, HALS, TAILLE, ARM, RUECKEN, FUESSE }

/**
 * Something the figure gets to wear.
 *
 * Tied to the **record** level rather than the current one, so equipment is permanent: form comes
 * and goes with the last thirty days, but a thing that was earned is not taken away again. Losing a
 * cape because of a quiet fortnight would punish exactly the moment that needs no more punishing.
 *
 * [attribute] null means the requirement is read against the sum of all five records — the pieces
 * that are about the whole figure rather than one part of it.
 */
enum class AvatarItem(
    val label: String,
    val slot: AvatarSlot,
    val attribute: AvatarAttribute?,
    val requiredLevel: Int,
) {
    STIRNBAND("Stirnband", AvatarSlot.KOPF, AvatarAttribute.AUSDAUER, 2),
    ARMBAND("Armband", AvatarSlot.ARM, AvatarAttribute.KRAFT, 3),
    KETTE("Kette", AvatarSlot.HALS, AvatarAttribute.VITALITAET, 3),
    GUERTEL("Gürtel", AvatarSlot.TAILLE, AvatarAttribute.FORM, 3),
    LAUFSCHUHE("Laufschuhe", AvatarSlot.FUESSE, AvatarAttribute.AUSDAUER, 5),
    MUETZE("Mütze", AvatarSlot.KOPF, AvatarAttribute.KLARHEIT, 5),
    UMHANG("Umhang", AvatarSlot.RUECKEN, null, 12),
    KRONE("Krone", AvatarSlot.KOPF, null, 20),
    ;

    /** What still has to happen, in words — the sentence the locked preview shows. */
    fun requirementText(): String = attribute
        ?.let { "${it.label} Stufe $requiredLevel" }
        ?: "Stufe $requiredLevel über alle Attribute"
}

/** The record level an item is measured against: one attribute's, or the sum of all of them. */
private fun AvatarItem.standingIn(levels: List<AttributeLevel>): Int = attribute
    ?.let { wanted -> levels.firstOrNull { it.attribute == wanted }?.record ?: 0 }
    ?: levels.sumOf { it.record }

/** Everything earned so far. Never shrinks, because it is read off the records. */
fun unlockedItems(levels: List<AttributeLevel>): Set<AvatarItem> =
    AvatarItem.entries.filterTo(mutableSetOf()) { it.standingIn(levels) >= it.requiredLevel }

/**
 * The piece that is closest to being earned, with how far there still is to go.
 *
 * There is always meant to be one of these on screen while anything is still locked: a visible next
 * thing is what keeps a collection from feeling finished, and "noch 2 Stufen" is a far better
 * prompt than a grid of grey silhouettes.
 */
fun nextUnlock(levels: List<AttributeLevel>): AvatarItemProgress? = AvatarItem.entries
    .mapNotNull { item ->
        val standing = item.standingIn(levels)
        if (standing >= item.requiredLevel) {
            null
        } else {
            AvatarItemProgress(item = item, standing = standing, required = item.requiredLevel)
        }
    }
    .minByOrNull { it.remaining }

data class AvatarItemProgress(val item: AvatarItem, val standing: Int, val required: Int) {
    val remaining: Int get() = required - standing
    val fraction: Float get() = (standing.toFloat() / required).coerceIn(0f, 1f)
}
