package com.example.mytracker.nutrition.diary

/**
 * Which kind of item a list is asking for. The Bibliothek's tabs pick this — one mode per tab — so
 * there is no cycling between them any more.
 */
enum class DiaryPickerMode {
    ALL, FOOD, RECIPE,
}

fun DiaryPickerMode.label(): String = when (this) {
    DiaryPickerMode.ALL -> "Alle"
    DiaryPickerMode.FOOD -> "Lebensmittel"
    DiaryPickerMode.RECIPE -> "Rezept"
}

/** The order a list is in. Declaration order is the order the sort button cycles through. */
enum class DiaryPickerSort {
    LAST_EATEN, MOST_EATEN, NAME,
}

fun DiaryPickerSort.label(): String = when (this) {
    DiaryPickerSort.LAST_EATEN -> "Zuletzt gegessen"
    DiaryPickerSort.MOST_EATEN -> "Am meisten gegessen"
    DiaryPickerSort.NAME -> "Name"
}

/**
 * One tap further along the sort button's cycle, wrapping round to the first. The button shows no
 * word of its own, so this has to stay in the enum's declaration order — that order is what the
 * dropdown behind the same button lists, and the two must agree.
 */
fun DiaryPickerSort.next(): DiaryPickerSort {
    val all = DiaryPickerSort.entries
    return all[(all.indexOf(this) + 1) % all.size]
}
