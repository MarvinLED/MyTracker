package com.example.mytracker.achievements

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.ui.theme.AppDomain
import com.example.mytracker.ui.theme.topAppBarColors

private val BarShape = RoundedCornerShape(6.dp)
private val ChipShape = RoundedCornerShape(16.dp)
private val BarHeight = 8.dp

/**
 * Everything that has already been earned, in one place: best marks, runs, and the milestone ladders
 * that are still being climbed.
 *
 * Nothing on this screen is awarded — it is all read back out of what was logged, so the wall is
 * full the very first time it is opened rather than starting empty and asking for patience.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AchievementsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Once the wall has actually been drawn, everything on it counts as seen — so the next visit
    // can tell what has changed since. Keyed on `loaded` so it runs on the first real state, not on
    // the empty one the screen starts with.
    LaunchedEffect(uiState.loaded) {
        if (uiState.loaded) viewModel.markSeen()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = AppDomain.ACHIEVEMENTS.topAppBarColors(),
                title = { Text("Erfolge") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menü")
                    }
                },
            )
        },
    ) { padding ->
        // Only once the first collection has arrived: an empty wall announced while the data is
        // still loading would tell a long-time user they have achieved nothing.
        if (uiState.loaded && uiState.isEmpty) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Hier ist noch nichts zu holen. Sobald du Training, Gewicht, Habits oder " +
                        "Mahlzeiten erfasst, füllt sich diese Wand von allein — rückwirkend, aus " +
                        "allem, was schon da ist.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "figure") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Deine Figur", style = MaterialTheme.typography.titleMedium)
                        AvatarFigure(current = uiState.attributes, items = uiState.unlockedItems)
                        Text(
                            "Sie folgt den letzten 30 Tagen — die Kontur dahinter ist deine " +
                                "Bestform, und die bleibt.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        uiState.attributes.forEach { level -> AttributeRow(level) }
                        if (uiState.lastBookedPoints > 0.0) {
                            Text(
                                "Gestern verdient: ${uiState.lastBookedPoints.formatCompact()} Punkte",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item(key = "gear") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("🎽 Ausstattung", style = MaterialTheme.typography.titleMedium)
                        if (uiState.unlockedItems.isEmpty()) {
                            Text(
                                "Noch nichts freigeschaltet — Ausstattung hängt an den " +
                                    "Bestwerten deiner Attribute, nicht an der Tagesform.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // In enum order, which is roughly the order they get earned in, so
                                // the collection reads as a progression rather than a bag.
                                AvatarItem.entries
                                    .filter { it in uiState.unlockedItems }
                                    .forEach { item ->
                                        GearChip(item = item, isNew = item in uiState.newItems)
                                    }
                            }
                        }
                        // Always a visible next thing while anything is still locked: that is what
                        // keeps a collection from ever feeling finished.
                        uiState.nextUnlock?.let { next ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Als Nächstes: ${next.item.label}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                MilestoneBar(
                                    fraction = next.fraction,
                                    label = "${next.item.label}: noch ${next.remaining} Stufen",
                                )
                                Text(
                                    "braucht ${next.item.requirementText()} — noch ${next.remaining}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            uiState.sections.forEach { section ->
                item(key = "section-${section.title}") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                "${sectionMark(section.title)} ${section.title}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            section.items.forEach { item -> AchievementRow(item) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The emoji in front of a section heading. Decoration only — the heading beside it says the same
 * thing in words, so nothing is lost where the emoji cannot be read.
 */
private fun sectionMark(title: String): String = when (title) {
    "Bestmarken" -> "🏅"
    "Serien" -> "🔥"
    else -> "📈"
}

/**
 * One attribute under the figure: what it is, what it drives, and how far to the next level.
 *
 * The record is spelled out beside the level rather than left to the silhouette — the outline says
 * "you were bigger once", but only the number says by how much.
 */
@Composable
private fun AttributeRow(level: AttributeLevel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text(level.attribute.label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    level.attribute.shows,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                buildString {
                    append("Stufe ${level.level}")
                    if (level.record > level.level) append(" · Rekord ${level.record}")
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        MilestoneBar(
            fraction = level.fraction,
            label = "${level.attribute.label}: Stufe ${level.level}, " +
                "auf dem Weg zu ${level.level + 1}",
        )
    }
}

/**
 * One earned piece of equipment. New ones grow in once when the card appears — the single animation
 * on this screen, spent on the one thing that only happens occasionally.
 */
@Composable
private fun GearChip(item: AvatarItem, isNew: Boolean) {
    val scale = remember(item, isNew) { Animatable(if (isNew) 0.6f else 1f) }
    LaunchedEffect(item, isNew) {
        if (isNew) scale.animateTo(1f, animationSpec = tween(durationMillis = 420))
    }

    val container = if (isNew) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    Row(
        modifier = Modifier
            .scale(scale.value)
            .clip(ChipShape)
            .background(container)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(item.label, style = MaterialTheme.typography.labelLarge)
        // The word, not only the colour — the highlight alone would be invisible to half the
        // reasons someone might miss it.
        if (isNew) {
            Text(
                "NEU",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AchievementRow(item: Achievement) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (item.isNew) "✨ ${item.title}" else item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.isNew) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            // The mark itself carries the weight of the row — it is the thing being celebrated.
            Text(
                item.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        item.detail?.let { detail ->
            Text(
                detail,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Only a milestone has a way still to go; a record is simply reached.
        item.fraction?.let { fraction ->
            MilestoneBar(fraction = fraction, label = item.nextLabel)
            item.nextLabel?.let { next ->
                Text(
                    next,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MilestoneBar(fraction: Float, label: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BarHeight)
            .clip(BarShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            // The bar's fill is the only thing saying how far along this is, and a screen reader
            // sees none of it — so it gets the same sentence the label under it carries.
            .semantics { contentDescription = label ?: "Höchster Meilenstein erreicht" },
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
