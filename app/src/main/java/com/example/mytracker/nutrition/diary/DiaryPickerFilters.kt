package com.example.mytracker.nutrition.diary

enum class DiaryPickerMode {
    ALL, FOOD, RECIPE, QUICK,
}

fun DiaryPickerMode.label(): String = when (this) {
    DiaryPickerMode.ALL -> "Alle"
    DiaryPickerMode.FOOD -> "Lebensmittel"
    DiaryPickerMode.RECIPE -> "Rezept"
    DiaryPickerMode.QUICK -> "Schnell hinzufügen"
}

/**
 * The states the combined list button cycles through, in tap order. [DiaryPickerMode.QUICK] is
 * deliberately not one of them: Schnelleintrag lists nothing and keeps its own button.
 */
val diaryPickerListModes = listOf(DiaryPickerMode.ALL, DiaryPickerMode.FOOD, DiaryPickerMode.RECIPE)

/** One tap further along the cycle. Anything outside it starts the cycle from the beginning. */
fun DiaryPickerMode.nextListMode(): DiaryPickerMode {
    val index = diaryPickerListModes.indexOf(this)
    if (index == -1) return diaryPickerListModes.first()
    return diaryPickerListModes[(index + 1) % diaryPickerListModes.size]
}

/** The order the picker list is in. Declaration order is chip order on the screen. */
enum class DiaryPickerSort {
    LAST_EATEN, MOST_EATEN, NAME,
}

fun DiaryPickerSort.label(): String = when (this) {
    // Short labels: three chips share one row with the tag filter, and "Zuletzt gegessen" ate the
    // width the third chip now needs.
    DiaryPickerSort.LAST_EATEN -> "Zuletzt"
    DiaryPickerSort.MOST_EATEN -> "Am meisten"
    DiaryPickerSort.NAME -> "Name"
}
