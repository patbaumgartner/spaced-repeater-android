package com.patbaumgartner.android.spacedrepeater.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.patbaumgartner.android.spacedrepeater.data.local.deck.DeckDao
import com.patbaumgartner.android.spacedrepeater.data.local.model.CardEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.CardStateEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.DeckEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.DeckWithCounts
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented DAO tests using an in-memory Room database.
 *
 * These exercise real SQLite behavior — cascade deletes, join results,
 * Flow emissions — that JVM tests with a mocked Room database cannot verify.
 * Run via: `./gradlew connectedDebugAndroidTest`
 */
@RunWith(AndroidJUnit4::class)
class DeckDaoTest {

    private lateinit var db: SpacedRepeaterDatabase
    private lateinit var deckDao: DeckDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SpacedRepeaterDatabase::class.java,
        ).build()
        deckDao = db.deckDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndGetById() = runBlocking {
        val deck = DeckEntity(name = "Vokabeln", createdAt = 1000L)
        val id = deckDao.insert(deck)
        assertTrue(id > 0)

        val retrieved = deckDao.getById(id)
        assertEquals("Vokabeln", retrieved?.name)
        assertEquals(1000L, retrieved?.createdAt)
    }

    @Test
    fun getByIdReturnsNullForNonexistent() = runBlocking {
        assertNull(deckDao.getById(9999L))
    }

    @Test
    fun observeDecksWithCounts_emitsEmptyWhenNoDecks() = runBlocking {
        val now = System.currentTimeMillis()
        val result = deckDao.observeDecksWithCounts(now).first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun observeDecksWithCounts_showsDueAndStudiedCards() = runBlocking {
        val now = System.currentTimeMillis()

        val deckId = deckDao.insert(DeckEntity(name = "Test", createdAt = now))
        val cardIds = db.cardDao().insertAll(
            listOf(
                CardEntity(id = 0, deckId = deckId, front = "Q1", back = "A1", createdAt = now),
                CardEntity(id = 0, deckId = deckId, front = "Q2", back = "A2", createdAt = now),
            ),
        )
        assertEquals(2, cardIds.size)

        // Card 1: past-due and already reviewed.
        db.cardStateDao().insertAll(
            listOf(
                CardStateEntity(
                    cardId = cardIds[0],
                    dueDate = now - 10_000,
                    lastReviewedAt = now - 5_000,
                    phase = "REVIEW",
                    repetitions = 1,
                    easeFactor = 2.5f,
                    interval = 1,
                ),
                // Card 2: future due-date — not yet due.
                CardStateEntity(
                    cardId = cardIds[1],
                    dueDate = now + 86_400_000,
                    phase = "REVIEW",
                    repetitions = 0,
                    easeFactor = 2.5f,
                    interval = 0,
                ),
            ),
        )

        deckDao.markStudied(deckId, now)

        val decks = deckDao.observeDecksWithCounts(now).first()
        assertEquals(1, decks.size)

        val row: DeckWithCounts = decks[0]
        assertEquals(2, row.totalCards)
        assertEquals(1, row.dueCards)       // Only card 1 is past-due.
        assertEquals(1, row.studiedCards)  // Only card 1 was reviewed.
    }

    @Test
    fun deleteRemovesDeck() = runBlocking {
        val deckId = deckDao.insert(DeckEntity(name = "Delete me", createdAt = 1000L))
        assertEquals("Delete me", deckDao.getById(deckId)?.name)

        deckDao.delete(deckId)
        assertNull(deckDao.getById(deckId))
    }

    @Test
    fun cascadeDeleteRemovesCardsAndStates() = runBlocking {
        val now = System.currentTimeMillis()
        val deckId = deckDao.insert(DeckEntity(name = "Cascade", createdAt = now))

        val cardIds = db.cardDao().insertAll(
            listOf(
                CardEntity(id = 0, deckId = deckId, front = "Q", back = "A", createdAt = now),
            ),
        )
        db.cardStateDao().insertAll(
            listOf(
                CardStateEntity(
                    cardId = cardIds[0],
                    dueDate = now + 86_400_000,
                    phase = "NEW",
                    repetitions = 0,
                    easeFactor = 2.5f,
                    interval = 0,
                ),
            ),
        )

        // Cascade: deleting the deck removes cards and their states.
        deckDao.delete(deckId)

        val remainingCards = db.cardDao().observeDeckCards(deckId).first()
        assertEquals(0, remainingCards.size)
        assertEquals(0, db.cardStateDao().countDue(deckId, now + 10_000_000))
    }
}
