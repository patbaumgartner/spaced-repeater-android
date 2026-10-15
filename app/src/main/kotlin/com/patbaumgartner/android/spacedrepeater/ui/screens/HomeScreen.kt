package com.patbaumgartner.android.spacedrepeater.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patbaumgartner.android.spacedrepeater.R
import com.patbaumgartner.android.spacedrepeater.ui.SpacedRepeaterIcons
import com.patbaumgartner.android.spacedrepeater.ui.theme.SpacedRepeaterTheme
import java.time.Instant

@Composable
internal fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDeck: (Long) -> Unit,
    onImport: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = stringResource(R.string.home_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SpacedRepeaterTheme.accents.textMuted,
                        )
                    }
                    IconButton(onClick = onStats) {
                        Icon(
                            SpacedRepeaterIcons.Stats,
                            contentDescription = stringResource(R.string.stats_title),
                            tint = SpacedRepeaterTheme.accents.textMuted,
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(
                            SpacedRepeaterIcons.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = SpacedRepeaterTheme.accents.textMuted,
                        )
                    }
                }
            }

            when {
                state.loading -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.decks.isEmpty() -> item { EmptyHome(onImport) }
                else -> items(state.decks, key = { it.deck.id }) { row ->
                    DeckCard(row, onOpenDeck = { onOpenDeck(row.deck.id) })
                }
            }
        }

        FloatingActionButton(
            onClick = onImport,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(SpacedRepeaterIcons.Add, contentDescription = stringResource(R.string.home_import))
        }
    }
}

@Composable
private fun EmptyHome(onImport: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.home_empty), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(8.dp))
        Text(
            stringResource(R.string.home_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = SpacedRepeaterTheme.accents.textMuted,
        )
    }
}

@Composable
private fun DeckCard(row: HomeDeckUi, onOpenDeck: () -> Unit) {
    val deck = row.deck
    Card(
        onClick = onOpenDeck,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(deck.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.size(4.dp))
                Text(
                    stringResource(R.string.home_progress, row.studiedCount, deck.cardCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpacedRepeaterTheme.accents.textMuted,
                )
            }
            if (row.dueCount > 0) {
                Text(
                    text = pluralStringResource(R.plurals.home_due_badge, row.dueCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = SpacedRepeaterTheme.accents.brand,
                    modifier = Modifier.padding(start = 12.dp),
                )
            } else {
                Text(
                    text = stringResource(R.string.home_caught_up),
                    style = MaterialTheme.typography.labelLarge,
                    color = SpacedRepeaterTheme.accents.textMuted,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}
