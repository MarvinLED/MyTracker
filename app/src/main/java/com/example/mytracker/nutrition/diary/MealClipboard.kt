package com.example.mytracker.nutrition.diary

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A copied Tageszeit, waiting to be pasted somewhere else. [epochDay] and [mealType] are where it
 * was copied *from* — the paste bar says so, since the whole point is that the target is a different
 * day or meal.
 */
data class CopiedMeal(
    val epochDay: Long,
    val mealType: MealType,
    val entries: List<DiaryEntrySnapshot>,
)

/**
 * Holds the one copied Tageszeit between the long press and the paste.
 *
 * A singleton rather than state in [DiaryViewModel] so the copy survives leaving the Tagebuch — you
 * can look something up in the Bibliothek and still paste afterwards. Deliberately in memory only:
 * a clipboard is scratch, and one restored days later from disk would paste something the user has
 * long forgotten copying.
 */
@Singleton
class MealClipboard @Inject constructor() {
    private val _copied = MutableStateFlow<CopiedMeal?>(null)
    val copied: StateFlow<CopiedMeal?> = _copied.asStateFlow()

    fun put(meal: CopiedMeal) {
        _copied.value = meal
    }

    fun clear() {
        _copied.value = null
    }
}
