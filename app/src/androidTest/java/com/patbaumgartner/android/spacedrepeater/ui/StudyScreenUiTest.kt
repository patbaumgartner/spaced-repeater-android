package com.patbaumgartner.android.spacedrepeater.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.patbaumgartner.android.spacedrepeater.data.importing.DeckImporter
import com.patbaumgartner.android.spacedrepeater.data.importing.DeckMediaStore
import com.patbaumgartner.android.spacedrepeater.data.importing.MarkdownParser
import com.patbaumgartner.android.spacedrepeater.data.local.SpacedRepeaterDatabase
import com.patbaumgartner.android.spacedrepeater.data.local.roomDbTransaction
import com.patbaumgartner.android.spacedrepeater.data.repository.AppPreferencesRepository
import com.patbaumgartner.android.spacedrepeater.data.repository.CardRepository
import com.patbaumgartner.android.spacedrepeater.data.repository.DeckRepository
import com.patbaumgartner.android.spacedrepeater.data.repository.StudyLimitsRepository
import com.patbaumgartner.android.spacedrepeater.service.ReviewReminderService
import com.patbaumgartner.android.spacedrepeater.ui.screens.StudyScreen
import com.patbaumgartner.android.spacedrepeater.ui.screens.StudyViewModel
import com.patbaumgartner.android.spacedrepeater.ui.theme.SpacedRepeaterTheme
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the study session flow — UC-03 Study Deck and UC-04
 * Rate Recall, closing the "UI | not yet covered" gap documented in arc42
 * §8.8 (Testing Strategy).
 *
 * Real StudyScreen + real StudyViewModel over an in-memory Room database and
 * the real importer, so the full production path is exercised (SAF is the
 * only part bypassed: the deck ZIP is imported directly from a byte array).
 */
