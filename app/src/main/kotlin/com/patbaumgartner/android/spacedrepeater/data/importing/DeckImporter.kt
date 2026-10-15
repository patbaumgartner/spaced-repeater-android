package com.patbaumgartner.android.spacedrepeater.data.importing

import com.patbaumgartner.android.spacedrepeater.data.local.model.CardEntity
import com.patbaumgartner.android.spacedrepeater.data.repository.CardRepository
import com.patbaumgartner.android.spacedrepeater.data.repository.DeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream

class ImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class ImportResult(
    val deckId: Long,
    val importedCards: Int,
    val skippedCards: Int,
)

/**
 * Reads a deck package (ZIP) in a single pass, validates deck.json, parses
 * cards/, copies bundled media/ images to app-private storage, and persists
 * everything. Per-card parse failures are skipped and reported as
 * [ImportResult.skippedCards]; a deck with zero valid cards is rejected. The
 * source archive is never mutated, and a failed import leaves no deck or media
 * behind.
 */
class DeckImporter(
    private val deckRepository: DeckRepository,
    private val cardRepository: CardRepository,
    private val parser: MarkdownParser,
    private val mediaStore: DeckMediaStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun readManifest(archive: InputStream): DeckManifest = withContext(Dispatchers.IO) {
        parseManifest(DeckPackageReader.read(archive, mediaDir = null).manifestJson)
    }

    suspend fun importArchive(archive: InputStream): ImportResult = withContext(Dispatchers.IO) {
        val staging = mediaStore.newStagingDir()
        var deckId: Long? = null
        try {
            val pkg = DeckPackageReader.read(archive, staging)
            val manifest = parseManifest(pkg.manifestJson)
            if (pkg.cards.isEmpty()) throw ImportException("Deck contains no cards")

            val parsed = pkg.cards.mapNotNull { raw -> runCatching { parser.parse(raw) }.getOrNull() }
            if (parsed.isEmpty()) throw ImportException("Deck contains no valid cards")

            val id = deckRepository.insertDeck(manifest)
            deckId = id
            val mediaDir = mediaStore.commit(staging, id)
            val now = System.currentTimeMillis()
            val entities = parsed.map {
                CardEntity(
                    deckId = id,
                    front = rewriteMediaReferences(it.front, mediaDir, pkg.mediaFiles),
                    back = rewriteMediaReferences(it.back, mediaDir, pkg.mediaFiles),
                    createdAt = now,
                )
            }
            cardRepository.insertCards(entities, dueDate = now)
            ImportResult(deckId = id, importedCards = parsed.size, skippedCards = pkg.cards.size - parsed.size)
        } catch (e: Exception) {
            deckId?.let { runCatching { deckRepository.delete(it) } }
            throw e
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun parseManifest(raw: String?): DeckManifest {
        if (raw == null) throw ImportException("Deck package is missing deck.json")
        return try {
            json.decodeFromString<DeckManifest>(raw)
        } catch (e: Exception) {
            throw ImportException("deck.json is invalid: ${e.message}", e)
        }
    }
}
