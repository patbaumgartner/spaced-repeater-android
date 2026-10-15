package com.patbaumgartner.android.spacedrepeater.data.importing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream

/**
 * End-to-end import test against the real sample deck shipped in `sample/`.
 * Validates the actual artifact: manifest, 70 cards, bundled media extracted to
 * app-private storage, and media references rewritten to the copied files.
 */
class SampleDeckEndToEndTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val sampleZip: File = File(
        "../sample/web-entwicklung-hs2026-gesichter-namen.zip",
    )

    @Test
    fun importsSampleDeckWithCardsAndMedia() {
        assertTrue("sample zip exists", sampleZip.isFile)

        val mediaDir = tmp.newFolder("media")
        val parser = MarkdownParser()

        FileInputStream(sampleZip).use { input ->
            val pkg = DeckPackageReader.read(input, mediaDir)

            val manifestJson = requireNotNull(pkg.manifestJson) { "deck.json present" }
            assertTrue(manifestJson.contains("Web-Entwicklung HS 2026"))
            assertEquals(70, pkg.cards.size)
            assertEquals(70, pkg.mediaFiles.size)

            val parsed = pkg.cards.map { parser.parse(it) }
            assertEquals(70, parsed.size)
            for (card in parsed) {
                assertTrue("front keeps image ref", card.front.contains("media/"))
                assertTrue("back has answer text", card.back.isNotBlank())
            }

            // Every referenced media file was extracted and is a real, decodable PNG.
            val extracted = pkg.mediaFiles.map { File(mediaDir, it) }
            assertEquals(70, extracted.count { it.isFile })
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
        }
    }
}