@RunWith(AndroidJUnit4::class)
class StudyScreenUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var db: SpacedRepeaterDatabase
    private lateinit var viewModel: StudyViewModel
    private var deckId: Long = -1L

    @Before
    fun setup(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Test isolation: the daily-limit test writes app-scoped SharedPreferences;
        // drop the file so every test starts with default limits (also clears
        // pollution left by earlier runs on shared devices/emulators).
        context.deleteSharedPreferences("study_limits")
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
            mediaStore = DeckMediaStore(context.getDir("test-media", android.content.Context.MODE_PRIVATE)),
        )
        val importer = DeckImporter(
            deckRepository = deckRepository,
            cardRepository = cardRepository,
            parser = com.patbaumgartner.android.spacedrepeater.data.importing.MarkdownParser(),
            mediaStore = DeckMediaStore(context.getDir("test-staging", android.content.Context.MODE_PRIVATE)),
        )

        // Seed one deck with three due cards through the real importer (UC-01 path).
        importer.importArchive(ByteArrayInputStream(testDeckZip("UI Test Deck", 3)))
        deckId = deckRepository.observeDecks().first().first().deck.id

        viewModel = StudyViewModel(
            cardRepository = cardRepository,
            deckRepository = deckRepository,
            reminderService = noopReminderService(context),
            studyLimitsRepository = StudyLimitsRepository(context, db.reviewLogDao()),
            appPreferencesRepository = AppPreferencesRepository(context),
        )
    }

    @After
    fun teardown() {
        db.close()
    }


    /** Sets the screen and waits until the session has loaded its cards and the window can take input. */
    private fun setContentAndWait() {
        composeRule.setContent {
            SpacedRepeaterTheme {
                StudyScreen(deckId = deckId, viewModel = viewModel, onExit = {})
            }
        }
        composeRule.waitUntil(timeoutMillis = 60_000) {
            viewModel.uiState.value.current != null
        }
        // On slow emulators the window transition can complete after onResume;
        // injected input is silently dropped until the window has focus.
        composeRule.waitUntil(timeoutMillis = 60_000) {
            composeRule.activity.window.decorView.hasWindowFocus()
        }
    }

    /** Waits until a node with the given text exists (recomposition can lag seconds on slow emulators). */
    private fun waitUntilText(text: String) {
        composeRule.waitUntil(timeoutMillis = 60_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun showsQuestionFirstAndRevealsAnswerOnTap() {
        setContentAndWait()

        // Question visible before reveal…
        waitUntilText("Question 1")
        composeRule.onNodeWithText("Question 1").assertIsDisplayed()
        // …answer hidden until the card is tapped…
        composeRule.onNodeWithText("Answer 1").assertDoesNotExist()
        composeRule.onNodeWithText("Question 1").performClick()
        // …then both are visible.
        waitUntilText("Answer 1")
        composeRule.onNodeWithText("Answer 1").assertIsDisplayed()
    }

    @Test
    fun revealButtonShowsAnswerWithoutTappingCard() {
        setContentAndWait()

        waitUntilText("Question 1")
        composeRule.onNodeWithText(
            androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().getString(com.patbaumgartner.android.spacedrepeater.R.string.study_reveal),
        ).performClick()

        composeRule.waitUntil(timeoutMillis = 60_000) { viewModel.uiState.value.answered }
        waitUntilText("Answer 1")
        composeRule.onNodeWithText("Answer 1").assertIsDisplayed()
    }

    @Test
    fun ratingAdvancesToNextCardAndPersistsReview() {
        setContentAndWait()

        waitUntilText("Question 1")
        composeRule.onNodeWithText("Question 1").performClick()
        waitUntilText("Good")
        composeRule.onNodeWithText("Good").performClick()

        waitUntilText("Question 2")
        composeRule.onNodeWithText("Question 2").assertIsDisplayed()

        // The rating is durably persisted (UC-04 data durability).
        val states = runBlocking { db.cardStateDao().getByDeckId(deckId) }
        assertEquals(1, states.count { it.phase != "NEW" })
    }

    @Test
    fun ratingLastCardShowsSessionCompleteScreen() {
        setContentAndWait()

        // Rate all three cards.
        repeat(3) { i ->
            waitUntilText("Question ${i + 1}")
            composeRule.onNodeWithText("Question ${i + 1}").performClick()
            waitUntilText("Good")
            composeRule.onNodeWithText("Good").performClick()
        }

        waitUntilText(
            androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().getString(com.patbaumgartner.android.spacedrepeater.R.string.study_done_title),
        )
        composeRule.onNodeWithText(
            androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().getString(com.patbaumgartner.android.spacedrepeater.R.string.study_done_title),
        ).assertIsDisplayed()
    }

    @Test
    fun undoReturnsToPreviousCardAndRestoresItsState() {
        setContentAndWait()

        // Rate card 1, land on card 2, then undo.
        waitUntilText("Question 1")
        composeRule.onNodeWithText("Question 1").performClick()
        waitUntilText("Good")
        composeRule.onNodeWithText("Good").performClick()
        waitUntilText("Question 2")
        composeRule.onNodeWithText("Question 2").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()

        waitUntilText("Question 1")
        composeRule.onNodeWithText("Question 1").assertIsDisplayed()
        // Undo also restores the card state: everything is NEW again.
        val states = runBlocking { db.cardStateDao().getByDeckId(deckId) }
        assertEquals(0, states.count { it.phase != "NEW" })
    }

    @Test
    fun dailyLimitHoldsBackNewCardsAndShowsInfoLine() {
        // Only 1 of the 3 fresh cards may be introduced today (UC-08).
        runBlocking {
            StudyLimitsRepository(
                ApplicationProvider.getApplicationContext(),
                db.reviewLogDao(),
            ).updateLimits(com.patbaumgartner.android.spacedrepeater.data.repository.StudyLimits(newPerDay = 1))
        }

        setContentAndWait()

        composeRule.waitUntil(timeoutMillis = 60_000) {
            viewModel.uiState.value.cards.isNotEmpty()
        }
        // Session contains exactly one card…
        assertEquals(1, viewModel.uiState.value.cards.size)
        // …and the held-back info line names the other two.
        composeRule.onNodeWithText("2 new cards held back until tomorrow (daily limit)")
            .assertIsDisplayed()
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    /** Builds an in-memory deck ZIP in the documented package format (ADR-0006). */
    private fun testDeckZip(name: String, cardCount: Int): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("deck.json"))
            zip.write(
                """{"name":"$name","description":"Built in-test","cardCount":$cardCount,"tags":["ui-test"]}"""
                    .toByteArray(),
            )
            zip.closeEntry()
            for (i in 1..cardCount) {
                zip.putNextEntry(ZipEntry("cards/card%02d.md".format(i)))
                zip.write("Question $i\n---\nAnswer $i\n".toByteArray())
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    /** No-op reminder service: UI tests must not touch WorkManager. */
    private fun noopReminderService(context: android.content.Context): ReviewReminderService {
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)
        return ReviewReminderService(
            context = context,
            workManager = androidx.work.WorkManager.getInstance(context),
        )
    }
}
