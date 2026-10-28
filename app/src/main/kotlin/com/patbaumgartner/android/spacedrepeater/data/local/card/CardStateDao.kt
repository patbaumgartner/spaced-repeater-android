package com.patbaumgartner.android.spacedrepeater.data.local.card

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.patbaumgartner.android.spacedrepeater.data.local.model.CardStateEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.ReviewLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardStateDao {
    @Query("SELECT * FROM card_states WHERE cardId = :cardId")
    suspend fun getById(cardId: Long): CardStateEntity?

    @Query(
        """
        SELECT card_states.* FROM card_states
        INNER JOIN cards ON cards.id = card_states.cardId
        WHERE cards.deckId = :deckId
        """,
    )
    suspend fun getByDeckId(deckId: Long): List<CardStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: CardStateEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(states: List<CardStateEntity>)

    @Query(
        """
        SELECT COUNT(*) FROM card_states
        JOIN cards ON cards.id = card_states.cardId
        WHERE cards.deckId = :deckId AND card_states.dueDate <= :now
        """,
    )
    suspend fun countDue(deckId: Long, now: Long): Int

    @Query("SELECT * FROM card_states WHERE dueDate <= :now")
    suspend fun dueStates(now: Long): List<CardStateEntity>

    @Query("SELECT MIN(dueDate) FROM card_states WHERE dueDate > :now")
    suspend fun nextDue(now: Long): Long?

    @Query(
        """
        SELECT COUNT(*) FROM card_states
        JOIN cards ON cards.id = card_states.cardId
        WHERE cards.deckId = :deckId AND card_states.dueDate <= :now
        """,
    )
    fun observeDueCount(deckId: Long, now: Long): Flow<Int>
}
