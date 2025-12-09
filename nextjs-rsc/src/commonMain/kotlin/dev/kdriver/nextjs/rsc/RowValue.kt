package dev.kdriver.nextjs.rsc

import kotlinx.serialization.json.JsonElement

/**
 * Represents the different types of row values that can be stored.
 */
sealed class RowValue {
    /**
     * JSON model data (React elements, objects, arrays).
     */
    data class Model(val json: JsonElement) : RowValue()

    /**
     * Module/import metadata.
     */
    data class Module(
        val path: String,
        val exports: List<String>,
        val name: String,
    ) : RowValue()

    /**
     * Hint for preload/prefetch (fonts, stylesheets, etc.).
     */
    data class Hint(
        val code: String,
        val model: JsonElement,
    ) : RowValue()

    /**
     * Error object.
     */
    data class Error(
        val message: String,
        val stack: String?,
        val digest: String?,
    ) : RowValue()

    /**
     * Large text string.
     */
    data class Text(val value: String) : RowValue()

    /**
     * Debug information.
     */
    data class DebugInfo(val data: JsonElement) : RowValue()

    /**
     * Unknown or unsupported tag type.
     */
    data class Unknown(val tag: Char, val rawData: String) : RowValue()
}
