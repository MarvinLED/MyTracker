package com.example.prokject2_tracker.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SleepTagManageViewModel @Inject constructor(
    private val sleepRepository: SleepRepository,
) : ViewModel() {
    val tags: StateFlow<List<SleepTag>> = sleepRepository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun create(name: String) {
        viewModelScope.launch { sleepRepository.createTag(name) }
    }

    fun rename(tag: SleepTag, name: String) {
        viewModelScope.launch { sleepRepository.renameTag(tag, name) }
    }

    /**
     * A tag in use isn't delete-blocked — the nights keep their data, they just lose the label, the
     * same call as the Körperstellen screen makes. [onConfirmNeeded] reports how many nights that
     * would touch so the screen can ask first.
     */
    fun requestDelete(tag: SleepTag, onConfirmNeeded: (Int) -> Unit) {
        viewModelScope.launch {
            val count = sleepRepository.tagUsageCount(tag.id)
            if (count == 0) sleepRepository.deleteTag(tag) else onConfirmNeeded(count)
        }
    }

    fun delete(tag: SleepTag) {
        viewModelScope.launch { sleepRepository.deleteTag(tag) }
    }
}
