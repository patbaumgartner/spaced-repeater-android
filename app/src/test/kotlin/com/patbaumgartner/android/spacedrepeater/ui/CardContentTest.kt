package com.patbaumgartner.android.spacedrepeater.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CardContentTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `path inside the media root is accepted`() {
        val media = tmp.newFolder("media")
        val deckDir = File(media, "42").apply { mkdirs() }
        val image = File(deckDir, "card.png").apply { writeText("x") }

        assertTrue(isPathInsideDir(media, image.absolutePath))
        assertTrue(isPathInsideDir(media, "file://" + image.absolutePath))
    }

    @Test
    fun `path outside the media root is rejected`() {
        val media = tmp.newFolder("media")
        val outside = tmp.newFile("secret.png")

        assertFalse(isPathInsideDir(media, outside.absolutePath))
        assertFalse(isPathInsideDir(media, "/etc/hosts"))
    }

    @Test
    fun `traversal escaping the media root is rejected`() {
        val media = tmp.newFolder("media")
        File(media, "42").mkdirs()

        assertFalse(isPathInsideDir(media, File(media, "42/../../outside.png").path))
        assertFalse(isPathInsideDir(media, "../outside.png"))
        assertFalse(isPathInsideDir(media, ""))
    }
}
