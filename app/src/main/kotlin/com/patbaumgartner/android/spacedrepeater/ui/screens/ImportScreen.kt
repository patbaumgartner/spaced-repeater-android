package com.patbaumgartner.android.spacedrepeater.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patbaumgartner.android.spacedrepeater.R
import com.patbaumgartner.android.spacedrepeater.ui.PageHeader
import com.patbaumgartner.android.spacedrepeater.ui.theme.SpacedRepeaterTheme

@Composable
internal fun ImportScreen(
    viewModel: ImportViewModel,
    onBack: () -> Unit,
    onImported: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.onArchiveSelected(context.contentResolver, uri)
        }
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        PageHeader(stringResource(R.string.import_title), onBack)

        when {
            state.importing -> Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.import_importing))
            }

            state.lastResult != null -> {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(stringResource(R.string.import_done_title), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        pluralStringResource(
                            R.plurals.import_done_message,
                            state.lastResult!!.imported,
                            state.lastResult!!.imported,
                            state.lastResult!!.skipped,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onImported, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.import_done_done))
                    }
                }
            }

            state.error != null -> {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(
                        stringResource(R.string.error_import, state.error!!),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpacedRepeaterTheme.accents.error,
                    )
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = viewModel::reset) {
                        Text(stringResource(R.string.import_cancel))
                    }
                }
            }

            state.preview != null -> {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(
                        stringResource(R.string.import_preview_name),
                        style = MaterialTheme.typography.labelLarge,
                        color = SpacedRepeaterTheme.accents.textMuted,
                    )
                    Text(state.preview!!.name, style = MaterialTheme.typography.headlineSmall)
                    if (state.preview!!.description.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.import_preview_description),
                            style = MaterialTheme.typography.labelLarge,
                            color = SpacedRepeaterTheme.accents.textMuted,
                        )
                        Text(state.preview!!.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (state.preview!!.tags.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.import_preview_tags),
                            style = MaterialTheme.typography.labelLarge,
                            color = SpacedRepeaterTheme.accents.textMuted,
                        )
                        Text(state.preview!!.tags.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        pluralStringResource(R.plurals.import_preview_cards, state.preview!!.cardCount),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.confirmImport(context.contentResolver) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.import_confirm))
                    }
                    TextButton(onClick = viewModel::reset, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.import_cancel))
                    }
                }
            }

            else -> {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Button(
                        onClick = { launcher.launch(arrayOf("application/zip", "application/octet-stream")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.import_pick))
                    }
                }
            }
        }
    }
}
