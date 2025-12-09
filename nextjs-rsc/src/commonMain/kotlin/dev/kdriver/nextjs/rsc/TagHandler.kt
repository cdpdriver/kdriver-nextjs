package dev.kdriver.nextjs.rsc

/**
 * Interface for handling specific RSC row tags.
 */
interface TagHandler {
    /**
     * The tag character this handler processes.
     */
    val tag: Char

    /**
     * Parse the row data into a [RowValue].
     *
     * @param data The data portion of the row (after tag)
     * @return A [RowValue] representing the parsed data
     */
    fun parse(data: String): RowValue
}
