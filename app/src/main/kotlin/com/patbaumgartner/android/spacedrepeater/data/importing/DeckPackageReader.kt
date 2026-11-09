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

    /** Cumulative uncompressed cap across ALL entries — guards against zip bombs. */
    const val MAX_TOTAL_BYTES = 200L * 1024 * 1024

    /** Maximum number of file entries accepted in one archive. */
    const val MAX_ENTRIES = 10_000

    private val SAFE_MEDIA_NAME = Regex("[A-Za-z0-9][A-Za-z0-9._-]*\\.(png|jpe?g|webp|gif)", RegexOption.IGNORE_CASE)

    fun read(
        archive: InputStream,
        mediaDir: File?,
        maxTotalBytes: Long = MAX_TOTAL_BYTES,
        maxEntries: Int = MAX_ENTRIES,
    ): DeckPackage {
        var manifest: String? = null
        val cards = sortedMapOf<String, String>()
        val media = LinkedHashSet<String>()
        var totalBytes = 0L
        var entryCount = 0

        // Local helpers capture totalBytes so every byte read is accounted for,
        // including entries that are discarded (zip-bomb guard).
        fun ZipInputStream.copyLimited(out: OutputStream, perEntryLimit: Long, name: String) {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var entryBytes = 0L
            while (true) {
                val read = read(buffer)
                if (read < 0) break
                entryBytes += read
                totalBytes += read
                if (entryBytes > perEntryLimit) throw ImportException("$name is too large")
                if (totalBytes > maxTotalBytes) {
                    throw ImportException("Archive expands beyond $maxTotalBytes bytes (zip-bomb guard)")
                }
                out.write(buffer, 0, read)
            }
        }

        fun ZipInputStream.readText(name: String): String {
            val out = ByteArrayOutputStream()
            copyLimited(out, MAX_TEXT_ENTRY_BYTES, name)
            return out.toString(Charsets.UTF_8.name())
        }

        fun ZipInputStream.copyToFile(target: File, name: String) {
            target.outputStream().use { copyLimited(it, MAX_MEDIA_ENTRY_BYTES, name) }
        }

        fun ZipInputStream.drainCounted() {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = read(buffer)
                if (read < 0) break
                totalBytes += read
                if (totalBytes > maxTotalBytes) {
                    throw ImportException("Archive expands beyond $maxTotalBytes bytes (zip-bomb guard)")
                }
            }
        }

        ZipInputStream(archive).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                // Entry names are treated as labels only: media writes go through
                // the SAFE_MEDIA_NAME allowlist plus a canonical-path containment
                // check, and deck.json / cards/*.md entries are read into memory,
                // never resolved against the file system.
                val name = entry.name.removePrefix("./")
                if (!entry.isDirectory) {
                    entryCount++
                    if (entryCount > maxEntries) {
                        throw ImportException("Archive has more than $maxEntries entries")
                    }
                    when {
                        name == "deck.json" -> manifest = zip.readText(name)
                        name.startsWith("cards/") && name.endsWith(".md") -> cards[name] = zip.readText(name)
                        mediaDir != null && name.startsWith("media/") -> {
                            val fileName = name.removePrefix("media/")
                            // Allowlist already forbids path separators ('/' and '\'), so
                            // fileName cannot traverse out of mediaDir; the resolved-path
                            // check below makes that invariant explicit for reviewers and
                            // static analysis (CodeQL java/zipslip).
                            if (SAFE_MEDIA_NAME.matches(fileName)) {
                                val target = File(mediaDir, fileName)
                                // CodeQL's recognized zipslip guard form (see
                                // java/zipslip recommendation): normalize the full
                                // path and verify it is still inside mediaDir.
                                if (!target.toPath().normalize().startsWith(mediaDir.toPath().normalize())) {
                                    throw ImportException("Unsafe media entry name: $fileName")
                                }
                                zip.copyToFile(target, name)
                                media += fileName
                            } else {
                                // Not an allowlisted image: drain and count, never write.
                                zip.drainCounted()
                            }
                        }
                        else -> zip.drainCounted()
                    }
                }
                entry = zip.nextEntry
            }
        }
        return DeckPackage(manifest, cards.values.toList(), media)
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
        // `available` only contains allowlisted flat names from the archive, so
        // the emitted path always resolves inside [mediaDir].
        if (fileName in available) {
            match.groupValues[1] + File(mediaDir, fileName).absolutePath + ")"
        } else {
            match.value
        }
    }
