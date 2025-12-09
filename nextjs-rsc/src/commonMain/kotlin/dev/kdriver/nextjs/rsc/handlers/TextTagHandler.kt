package dev.kdriver.nextjs.rsc.handlers

import dev.kdriver.nextjs.rsc.RowValue
import dev.kdriver.nextjs.rsc.TagHandler

/**
 * Handler for text chunks (tag: T).
 *
 * Format: plain text or with length encoding
 * Examples:
 * - T:Simple text → "Simple text"
 * - T10,Hello World → "Hello World" (10 = 0x10 = 16 bytes length)
 * - T7db,<text> → <text> (7db = 0x7db = 2011 bytes length)
 */
class TextTagHandler : TagHandler {
    override val tag: Char = 'T'

    override fun parse(data: String): RowValue {
        // Check if data starts with length encoding: <hexlength>,<text>
        val commaIndex = data.indexOf(',')

        if (commaIndex > 0) {
            // Try to parse the prefix as hex length
            val potentialLength = data.substring(0, commaIndex)

            // Check if it's a valid hex number
            if (potentialLength.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                // It's length-encoded text
                // Extract the actual text after the comma
                val text = if (commaIndex + 1 < data.length) {
                    data.substring(commaIndex + 1)
                } else {
                    ""
                }
                return RowValue.Text(text)
            }
        }

        // No length encoding, treat entire data as text
        return RowValue.Text(data)
    }
}
