package com.patbaumgartner.android.spacedrepeater.ui.screens

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
internal fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDeck: (Long) -> Unit,
    onImport: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<HomeDeckUi?>(null) }

    pendingDelete?.let { row ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.deck_delete_title)) },
            text = { Text(stringResource(R.string.deck_delete_message, row.deck.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDeck(row.deck)
                    pendingDelete = null
                }) { Text(stringResource(R.string.deck_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.deck_delete_cancel))
                }
            },
        )
    }

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
                    DeckCard(
                        row,
                        onOpenDeck = { onOpenDeck(row.deck.id) },
                        onDelete = { pendingDelete = row },
                    )
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
private fun DeckCard(row: HomeDeckUi, onOpenDeck: () -> Unit, onDelete: () -> Unit) {
    val deck = row.deck
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        onClick = onOpenDeck,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
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
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        SpacedRepeaterIcons.Delete,
                        contentDescription = stringResource(R.string.deck_delete_title),
                        tint = SpacedRepeaterTheme.accents.textMuted,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.deck_delete_confirm)) },
                        leadingIcon = { Icon(SpacedRepeaterIcons.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}
