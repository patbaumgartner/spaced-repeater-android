package com.patbaumgartner.android.spacedrepeater.data.importing

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

/** Preview data for the confirm-import screen: the manifest plus the ACTUAL card count. */
data class ImportPreviewData(
    val manifest: DeckManifest,
    /** Number of card files actually present in the archive (may differ from manifest cardCount). */
    val actualCardCount: Int,
)

/**
 * Reads a deck package (ZIP) in a single pass, validates deck.json, parses
 * cards/, copies bundled media/ images to app-private storage, and persists
 * everything. Per-card parse failures are skipped and reported as
 * [ImportResult.skippedCards]; a deck with zero valid cards is rejected.
 *
 * Durability (arc42 §8.3): the deck row, all card rows and their initial
 * states are written in ONE database transaction, so a crash mid-import can
 * never leave a half-imported deck behind. Media files are moved into place
 * after the DB commit; if that move fails the deck row is removed again, and
 * a missing single image only degrades rendering (alt text), never crashes.
 */
class DeckImporter(
    private val deckRepository: DeckRepository,
    private val parser: MarkdownParser,
    private val mediaStore: DeckMediaStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Reads only the manifest and counts the card files without extracting media
     * (`mediaDir = null`). The preview shows the ACTUAL card count; the manifest's
     * declared `cardCount` is kept separately for the ADR-0006 integrity check.
     */
    suspend fun readPreview(archive: InputStream): ImportPreviewData = withContext(Dispatchers.IO) {
        val pkg = DeckPackageReader.read(archive, mediaDir = null)
        ImportPreviewData(
            manifest = parseManifest(pkg.manifestJson),
            actualCardCount = pkg.cards.size,
        )
    }

    suspend fun importArchive(archive: InputStream): ImportResult = withContext(Dispatchers.IO) {
        val staging = mediaStore.newStagingDir()
        try {
            val pkg = DeckPackageReader.read(archive, staging)
            val manifest = parseManifest(pkg.manifestJson)
            if (pkg.cards.isEmpty()) throw ImportException("Deck contains no cards")

            val parsed = pkg.cards.mapNotNull { raw -> runCatching { parser.parse(raw) }.getOrNull() }
            if (parsed.isEmpty()) throw ImportException("Deck contains no valid cards")

            val now = System.currentTimeMillis()
            // arc42 §8.3: deck + cards + initial states commit in ONE transaction.
            val deckId = deckRepository.insertDeckWithCards(
                manifest = manifest,
                cards = parsed,
                dueDate = now,
                mediaFiles = pkg.mediaFiles,
                resolveMediaDir = { id -> mediaStore.deckDir(id) },
            )
            // Filesystem move after the DB commit. If it fails, remove the deck row
            // again — never leave a deck whose bundled media is missing wholesale.
            try {
                mediaStore.commit(staging, deckId)
            } catch (e: Exception) {
                runCatching { deckRepository.delete(deckId) }
                throw ImportException("Could not store bundled media: ${e.message}", e)
            }
            ImportResult(
                deckId = deckId,
                importedCards = parsed.size,
                skippedCards = pkg.cards.size - parsed.size,
            )
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
