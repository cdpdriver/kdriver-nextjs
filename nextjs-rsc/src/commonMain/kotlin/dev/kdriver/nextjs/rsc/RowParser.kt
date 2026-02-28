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
     * Handles T rows with length-encoded text (format: `id:T<hexLen>,<text>`) which
     * may contain literal newlines in their content. The hex length is used to read
     * the exact byte count of text, skipping past any embedded newlines, rather than
     * stopping at the first newline like a naive line-split would.
     *
     * @param payload The complete payload string with newline-separated rows
     * @return List of successfully parsed rows
     */
    fun parseRows(payload: String): List<ParsedRow> {
        val results = mutableListOf<ParsedRow>()
        var pos = 0

        while (pos < payload.length) {
            // Skip blank lines
            if (payload[pos] == '\n') {
                pos++
                continue
            }

            val lineEnd = payload.indexOf('\n', pos)
            val line = if (lineEnd == -1) payload.substring(pos) else payload.substring(pos, lineEnd)

            if (line.isBlank()) {
                pos = if (lineEnd == -1) payload.length else lineEnd + 1
                continue
            }

            // Check if this is a length-encoded T row (may span multiple lines)
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                val rest = line.substring(colonIdx + 1)
                if (rest.isNotEmpty() && rest[0] == 'T') {
                    val afterT = rest.substring(1)
                    val commaIdx = afterT.indexOf(',')
                    if (commaIdx > 0) {
                        val potentialLength = afterT.substring(0, commaIdx)
                        if (potentialLength.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                            val textByteLength = potentialLength.toLong(16).toInt()
                            val textStart = pos + colonIdx + 1 + 1 + commaIdx + 1 // skip id + ':' + 'T' + hexLen + ','
                            val remaining = payload.substring(textStart)
                            val remainingBytes = remaining.toByteArray(Charsets.UTF_8)
                            if (textByteLength <= remainingBytes.size) {
                                val text = String(remainingBytes, 0, textByteLength, Charsets.UTF_8)
                                results.add(ParsedRow(line.substring(0, colonIdx), 'T', "$potentialLength,$text"))
                                pos = textStart + text.length
                                if (pos < payload.length && payload[pos] == '\n') pos++
                                continue
                            }
                        }
                    }
                }
            }

            // Regular row — parse the current line as-is
            parseRow(line)?.let { results.add(it) }
            pos = if (lineEnd == -1) payload.length else lineEnd + 1
        }

        return results
    }
}
