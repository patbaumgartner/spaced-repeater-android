package com.patbaumgartner.android.spacedrepeater.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SpacedRepeaterDatabase::class.java,
    )

    @Test
    fun migration1To2PreservesDeckCardsStatesAndReviewLogs() {
        migrateV1LearningHistoryWithData().use { db ->
            assertEquals(1L, db.queryLong("SELECT COUNT(*) FROM decks"))
            assertEquals("Offline deck", db.queryString("SELECT name FROM decks WHERE id = 1"))
            assertEquals("Question:Answer", db.queryString("SELECT front || ':' || back FROM cards WHERE id = 7"))
            assertEquals(1L, db.queryLong("SELECT repetitions FROM card_states WHERE cardId = 7"))
            assertEquals(123_400L, db.queryLong("SELECT dueDate FROM card_states WHERE cardId = 7"))
            assertEquals(2L, db.queryLong("SELECT COUNT(*) FROM review_log"))
            assertEquals(1_250L, db.queryLong("SELECT timeTakenMs FROM review_log WHERE id = 12"))
        }
    }

    @Test
    fun migration1To2UsesNonNewSentinelForLaterLearningHistory() {
        migrateV1LearningHistoryWithData().use { db ->
            assertEquals(0L, db.queryLong("SELECT rating FROM review_log WHERE id = 11"))
            assertEquals(1L, db.queryLong("SELECT rating FROM review_log WHERE id = 12"))
            assertEquals("NEW", db.queryString("SELECT previousPhase FROM review_log WHERE id = 11"))
            // AGAIN then HARD means the second review's actual prior phase was
            // LEARNING. REVIEW is only a non-NEW budget sentinel, not replay state.
            assertEquals("REVIEW", db.queryString("SELECT previousPhase FROM review_log WHERE id = 12"))
            assertEquals("LEARNING", db.queryString("SELECT phase FROM card_states WHERE cardId = 7"))
        }
    }

    private fun migrateV1LearningHistoryWithData(): SupportSQLiteDatabase {
        val name = "migration-test"
        helper.createDatabase(name, 1).use { db ->
            db.execSQL(
                "INSERT INTO decks (id, name, description, createdAt, cardCount, lastStudiedAt) " +
                    "VALUES (1, 'Offline deck', 'preserved', 100, 1, 200)",
            )
            db.execSQL(
                "INSERT INTO cards (id, deckId, front, back, tags, createdAt) " +
                    "VALUES (7, 1, 'Question', 'Answer', '', 300)",
            )
            db.execSQL(
                "INSERT INTO card_states " +
                    "(cardId, repetitions, easeFactor, interval, dueDate, lastReviewedAt, phase) " +
                    "VALUES (7, 1, 2.15, 10, 123400, 1200, 'LEARNING')",
            )
            db.execSQL(
                "INSERT INTO review_log " +
                    "(id, cardId, reviewedAt, rating, interval, easeFactor, timeTakenMs) " +
                    "VALUES (11, 7, 1000, 0, 1, 2.3, 750)",
            )
            db.execSQL(
                "INSERT INTO review_log " +
                    "(id, cardId, reviewedAt, rating, interval, easeFactor, timeTakenMs) " +
                    "VALUES (12, 7, 1200, 1, 10, 2.15, 1250)",
            )
        }
        return helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2)
    }

    private fun SupportSQLiteDatabase.queryLong(sql: String): Long = query(sql).use { cursor ->
        assertTrue(cursor.moveToFirst())
        cursor.getLong(0)
    }

    private fun SupportSQLiteDatabase.queryString(sql: String): String = query(sql).use { cursor ->
        assertTrue(cursor.moveToFirst())
        cursor.getString(0)
    }
}
