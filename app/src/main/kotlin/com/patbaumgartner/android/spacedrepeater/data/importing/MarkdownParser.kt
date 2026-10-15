package com.patbaumgartner.android.spacedrepeater.data.importing

data class ParsedCard(
    val front: String,
    val back: String,
    val tags: List<String> = emptyList(),
)

/**
 * Splits a markdown card into front/back. The grammar is deliberately small and
 * documented (ADR-0006): a `---` separator at column zero, optionally wrapped in
 * `# Question` / `# Answer` headings.
 */
class MarkdownParser {
    fun parse(raw: String): ParsedCard {
        val normalized = raw.replace("\r\n", "\n").trim()
        require(normalized.isNotBlank()) { "Card is empty" }
        val parts = normalized.split(Regex("(?m)^---\\s*$"))
        return when (parts.size) {
            1 -> ParsedCard(front = stripHeading(normalized, "Question"), back = "")
            else -> {
                val frontRaw = parts[0]
                val backRaw = parts.drop(1).joinToString("\n")
                ParsedCard(
                    front = stripHeading(frontRaw, "Question"),
                    back = stripHeading(backRaw, "Answer"),
                )
            }
        }
    }

    private fun stripHeading(text: String, heading: String): String =
        text.replace(Regex("^#\\s*$heading\\s*$", RegexOption.MULTILINE), "")
            .trim()
}
