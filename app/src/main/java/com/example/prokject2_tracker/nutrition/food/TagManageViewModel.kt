package com.example.prokject2_tracker.nutrition.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TagManageViewModel @Inject constructor(
    private val tagRepository: TagRepository,
) : ViewModel() {

    val allTags: StateFlow<List<Tag>> = tagRepository.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateTagName(tagId: String, newName: String) {
        viewModelScope.launch {
            tagRepository.updateTagName(tagId, newName)
        }
    }

    fun addTagHierarchy(parentTagId: String, childTagId: String) {
        viewModelScope.launch {
            tagRepository.addTagHierarchy(parentTagId, childTagId)
        }
    }

    fun removeTagHierarchy(parentTagId: String, childTagId: String) {
        viewModelScope.launch {
            tagRepository.removeTagHierarchy(parentTagId, childTagId)
        }
    }
}
