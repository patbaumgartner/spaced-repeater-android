package com.patbaumgartner.android.spacedrepeater.data.repository

import com.patbaumgartner.android.spacedrepeater.data.importing.DeckManifest
import com.patbaumgartner.android.spacedrepeater.data.importing.DeckMediaStore
import com.patbaumgartner.android.spacedrepeater.data.importing.ParsedCard
import com.patbaumgartner.android.spacedrepeater.data.importing.rewriteMediaReferences
import com.patbaumgartner.android.spacedrepeater.data.local.DbTransaction
import com.patbaumgartner.android.spacedrepeater.data.local.card.CardDao
import com.patbaumgartner.android.spacedrepeater.data.local.card.CardStateDao
import com.patbaumgartner.android.spacedrepeater.data.local.deck.DeckDao
import com.patbaumgartner.android.spacedrepeater.data.local.model.CardEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.CardStateEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.DeckEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.DeckWithCounts
import com.patbaumgartner.android.spacedrepeater.data.local.model.toDomain
import com.patbaumgartner.android.spacedrepeater.domain.model.Deck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

data class HomeDeck(
    val deck: Deck,
    val dueCount: Int,
    val studiedCount: Int,
)

class DeckRepository(
    private val deckDao: DeckDao,
    private val cardDao: CardDao,
    private val cardStateDao: CardStateDao,
    private val dbTransaction: DbTransaction,
    private val mediaStore: DeckMediaStore,
    private val clock: () -> Long = { System.currentTimeMillis() },
    /** How often the due badges are recomputed so they stay correct while the app is open. */
    private val dueRefreshPeriodMs: Long = DUE_REFRESH_PERIOD_MS,
) {
    /**
     * Deck list with due/studied counts. The `now` used for the due badge is
     * re-evaluated on every refresh tick instead of being captured once, so
     * badges do not go stale while the app stays open (UC-02 reactivity).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDecks(): Flow<List<HomeDeck>> =
        dueTicker(dueRefreshPeriodMs).flatMapLatest {
            deckDao.observeDecksWithCounts(clock()).map { rows -> rows.map(::toHomeDeck) }
        }

    /**
     * Inserts the deck, all cards and their initial NEW states in ONE transaction
     * (arc42 §8.3: "Import | One transaction: deck + all cards + initial states"),
     * so a crash mid-import can never leave a half-imported deck behind.
     *
     * [resolveMediaDir] maps the generated deck id to the final media directory,
     * which lets the card media references be rewritten inside the same transaction.
     */
    suspend fun insertDeckWithCards(
        manifest: DeckManifest,
        cards: List<ParsedCard>,
        dueDate: Long,
        mediaFiles: Set<String>,
        resolveMediaDir: (deckId: Long) -> File,
    ): Long {
        var deckId = 0L
        dbTransaction.run {
            deckId = deckDao.insert(
                DeckEntity(
                    name = manifest.name,
                    description = manifest.description,
                    createdAt = clock(),
                ),
            )
            val mediaDir = resolveMediaDir(deckId)
            val entities = cards.map {
                CardEntity(
                    deckId = deckId,
                    front = rewriteMediaReferences(it.front, mediaDir, mediaFiles),
                    back = rewriteMediaReferences(it.back, mediaDir, mediaFiles),
                    createdAt = dueDate,
                )
            }
            val cardIds = cardDao.insertAll(entities)
            cardStateDao.insertAll(cardIds.map { CardStateEntity(cardId = it, dueDate = dueDate) })
        }
        return deckId
    }

    suspend fun delete(deckId: Long) {
        deckDao.delete(deckId)
        withContext(Dispatchers.IO) { mediaStore.delete(deckId) }
    }

    suspend fun markStudied(deckId: Long, now: Instant = Instant.now()) =
        deckDao.markStudied(deckId, now.toEpochMilli())

    companion object {
        const val DUE_REFRESH_PERIOD_MS = 30_000L
    }
}

private fun toHomeDeck(row: DeckWithCounts): HomeDeck = HomeDeck(
    deck = row.deck.toDomain().copy(cardCount = row.totalCards),
    dueCount = row.dueCards,
    studiedCount = row.studiedCards,
)

private fun dueTicker(periodMs: Long): Flow<Unit> = flow {
    while (currentCoroutineContext().isActive) {
        emit(Unit)
        delay(periodMs)
    }
}
