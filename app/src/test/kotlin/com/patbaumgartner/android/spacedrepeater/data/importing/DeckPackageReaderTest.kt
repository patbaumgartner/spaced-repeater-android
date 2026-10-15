package com.patbaumgartner.android.spacedrepeater.data.importing

import com.patbaumgartner.android.spacedrepeater.ui.CardBlock
import com.patbaumgartner.android.spacedrepeater.ui.parseCardBlocks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DeckPackageReaderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private val card = """
        # Question

        Wie heisst diese Person?

        ![Portraet](media/card-001.png)

        ---

        # Answer

        Zoë Müller
    """.trimIndent()

    @Test
    fun readsManifestCardsAndMediaInOnePass() {
        val archive = zip(
            "deck.json" to """{"name":"Faces","cardCount":2}""".toByteArray(),
            "cards/card-002.md" to "B\n---\nb".toByteArray(),
            "cards/card-001.md" to card.toByteArray(),
            "media/card-001.png" to byteArrayOf(1, 2, 3),
        )
        val dir = tmp.newFolder("staging")

        val pkg = DeckPackageReader.read(ByteArrayInputStream(archive), dir)

        assertTrue(pkg.manifestJson!!.contains("Faces"))
        assertEquals(2, pkg.cards.size)
        assertTrue("cards are ordered by path", pkg.cards[0].startsWith("# Question"))
        assertEquals(setOf("card-001.png"), pkg.mediaFiles)
        assertEquals(3L, File(dir, "card-001.png").length())
    }

    @Test
    fun ignoresUnsafeMediaNames() {
        val archive = zip(
            "deck.json" to "{}".toByteArray(),
            "media/../../evil.png" to byteArrayOf(1),
            "media/sub/x.png" to byteArrayOf(1),
            "media/script.sh" to byteArrayOf(1),
        )
        val dir = tmp.newFolder("staging")

        val pkg = DeckPackageReader.read(ByteArrayInputStream(archive), dir)

        assertTrue(pkg.mediaFiles.isEmpty())
        assertTrue(dir.listFiles()!!.isEmpty())
        assertFalse(File(tmp.root, "evil.png").exists())
    }

    @Test
    fun manifestOnlyReadSkipsMedia() {
        val archive = zip("media/a.png" to byteArrayOf(1), "deck.json" to "{}".toByteArray())
        val pkg = DeckPackageReader.read(ByteArrayInputStream(archive), mediaDir = null)
        assertEquals("{}", pkg.manifestJson)
        assertTrue(pkg.mediaFiles.isEmpty())
    }

    @Test
    fun rewritesBundledReferencesAndRendersImageBlocks() {
        val dir = tmp.newFolder("deck-1")
        val parsed = MarkdownParser().parse(card)

        val front = rewriteMediaReferences(parsed.front, dir, setOf("card-001.png"))
        val blocks = parseCardBlocks(front)

        assertEquals(
            listOf(
                CardBlock.Text("Wie heisst diese Person?"),
                CardBlock.Image(path = File(dir, "card-001.png").absolutePath, alt = "Portraet"),
            ),
            blocks,
        )
        assertEquals(listOf(CardBlock.Text("Zoë Müller")), parseCardBlocks(parsed.back))
    }

    @Test
    fun leavesMissingMediaReferencesUntouched() {
        val text = "![x](media/missing.png)"
        assertEquals(text, rewriteMediaReferences(text, tmp.root, emptySet()))
    }

    @Test
    fun readsFaceDeckWhenProvided() {
        val path = System.getenv("FACE_DECK") ?: return
        val dir = tmp.newFolder("face")
        val pkg = File(path).inputStream().use { DeckPackageReader.read(it, dir) }
        assertEquals(70, pkg.cards.size)
        assertEquals(70, pkg.mediaFiles.size)
        for (raw in pkg.cards) {
            val parsed = MarkdownParser().parse(raw)
            val image = parseCardBlocks(rewriteMediaReferences(parsed.front, dir, pkg.mediaFiles))
                .filterIsInstance<CardBlock.Image>().single()
            assertTrue(image.path, File(image.path).isFile)
            assertTrue(parsed.back.isNotBlank())
            assertNull(parseCardBlocks(parsed.back).firstOrNull { it is CardBlock.Image })
        }
    }
}
