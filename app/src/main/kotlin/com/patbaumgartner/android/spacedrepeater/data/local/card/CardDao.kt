package com.patbaumgartner.android.spacedrepeater.data.local.card

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.patbaumgartner.android.spacedrepeater.data.local.model.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE deckId = :deckId")
    fun observeDeckCards(deckId: Long): Flow<List<CardEntity>>

    @Query(
        """
        SELECT cards.* FROM cards
        INNER JOIN card_states ON card_states.cardId = cards.id
        WHERE cards.deckId = :deckId AND card_states.dueDate <= :now
        ORDER BY
            CASE WHEN card_states.phase IN ('LEARNING', 'RELEARNING') THEN 0 ELSE 1 END,
            card_states.dueDate ASC
        """,
    )
    fun observeDueCards(deckId: Long, now: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getById(cardId: Long): CardEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(cards: List<CardEntity>): List<Long>
}
