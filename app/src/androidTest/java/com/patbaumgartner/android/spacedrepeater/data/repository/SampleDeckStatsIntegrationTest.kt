package com.patbaumgartner.android.spacedrepeater.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.patbaumgartner.android.spacedrepeater.data.importing.DeckImporter
import com.patbaumgartner.android.spacedrepeater.data.importing.DeckMediaStore
import com.patbaumgartner.android.spacedrepeater.data.importing.MarkdownParser
import com.patbaumgartner.android.spacedrepeater.data.local.SpacedRepeaterDatabase
import com.patbaumgartner.android.spacedrepeater.data.local.card.CardDao
import com.patbaumgartner.android.spacedrepeater.data.local.model.CardEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.DeckEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.ReviewLogEntity
import com.patbaumgartner.android.spacedrepeater.data.local.roomDbTransaction
import com.patbaumgartner.android.spacedrepeater.data.scheduler.SM2Scheduler
import com.patbaumgartner.android.spacedrepeater.domain.model.ReviewRating
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.time.ZoneId

/**
 * Integration test that imports the real sample deck from Dropbox and drives
 * study sessions against the real repositories and Room DB.
 *
 * Validates UC-06 (View Progress): streak, retention, and cards-studied-today
 * are correct after review sessions spanning multiple days.
 *
 * Run on a device or emulator via: ./gradlew connectedDebugAndroidTest
 *
 * The test skips gracefully when the Dropbox sample zip is absent.
 */
@RunWith(AndroidJUnit4::class)
class SampleDeckStatsIntegrationTest {

    private lateinit var db: SpacedRepeaterDatabase
    private lateinit var cardRepository: CardRepository
    private lateinit var statsRepository: StatsRepository
    private lateinit var importer: DeckImporter

    /** Path to the sample deck maintained at ~/Dropbox/spaced-repeater-sample/. */
    private val sampleZip = File(
        System.getenv("HOME")
            .orEmpty()
            .replace("/home/patbaumgartner", "/mnt/c/Users/patbaumgartner")
            + "/Dropbox/spaced-repeater-sample/web-entwicklung-hs2026-gesichter-namen.zip",
    )

    private val zone = ZoneId.of("UTC")

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SpacedRepeaterDatabase::class.java)
            .build()
        val mediaStore = DeckMediaStore(context.cacheDir)
        val scheduler = SM2Scheduler()
        val deckRepository = DeckRepository(db.deckDao(), mediaStore)
        cardRepository = CardRepository(
            db = db,
            cardDao = db.cardDao(),
            cardStateDao = db.cardStateDao(),
            reviewLogDao = db.reviewLogDao(),
            scheduler = scheduler,
            dbTransaction = roomDbTransaction(db),
        )
        statsRepository = StatsRepository(db.reviewLogDao())
        importer = DeckImporter(
            deckRepository = deckRepository,
            cardRepository = cardRepository,
            parser = MarkdownParser(),
            mediaStore = mediaStore,
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    /** Milliseconds at midnight UTC for days ago relative to now in [zone]. */
    private fun midnight(daysAgo: Int): Long =
        LocalDate.now(zone).minusDays(daysAgo.toLong())
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

    private suspend fun insertCardStub(): Long {
        val deckId = db.deckDao().insert(DeckEntity(name = "Stub", createdAt = 0L))
        return db.cardDao().insertAll(
            listOf(CardEntity(deckId = deckId, front = "Q", back = "A", createdAt = 0L)),
        ).first()
    }

    @Test
    fun sampleDeck_importsWith70Cards() = runBlocking {
        if (!sampleZip.isFile) {
            println("Skipping: Dropbox sample deck not found at $sampleZip")
            return@runBlocking
        }

        val result = FileInputStream(sampleZip).use { importer.importArchive(it) }
        assertEquals("deck id assigned", true, result.deckId > 0)
        assertEquals("70 cards imported", 70, result.importedCards)
        assertEquals("no skipped cards", 0, result.skippedCards)
    }

    @Test
    fun afterStudySession_statsShowStudiedTodayAndStreak() = runBlocking {
        if (!sampleZip.isFile) {
            println("Skipping: Dropbox sample deck not found at $sampleZip")
            return@runBlocking
        }

        FileInputStream(sampleZip).use { importer.importArchive(it) }
        val deckId = db.deckDao().allDecks().first().id
        val cardIds = db.cardDao().getByDeckId(deckId)

        // Simulate a full review session: rate all 70 cards GOOD.
        // Stamp the log entries as "today" so the stats query picks them up.
        val now = System.currentTimeMillis()
        cardIds.forEach { card ->
            val rated = cardRepository.rate(card.id, ReviewRating.GOOD, timeTakenMs = 2_000L)
            db.reviewLogDao().updateReviewedAt(rated.logId, now)
        }

        val stats = statsRepository.computeStats(zone)
        assertEquals("studied today = 70", 70, stats.studiedToday)
        assertNotNull("retention computed", stats.retention)
        assertTrue("streak ≥ 1", stats.streakDays >= 1)
    }

    @Test
    fun streakIsZero_whenNoReviewsAtAll() = runBlocking {
        val stats = statsRepository.computeStats(zone)
        assertEquals(0, stats.streakDays)
        assertEquals(0, stats.studiedToday)
        assertNull(stats.retention)
    }

    @Test
    fun streakCarriesOverFromYesterday_whenNotStudiedToday() = runBlocking {
        // Pre-populate review log with a 3-day streak ending yesterday.
        val cardId = insertCardStub()
        listOf(midnight(1), midnight(2), midnight(3)).forEach { ts ->
            db.reviewLogDao().insert(
                ReviewLogEntity(
                    cardId = cardId,
                    rating = 2,
                    interval = 1,
                    easeFactor = 2.5f,
                    reviewedAt = ts,
                ),
            )
        }

        val stats = statsRepository.computeStats(zone)
        // Streak should be 3, not reset to 0 just because today is absent.
        assertEquals(3, stats.streakDays)
    }
}