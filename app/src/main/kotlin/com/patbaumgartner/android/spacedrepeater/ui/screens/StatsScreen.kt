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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patbaumgartner.android.spacedrepeater.R
import com.patbaumgartner.android.spacedrepeater.ui.PageHeader
import com.patbaumgartner.android.spacedrepeater.ui.theme.SpacedRepeaterTheme
import java.util.Locale

@Composable
internal fun StatsScreen(
    viewModel: StatsViewModel,
    onBack: () -> Unit,
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        PageHeader(stringResource(R.string.stats_title), onBack)
        val s = stats
        if (s == null) {
            Text(
                stringResource(R.string.stats_empty),
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = SpacedRepeaterTheme.accents.textMuted,
            )
            return@Column
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
            StatCell(
                label = stringResource(R.string.stats_studied_today),
                value = s.studiedToday.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCell(
                label = stringResource(R.string.stats_retention),
                value = s.retention?.let { String.format(Locale.ROOT, "%d%%", (it * 100).toInt()) } ?: "—",
                modifier = Modifier.weight(1f),
            )
            StatCell(
                label = stringResource(R.string.stats_streak),
                value = s.streakDays.toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(8.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = SpacedRepeaterTheme.accents.textMuted,
        )
    }
}
