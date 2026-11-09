package com.patbaumgartner.android.spacedrepeater.data.importing

import com.patbaumgartner.android.spacedrepeater.ui.parseCardBlocks
import com.patbaumgartner.android.spacedrepeater.ui.CardBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

/**
 * End-to-end import test against the committed synthetic fixture deck
 * (`app/src/test/resources/fixture-deck/deck.zip`).
 *
 * Unlike [SampleDeckEndToEndTest] this runs in CI because the fixture is checked
 * into the repository (no private data). It validates:
 *  - manifest is readable,
 *  - the ACTUAL card file count is what the preview must show (ADR-0006 integrity),
 *  - bundled media is extracted and references resolve.
 */
class FixtureDeckEndToEndTest {

    private val fixture: File = File(
        "src/test/resources/fixture-deck/deck.zip",
    )

    @Test
    fun importsFixtureDeckWithCardsAndMedia() {
        assertTrue("fixture deck present at $fixture", fixture.isFile)

        val mediaDir = java.io.File(java.io.File(System.getProperty("java.io.tmpdir")), "fixture-media-${System.nanoTime()}")
        mediaDir.mkdirs()

        FileInputStream(fixture).use { input ->
            val pkg = DeckPackageReader.read(input, mediaDir)

            val manifestJson = requireNotNull(pkg.manifestJson) { "deck.json present" }
            assertTrue(manifestJson.contains("Fixture Deck (Synthetic)"))

            // ADR-0006 integrity check: preview shows ACTUAL card count.
            assertEquals(3, pkg.cards.size)
            assertEquals(3, pkg.mediaFiles.size)

            val parser = MarkdownParser()
            val parsed = pkg.cards.map { parser.parse(it) }
            assertEquals(3, parsed.size)
            for (card in parsed) {
                assertTrue("front keeps image ref", card.front.contains("media/"))
                assertTrue("back has answer text", card.back.isNotBlank())
            }

            // Every referenced media file was extracted and is a real PNG.
            val extracted = pkg.mediaFiles.map { File(mediaDir, it) }
            assertEquals(3, extracted.count { it.isFile })
            val signature = extracted.first().readBytes().take(4).toByteArray()
            assertTrue(
                "PNG signature present",
                signature.size == 4 && signature[0] == 0x89.toByte() && signature[1] == 'P'.code.toByte(),
            )

            // Rewritten references point at the extracted files.
            val front = rewriteMediaReferences(parsed.first().front, mediaDir, pkg.mediaFiles)
            assertTrue("reference rewritten to absolute path", front.contains(mediaDir.absolutePath))
            val referenced = front.substringAfter("(").substringBefore(")")
            assertTrue("referenced file exists", File(referenced).isFile)

            mediaDir.deleteRecursively()
        }
    }
}
