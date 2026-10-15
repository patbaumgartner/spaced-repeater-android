package com.patbaumgartner.android.spacedrepeater.data.repository

import com.patbaumgartner.android.spacedrepeater.data.local.card.CardDao
import com.patbaumgartner.android.spacedrepeater.data.local.card.CardStateDao
import com.patbaumgartner.android.spacedrepeater.data.local.card.ReviewLogDao
import com.patbaumgartner.android.spacedrepeater.data.local.deck.DeckDao
import com.patbaumgartner.android.spacedrepeater.data.local.model.DeckEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.DeckWithCounts
import com.patbaumgartner.android.spacedrepeater.data.local.model.toDomain
import com.patbaumgartner.android.spacedrepeater.domain.model.Card
import com.patbaumgartner.android.spacedrepeater.domain.model.CardPhase
import com.patbaumgartner.android.spacedrepeater.domain.model.CardState
import com.patbaumgartner.android.spacedrepeater.domain.model.Deck
import com.patbaumgartner.android.spacedrepeater.domain.model.ReviewRating
import com.patbaumgartner.android.spacedrepeater.domain.model.ReviewLog
import com.patbaumgartner.android.spacedrepeater.data.importing.DeckMediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HomeDeck(
    val deck: Deck,
    val dueCount: Int,
    val studiedCount: Int,
)

class DeckRepository(
    private val deckDao: DeckDao,
    private val mediaStore: DeckMediaStore,
) {
    fun observeDecks(): Flow<List<HomeDeck>> {
        val now = System.currentTimeMillis()
        return deckDao.observeDecksWithCounts(now).map { rows ->
            rows.map { row ->
                HomeDeck(
                    deck = row.deck.toDomain().copy(cardCount = row.totalCards),
                    dueCount = row.dueCards,
                    studiedCount = row.studiedCards,
                )
            }
        }
    }

    suspend fun insertDeck(manifest: com.patbaumgartner.android.spacedrepeater.data.importing.DeckManifest): Long =
        deckDao.insert(
            DeckEntity(
                name = manifest.name,
                description = manifest.description,
                createdAt = System.currentTimeMillis(),
            ),
        )

    suspend fun delete(deckId: Long) {
        deckDao.delete(deckId)
        withContext(Dispatchers.IO) { mediaStore.delete(deckId) }
    }

    suspend fun markStudied(deckId: Long, now: Instant = Instant.now()) =
        deckDao.markStudied(deckId, now.toEpochMilli())
}
