package com.patbaumgartner.android.spacedrepeater.data.importing

import java.io.File

/**
 * Owns the app-private directory tree that holds images bundled with imported
 * deck packages (ADR-0006). Each deck gets its own folder so deleting a deck
 * removes its media as well.
 */
class DeckMediaStore(private val root: File) {

    fun deckDir(deckId: Long): File = File(root, deckId.toString())

    fun newStagingDir(): File =
        File(root, ".staging-${System.nanoTime()}").apply { mkdirs() }

    /** Moves a staging directory into place for [deckId]; returns the final directory. */
    fun commit(staging: File, deckId: Long): File {
        val target = deckDir(deckId)
        target.deleteRecursively()
        if (!staging.renameTo(target)) {
            staging.copyRecursively(target, overwrite = true)
            staging.deleteRecursively()
        }
        return target
    }

    fun delete(deckId: Long) {
        deckDir(deckId).deleteRecursively()
    }
}
