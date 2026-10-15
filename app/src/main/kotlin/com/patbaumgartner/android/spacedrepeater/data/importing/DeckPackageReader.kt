package com.patbaumgartner.android.spacedrepeater.data.importing

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream

/** Raw contents of a deck package after a single streaming pass over the ZIP. */
class DeckPackage(
    val manifestJson: String?,
    /** Card markdown sources, ordered by their path inside the archive. */
    val cards: List<String>,
    /** File names (without `media/`) that were extracted to the staging directory. */
    val mediaFiles: Set<String>,
)

/**
 * Reads a deck package in one pass. Media entries are streamed straight to
 * [mediaDir] (never held in memory); only flat, whitelisted image names are
 * accepted so crafted entry names cannot escape the target directory.
 */
object DeckPackageReader {
    private const val MAX_TEXT_ENTRY_BYTES = 1L * 1024 * 1024
    private const val MAX_MEDIA_ENTRY_BYTES = 20L * 1024 * 1024
    private val SAFE_MEDIA_NAME = Regex("[A-Za-z0-9][A-Za-z0-9._-]*\\.(png|jpe?g|webp|gif)", RegexOption.IGNORE_CASE)

    fun read(archive: InputStream, mediaDir: File?): DeckPackage {
        var manifest: String? = null
        val cards = sortedMapOf<String, String>()
        val media = LinkedHashSet<String>()
        ZipInputStream(archive).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name.removePrefix("./")
                if (!entry.isDirectory) {
                    when {
                        name == "deck.json" -> manifest = zip.readText(name)
                        name.startsWith("cards/") && name.endsWith(".md") -> cards[name] = zip.readText(name)
                        mediaDir != null && name.startsWith("media/") -> {
                            val fileName = name.removePrefix("media/")
                            if (SAFE_MEDIA_NAME.matches(fileName)) {
                                zip.copyLimited(File(mediaDir, fileName), name)
                                media += fileName
                            }
                        }
                    }
                }
                entry = zip.nextEntry
            }
        }
        return DeckPackage(manifest, cards.values.toList(), media)
    }

    private fun ZipInputStream.readText(name: String): String {
        val out = ByteArrayOutputStream()
        copyLimited(out, MAX_TEXT_ENTRY_BYTES, name)
        return out.toString(Charsets.UTF_8.name())
    }

    private fun ZipInputStream.copyLimited(target: File, name: String) {
        target.outputStream().use { copyLimited(it, MAX_MEDIA_ENTRY_BYTES, name) }
    }

    private fun ZipInputStream.copyLimited(out: OutputStream, limit: Long, name: String) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) throw ImportException("$name is too large")
            out.write(buffer, 0, read)
        }
    }
}

private val MEDIA_REFERENCE = Regex("""(!\[[^\]]*]\()\s*(?:\./)?media/([^)\s]+)\s*\)""")

/**
 * Rewrites `![alt](media/file.png)` references to the absolute path of the
 * imported copy in [mediaDir]. References to files that were not bundled are
 * left untouched so the renderer can fall back to the alt text.
 */
fun rewriteMediaReferences(markdown: String, mediaDir: File, available: Set<String>): String =
    MEDIA_REFERENCE.replace(markdown) { match ->
        val fileName = match.groupValues[2]
        if (fileName in available) {
            match.groupValues[1] + File(mediaDir, fileName).absolutePath + ")"
        } else {
            match.value
        }
    }
