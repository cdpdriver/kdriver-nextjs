package dev.kdriver.nextjs.rsc

/**
 * Parses RSC row format: `id:tag:data` or `id:data` (no tag).
 *
 * Examples:
 * - `0:"$L1"` → ParsedRow(id="0", tag=null, data="\"$L1\"")
 * - `1:I["path",["exports"],"name"]` → ParsedRow(id="1", tag='I', data="[\"path\",...]")
 * - `2:H:{"key":"value"}` → ParsedRow(id="2", tag='H', data="{\"key\":...}")
 */
object RowParser {

    /**
     * Parses a single row line into its components.
     *
     * @param line The row line to parse
     * @return A [ParsedRow] with id, tag (if present), and data, or null if invalid
     */
    fun parseRow(line: String): ParsedRow? {
        if (line.isBlank()) return null

        // Find first colon (separates ID from rest)
        val firstColonIndex = line.indexOf(':')
        if (firstColonIndex == -1) return null

        val id = line.substring(0, firstColonIndex)
        val rest = line.substring(firstColonIndex + 1)

        // Check if there's a tag (single letter followed by colon or data)
        val (tag, data) = extractTagAndData(rest)

        return ParsedRow(id, tag, data)
    }

    /**
     * Extracts the tag (if present) and data from the rest of the row.
     *
     * Format can be:
     * - `I:data` → tag='I', data="data"
     * - `I[...]` → tag='I', data="[...]"
     * - `data` → tag=null, data="data"
     */
    private fun extractTagAndData(rest: String): Pair<Char?, String> {
        if (rest.isEmpty()) return null to ""

        // Check if first character is an UPPERCASE letter (potential tag)
        val firstChar = rest[0]
        if (!firstChar.isUpperCase()) {
            // No tag, entire rest is data
            return null to rest
        }

        // Check if it's a tag by looking at what follows
        if (rest.length == 1) {
            // Just a letter, treat as tag with empty data
            return firstChar to ""
        }

        val secondChar = rest[1]
        return when {
            // Pattern: `T:data` → tag with colon separator
            secondChar == ':' -> firstChar to rest.substring(2)

            // Pattern: `Testing` → lowercase after uppercase means it's a word, not a tag
            secondChar.isLowerCase() -> null to rest

            // Pattern: `I[...]` or `TText` (uppercase) → tag directly followed by data
            // Tags are single uppercase letters followed by non-lowercase chars
            else -> firstChar to rest.substring(1)
        }
    }

    /**
     * Parses multiple row lines from a payload.
     *
     * @param payload The complete payload string with newline-separated rows
     * @return List of successfully parsed rows
     */
    fun parseRows(payload: String): List<ParsedRow> {
        return payload.split('\n')
            .mapNotNull { parseRow(it) }
    }
}
