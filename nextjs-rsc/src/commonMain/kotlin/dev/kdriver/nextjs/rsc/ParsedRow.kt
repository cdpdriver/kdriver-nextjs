package dev.kdriver.nextjs.rsc

/**
 * Represents a parsed RSC row with its ID, optional tag, and data.
 */
data class ParsedRow(
    val id: String,
    val tag: Char?,
    val data: String,
)
