package com.patbaumgartner.android.spacedrepeater.ui

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.patbaumgartner.android.spacedrepeater.R
import com.patbaumgartner.android.spacedrepeater.data.importing.DeckImporter
import com.patbaumgartner.android.spacedrepeater.data.importing.DeckMediaStore
import com.patbaumgartner.android.spacedrepeater.data.importing.MarkdownParser
import com.patbaumgartner.android.spacedrepeater.data.local.SpacedRepeaterDatabase
import com.patbaumgartner.android.spacedrepeater.data.local.roomDbTransaction
import com.patbaumgartner.android.spacedrepeater.data.repository.CardRepository
import com.patbaumgartner.android.spacedrepeater.data.repository.DeckRepository
import com.patbaumgartner.android.spacedrepeater.ui.screens.ImportScreen
import com.patbaumgartner.android.spacedrepeater.ui.screens.ImportViewModel
import com.patbaumgartner.android.spacedrepeater.ui.theme.SpacedRepeaterTheme
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the import flow — UC-01 Import Deck, closing the
 * "UI | not yet covered" gap documented in arc42 §8.8 (Testing Strategy).
 *
 * Real ImportScreen + real ImportViewModel over an in-memory Room database
 * and the real DeckImporter. The SAF document picker is bypassed by writing
 * fixture ZIPs into a test file and handing its [Uri] to the view model —
 * the same ContentResolver path production uses.
 */
@RunWith(AndroidJUnit4::class)
class ImportScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: SpacedRepeaterDatabase
    private lateinit var viewModel: ImportViewModel

    @Before
    fun setup(): Unit = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SpacedRepeaterDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val cardRepository = CardRepository(
            db = db,
            cardDao = db.cardDao(),
            cardStateDao = db.cardStateDao(),
            reviewLogDao = db.reviewLogDao(),
            scheduler = com.patbaumgartner.android.spacedrepeater.data.scheduler.SM2Scheduler(),
            dbTransaction = roomDbTransaction(db),
        )
        val deckRepository = DeckRepository(
            deckDao = db.deckDao(),
            mediaStore = DeckMediaStore(context.getDir("test-media", Context.MODE_PRIVATE)),
        )
        viewModel = ImportViewModel(
            DeckImporter(
                deckRepository = deckRepository,
                cardRepository = cardRepository,
                parser = MarkdownParser(),
                mediaStore = DeckMediaStore(context.getDir("test-staging", Context.MODE_PRIVATE)),
            ),
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun deckRepository() = DeckRepository(
        deckDao = db.deckDao(),
        mediaStore = DeckMediaStore(context.getDir("test-media", Context.MODE_PRIVATE)),
    )

    /** Writes a fixture ZIP to the test app's cache dir and returns its Uri. */
    private fun fixtureUri(name: String, cardCount: Int): Uri {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("deck.json"))
            zip.write("""{"name":"$name","description":"Fixture","cardCount":$cardCount,"tags":["t"]}""".toByteArray())
            zip.closeEntry()
            for (i in 1..cardCount) {
                zip.putNextEntry(ZipEntry("cards/card%02d.md".format(i)))
                zip.write("Q$i\n---\nA$i\n".toByteArray())
                zip.closeEntry()
            }
        }
        val file = java.io.File(context.cacheDir, name.replace(" ", "_") + ".zip")
        file.writeBytes(out.toByteArray())
        return Uri.fromFile(file)
    }

    private fun setContent() {
        composeRule.setContent {
            SpacedRepeaterTheme {
                ImportScreen(viewModel = viewModel, onBack = {}, onImported = {})
            }
        }
    }

    /** Waits until a node with the given text exists (recomposition can lag seconds on slow emulators). */
    private fun waitUntilText(text: String) {
        composeRule.waitUntil(timeoutMillis = 60_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun selectArchive(name: String, cardCount: Int) {
        val uri = fixtureUri(name, cardCount)
        // The SAF picker is UI-external; hand the Uri to the view model exactly
        // as the ActivityResult callback would.
        viewModel.onArchiveSelected(context.contentResolver, uri)
    }

    @Test
    fun importFlowShowsPreviewThenSuccessSummary() {
        setContent()
        composeRule.onNodeWithText(context.getString(R.string.import_pick))
            .assertIsDisplayed()

        selectArchive("Preview Deck", cardCount = 4)
        composeRule.waitUntil(timeoutMillis = 60_000) { viewModel.uiState.value.preview != null }
        waitUntilText("Preview Deck")

        // Preview shows manifest name and card count (UC-01 steps 3–4).
        composeRule.onNodeWithText("Preview Deck").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.import_confirm)).performClick()

        composeRule.waitUntil(timeoutMillis = 60_000) { viewModel.uiState.value.lastResult != null }
        waitUntilText(context.getString(R.string.import_done_title))

        composeRule.onNodeWithText(context.getString(R.string.import_done_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.import_done_done)).assertIsDisplayed()

        // The deck really landed in the database with all its cards (durability).
        val decks = runBlocking { deckRepository().observeDecks().first() }
        assertEquals(1, decks.size)
        assertEquals("Preview Deck", decks.first().deck.name)
        val cardCount = runBlocking { db.cardDao().getByDeckId(decks.first().deck.id).size }
        assertEquals(4, cardCount)
    }

    @Test
    fun invalidArchiveShowsErrorAndCancelReturnsToStart() {
        setContent()

        val bad = java.io.File(context.cacheDir, "broken.zip")
        bad.writeBytes("not a zip at all".toByteArray())
        viewModel.onArchiveSelected(context.contentResolver, Uri.fromFile(bad))

        composeRule.waitUntil(timeoutMillis = 60_000) { viewModel.uiState.value.error != null }
        waitUntilText(context.getString(R.string.error_import, viewModel.uiState.value.error!!))

        composeRule.onNodeWithText(context.getString(R.string.error_import, viewModel.uiState.value.error!!))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.import_cancel)).performClick()

        // Back to the initial pick state (UC-01 extension: abort).
        composeRule.waitUntil(timeoutMillis = 60_000) { viewModel.uiState.value.error == null }
        waitUntilText(context.getString(R.string.import_pick))
        composeRule.onNodeWithText(context.getString(R.string.import_pick)).assertIsDisplayed()
        assertTrue(runBlocking { deckRepository().observeDecks().first().isEmpty() })
    }

    @Test
    fun emptyDeckIsRejectedWithError() {
        setContent()

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("deck.json"))
            zip.write("""{"name":"Empty Deck","cardCount":0}""".toByteArray())
            zip.closeEntry()
        }
        val file = java.io.File(context.cacheDir, "empty.zip")
        file.writeBytes(out.toByteArray())
        viewModel.onArchiveSelected(context.contentResolver, Uri.fromFile(file))
        composeRule.waitUntil(timeoutMillis = 60_000) { viewModel.uiState.value.preview != null }
        waitUntilText(context.getString(R.string.import_confirm))

        composeRule.onNodeWithText(context.getString(R.string.import_confirm)).performClick()
        composeRule.waitUntil(timeoutMillis = 60_000) { viewModel.uiState.value.error != null }
        waitUntilText(context.getString(R.string.import_cancel))

        // "Deck contains no cards" → error screen, no deck persisted.
        composeRule.onNodeWithText(context.getString(R.string.import_cancel)).assertIsDisplayed()
        assertTrue(runBlocking { deckRepository().observeDecks().first().isEmpty() })
    }
}
