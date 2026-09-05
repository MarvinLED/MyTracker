package com.example.mytracker.nutrition.library

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.mytracker.nutrition.diary.DiaryPickerSort
import com.example.mytracker.nutrition.diary.label
import com.example.mytracker.nutrition.food.Tag
import com.example.mytracker.nutrition.food.TagDot
import com.example.mytracker.nutrition.food.displayColor

/**
 * The two filter buttons beside the search field. They carry no word of their own — the row has to
 * leave the search field its width — so each state gets its **own icon**, never just its own colour,
 * and the spoken description names the state in full.
 *
 * Short press moves one state on, long press opens the whole list with a tick on the current one.
 * The short press is the fast way through two or three states; the long press is the way out when
 * there are more of them than that.
 */
@Composable
private fun CycleIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .combinedClickable(
                role = Role.Button,
                onLongClickLabel = "Auswahl öffnen",
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Zuletzt → Am meisten → Name. */
@Composable
fun SortButton(
    sort: DiaryPickerSort,
    onCycle: () -> Unit,
    onSelect: (DiaryPickerSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        CycleIconButton(
            icon = sort.icon(),
            contentDescription = "Sortierung: ${sort.label()}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onCycle,
            onLongClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DiaryPickerSort.entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.label()) },
                    onClick = {
                        onSelect(entry)
                        expanded = false
                    },
                    leadingIcon = { Icon(entry.icon(), contentDescription = null) },
                    trailingIcon = {
                        if (entry == sort) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                )
            }
        }
    }
}

/**
 * Alle → each tag in turn → Alle. An active filter is a different icon in the tag's own colour, so
 * that something is filtered is visible even where the colour is not.
 */
@Composable
fun TagFilterButton(
    tags: List<Tag>,
    selectedTagId: String?,
    onCycle: () -> Unit,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = tags.indexOfFirst { it.id == selectedTagId }
    val selectedTag = tags.getOrNull(selectedIndex)

    Box(modifier = modifier) {
        CycleIconButton(
            icon = if (selectedTag != null) Icons.Filled.FilterAlt else Icons.AutoMirrored.Filled.Label,
            contentDescription = "Tag-Filter: ${selectedTag?.name ?: "alle"}",
            tint = selectedTag?.displayColor(selectedIndex) ?: MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onCycle,
            onLongClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Alle") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
                trailingIcon = {
                    if (selectedTagId == null) Icon(Icons.Filled.Check, contentDescription = null)
                },
            )
            tags.forEachIndexed { index, tag ->
                DropdownMenuItem(
                    text = { Text(tag.name) },
                    onClick = {
                        onSelect(tag.id)
                        expanded = false
                    },
                    leadingIcon = { TagDot(color = tag.displayColor(index), size = 12) },
                    trailingIcon = {
                        if (selectedTagId == tag.id) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                )
            }
        }
    }
}

/** One picture per order, so the button says which one it is on without a word under it. */
private fun DiaryPickerSort.icon(): ImageVector = when (this) {
    DiaryPickerSort.LAST_EATEN -> Icons.Filled.History
    DiaryPickerSort.MOST_EATEN -> Icons.Filled.Repeat
    DiaryPickerSort.NAME -> Icons.Filled.SortByAlpha
}
