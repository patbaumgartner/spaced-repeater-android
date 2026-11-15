package com.patbaumgartner.android.spacedrepeater.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patbaumgartner.android.spacedrepeater.R
import com.patbaumgartner.android.spacedrepeater.ui.AuthorNote
import com.patbaumgartner.android.spacedrepeater.ui.PageHeader
import com.patbaumgartner.android.spacedrepeater.ui.theme.SpacedRepeaterTheme

@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    var licensesShown by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
    ) {
        PageHeader(stringResource(R.string.settings_title), onBack)

        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(
                stringResource(R.string.settings_study_section),
                style = MaterialTheme.typography.labelLarge,
                color = SpacedRepeaterTheme.accents.textMuted,
            )
            Spacer(Modifier.height(4.dp))
            NewPerDayRow(
                value = state.newPerDay,
                onChange = viewModel::setNewPerDay,
            )
            HorizontalDivider()
            SwitchRow(
                label = stringResource(R.string.settings_reminders),
                checked = state.remindersEnabled,
                onCheckedChange = viewModel::setRemindersEnabled,
            )
            HorizontalDivider()

            Spacer(Modifier.height(24.dp))
            AuthorNote(onShowTypefaceLicense = { licensesShown = true })
        }
    }

    if (licensesShown) {
        val licenseText = remember(resources) {
            resources.openRawResource(R.raw.manrope_license).bufferedReader().use { it.readText() }
        }
        AlertDialog(
            onDismissRequest = { licensesShown = false },
            title = { Text(stringResource(R.string.about_open_source_licenses), color = SpacedRepeaterTheme.accents.brand) },
            text = {
                Text(
                    text = licenseText,
                    modifier = Modifier.height(360.dp).verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = { licensesShown = false }) { Text(stringResource(R.string.about_close)) }
            },
        )
    }
}

@Composable
private fun NewPerDayRow(
    value: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_new_per_day),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = if (value == 0) {
                    stringResource(R.string.settings_new_per_day_unlimited)
                } else {
                    stringResource(R.string.settings_new_per_day_value, value)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = SpacedRepeaterTheme.accents.textMuted,
            )
        }
        Stepper(
            onDecrement = { if (value > 0) onChange(value - 5) },
            onIncrement = { onChange(value + 5) },
            decrementEnabled = value > 0,
        )
    }
}

/** Compact +/− stepper used for numeric settings. */
@Composable
private fun Stepper(
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementEnabled: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.TextButton(onClick = onDecrement, enabled = decrementEnabled) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        androidx.compose.material3.TextButton(onClick = { onIncrement() }) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
