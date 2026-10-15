package com.patbaumgartner.android.spacedrepeater.data.local.deck

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.patbaumgartner.android.spacedrepeater.data.local.model.DeckEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.DeckWithCounts
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query(
        """
        SELECT decks.*,
               COUNT(DISTINCT cards.id) AS totalCards,
               COUNT(DISTINCT CASE WHEN card_states.dueDate <= :now THEN cards.id END) AS dueCards,
               COUNT(DISTINCT CASE WHEN card_states.lastReviewedAt IS NOT NULL THEN cards.id END) AS studiedCards
        FROM decks
        LEFT JOIN cards ON cards.deckId = decks.id
        LEFT JOIN card_states ON card_states.cardId = cards.id
        GROUP BY decks.id
        ORDER BY decks.lastStudiedAt DESC, decks.name ASC
        """,
    )
    fun observeDecksWithCounts(now: Long): Flow<List<DeckWithCounts>>

    @Query("SELECT * FROM decks WHERE id = :deckId")
    suspend fun getById(deckId: Long): DeckEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(deck: DeckEntity): Long

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun delete(deckId: Long)

    @Query("UPDATE decks SET lastStudiedAt = :now WHERE id = :deckId")
    suspend fun markStudied(deckId: Long, now: Long)

    @Transaction
    suspend fun insertFull(deck: DeckEntity) = insert(deck)
}
