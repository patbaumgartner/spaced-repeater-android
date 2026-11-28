package com.patbaumgartner.android.spacedrepeater.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds review-phase history without discarding decks, cards, scheduling state, or review logs.
 *
 * Schema v1 did not store the phase before each review, so LEARNING/RELEARNING
 * history cannot be reconstructed faithfully. This migration is intentionally
 * lossy: the earliest row per card is NEW and every later row is REVIEW solely
 * as a non-NEW sentinel for the new-card budget. REVIEW is not an exact
 * historical phase and must not be used as scheduling or replay state.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE review_log ADD COLUMN previousPhase TEXT NOT NULL DEFAULT 'NEW'")
        // First-introduction marker only. Later REVIEW values mean non-NEW;
        // they are not claims about the card's exact historical phase.
        db.execSQL(
            """
            UPDATE review_log
            SET previousPhase = 'REVIEW'
            WHERE id NOT IN (
                SELECT MIN(id) FROM review_log GROUP BY cardId
            )
            """.trimIndent(),
        )
    }
}
