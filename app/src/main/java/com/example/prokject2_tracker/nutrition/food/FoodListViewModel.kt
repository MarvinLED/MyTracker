package com.example.prokject2_tracker.nutrition.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoodListViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val foods: StateFlow<List<FoodItem>> = _query
        .flatMapLatest { q -> if (q.isBlank()) foodRepository.observeAll() else foodRepository.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tagsByFoodId: StateFlow<Map<String, List<Tag>>> = tagRepository.observeTagsByFoodId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun deleteIfUnused(food: FoodItem, onBlocked: () -> Unit) {
        viewModelScope.launch {
            if (foodRepository.canDelete(food.id)) {
                foodRepository.delete(food)
            } else {
                onBlocked()
            }
        }
    }
}
