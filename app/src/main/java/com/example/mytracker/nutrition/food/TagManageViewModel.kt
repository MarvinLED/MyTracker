package com.example.mytracker.nutrition.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TagManageViewModel @Inject constructor(
    private val tagRepository: TagRepository,
) : ViewModel() {
    val tags: StateFlow<List<Tag>> = tagRepository.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val implications: StateFlow<List<TagImplication>> = tagRepository.observeAllImplications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Why a create or save did nothing — otherwise the dialog would just close on a taken name. */
    private val _errors = MutableSharedFlow<String>()
    val errors = _errors.asSharedFlow()

    fun create(name: String) {
        viewModelScope.launch {
            if (tagRepository.createTag(name.trim()) == null && name.isNotBlank()) {
                _errors.emit("\"${name.trim()}\" gibt es schon.")
            }
        }
    }

    /**
     * Saves name, colour and the whole set of implied tags in one go: the dialog edits all three
     * together, so applying only some of them would leave the tag in a state the user never chose.
     */
    fun save(tag: Tag, name: String, colorArgb: Int?, impliedTagIds: Set<String>) {
        viewModelScope.launch {
            if (!tagRepository.updateTag(tag, name, colorArgb)) {
                _errors.emit("\"${name.trim()}\" gibt es schon.")
                return@launch
            }
            val current = implications.value.filter { it.childTagId == tag.id }.map { it.parentTagId }.toSet()
            (current - impliedTagIds).forEach { tagRepository.removeImplication(tag.id, it) }
            (impliedTagIds - current).forEach { tagRepository.addImplication(tag.id, it) }
        }
    }

    /**
     * A tag in use isn't delete-blocked — the Lebensmittel keep their data and only lose the label,
     * the same as the Schlaf-Tags. [onConfirmNeeded] reports how many that would touch so the screen
     * can ask first; an unused tag goes straight away.
     */
    fun requestDelete(tag: Tag, onConfirmNeeded: (Int) -> Unit) {
        viewModelScope.launch {
            val count = tagRepository.tagUsageCount(tag.id)
            if (count == 0) tagRepository.deleteTag(tag) else onConfirmNeeded(count)
        }
    }

    fun delete(tag: Tag) {
        viewModelScope.launch { tagRepository.deleteTag(tag) }
    }
}
