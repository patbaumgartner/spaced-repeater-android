package com.patbaumgartner.android.spacedrepeater.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patbaumgartner.android.spacedrepeater.R
import com.patbaumgartner.android.spacedrepeater.domain.model.ReviewRating
import com.patbaumgartner.android.spacedrepeater.ui.CardContent
import com.patbaumgartner.android.spacedrepeater.ui.theme.SpacedRepeaterTheme

@Composable
internal fun StudyScreen(
    deckId: Long,
    viewModel: StudyViewModel,
    onExit: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(deckId) {
        if (deckId != 0L) viewModel.startSession(deckId)
    }
    val s = state
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (s.total > 0) pluralStringResource(R.plurals.study_remaining, s.remaining) else "",
                style = MaterialTheme.typography.labelLarge,
                color = SpacedRepeaterTheme.accents.textMuted,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = viewModel::undo,
                enabled = viewModel.canUndo(),
            ) {
                Text(stringResource(R.string.study_undo))
            }
        }
        if (s.newCardsHeldBack > 0) {
            Text(
                text = pluralStringResource(R.plurals.study_new_limit_info, s.newCardsHeldBack, s.newCardsHeldBack),
                style = MaterialTheme.typography.labelMedium,
                color = SpacedRepeaterTheme.accents.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(12.dp))

        when {
            s.done -> DoneScreen(onExit)
            s.current == null -> EmptyStudy(onExit)
            else -> FlashcardArea(
                modifier = Modifier.weight(1f),
                front = s.current!!.front,
                back = s.current!!.back,
                revealed = s.answered,
                onReveal = viewModel::reveal,
                onRate = viewModel::rate,
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.FlashcardArea(
    modifier: Modifier = Modifier,
    front: String,
    back: String,
    revealed: Boolean,
    onReveal: () -> Unit,
    onRate: (ReviewRating) -> Unit,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onReveal,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CardContent(front, style = MaterialTheme.typography.headlineSmall)
                if (revealed) {
                    Spacer(Modifier.height(16.dp))
                    CardContent(
                        markdown = back,
                        style = MaterialTheme.typography.bodyLarge,
                        color = SpacedRepeaterTheme.accents.textMuted,
                    )
                } else {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.study_tap_to_reveal),
                        style = MaterialTheme.typography.labelLarge,
                        color = SpacedRepeaterTheme.accents.textMuted,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    if (revealed) {
        RatingButtons(onRate)
    } else {
        Button(onClick = onReveal, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.study_reveal))
        }
    }
}

@Composable
private fun RatingButtons(onRate: (ReviewRating) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val ratings = listOf(
            ReviewRating.AGAIN to stringResource(R.string.study_rating_again),
            ReviewRating.HARD to stringResource(R.string.study_rating_hard),
            ReviewRating.GOOD to stringResource(R.string.study_rating_good),
            ReviewRating.EASY to stringResource(R.string.study_rating_easy),
        )
        for ((rating, label) in ratings) {
            Button(
                onClick = { onRate(rating) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (rating) {
                        ReviewRating.AGAIN -> SpacedRepeaterTheme.accents.error
                        ReviewRating.EASY -> SpacedRepeaterTheme.accents.brand
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(label, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DoneScreen(onExit: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.study_done_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.study_done_message),
            style = MaterialTheme.typography.bodyMedium,
            color = SpacedRepeaterTheme.accents.textMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_done_done))
        }
    }
}

@Composable
private fun EmptyStudy(onExit: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.study_empty), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.study_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = SpacedRepeaterTheme.accents.textMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_done_done))
        }
    }
}
