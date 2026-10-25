package com.patbaumgartner.android.spacedrepeater.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.patbaumgartner.android.spacedrepeater.data.local.card.ReviewLogDao
import com.patbaumgartner.android.spacedrepeater.data.local.model.CardEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.DeckEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.ReviewLogEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

/**
 * Instrumented DAO tests for [com.patbaumgartner.android.spacedrepeater.data.local.card.ReviewLogDao].
 * Uses an in-memory Room database to exercise real SQLite behavior, including
 * the foreign keys backing the review log. Run via `connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class ReviewLogDaoTest {

    private lateinit var db: SpacedRepeaterDatabase
    private lateinit var reviewLogDao: ReviewLogDao

    /** Millisecond timestamps pinned to midnight of three consecutive days. */
    private var yesterday: Long = 0L
    private var today: Long = 0L
    private var tomorrow: Long = 0L

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SpacedRepeaterDatabase::class.java,
        ).build()
        reviewLogDao = db.reviewLogDao()

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cal.add(Calendar.DAY_OF_YEAR, -1)
        yesterday = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        today = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        tomorrow = cal.timeInMillis
    }

    @After
    fun teardown() {
        db.close()
    }

    /** Creates a fresh deck plus one card and returns the card id. */
    private suspend fun newCard(name: String): Long {
        val deckId = db.deckDao().insert(DeckEntity(name = name, createdAt = 0L))
        return db.cardDao().insertAll(
            listOf(CardEntity(id = 0, deckId = deckId, front = "Q", back = "A", createdAt = 0L)),
        ).first()
    }

    @Test
    fun insertAndGetById() = runBlocking {
        val cardId = newCard("InsertTest")

        val log = ReviewLogEntity(
            cardId = cardId,
            rating = 3,
            interval = 1,
            easeFactor = 2.5f,
            reviewedAt = today,
        )
        val id = reviewLogDao.insert(log)

        val retrieved = reviewLogDao.getById(id)
        assertEquals(log.cardId, retrieved?.cardId)
        assertEquals(log.rating, retrieved?.rating)
    }

    @Test
    fun getByIdReturnsNullForNonexistent() = runBlocking {
        assertNull(reviewLogDao.getById(9999L))
    }

    @Test
    fun countToday_excludesYesterdayAndTomorrow() = runBlocking {
        val cardId = newCard("CountTest")
        fun log(rating: Int, at: Long) = ReviewLogEntity(
            cardId = cardId,
            rating = rating,
            interval = 1,
            easeFactor = 2.5f,
            reviewedAt = at,
        )

        reviewLogDao.insert(log(1, yesterday))
        reviewLogDao.insert(log(3, today))
        reviewLogDao.insert(log(4, today))
        reviewLogDao.insert(log(3, tomorrow))

        assertEquals(2, reviewLogDao.countToday(today, tomorrow))
    }

    @Test
    fun today_returnsOnlyTodayEntries() = runBlocking {
        val cardId = newCard("TodayTest")
        fun log(rating: Int, at: Long) = ReviewLogEntity(
            cardId = cardId,
            rating = rating,
            interval = 1,
            easeFactor = 2.5f,
            reviewedAt = at,
        )

        reviewLogDao.insert(log(1, yesterday))
        reviewLogDao.insert(log(3, today))
        reviewLogDao.insert(log(4, today))

        assertEquals(2, reviewLogDao.today(today, tomorrow).size)
    }

    @Test
    fun latest_returnsMostRecentEntry() = runBlocking {
        val cardId = newCard("LatestTest")
        fun log(rating: Int, at: Long) = ReviewLogEntity(
            cardId = cardId,
            rating = rating,
            interval = 1,
            easeFactor = 2.5f,
            reviewedAt = at,
        )

        reviewLogDao.insert(log(1, yesterday))
        reviewLogDao.insert(log(3, today))
        reviewLogDao.insert(log(4, today)) // Same day, inserted last.

        assertEquals(4, reviewLogDao.latest()?.rating)
    }

    @Test
    fun deleteRemovesEntry() = runBlocking {
        val cardId = newCard("DeleteTest")
        val id = reviewLogDao.insert(
            ReviewLogEntity(cardId = cardId, rating = 3, interval = 1, easeFactor = 2.5f, reviewedAt = today),
        )

        reviewLogDao.delete(id)

        assertNull(reviewLogDao.getById(id))
    }
}
