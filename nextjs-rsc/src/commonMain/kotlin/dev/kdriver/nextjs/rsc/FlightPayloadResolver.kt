package dev.kdriver.nextjs.rsc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Resolves React Server Components (RSC) Flight payloads.
 *
 * This resolver parses RSC row format, handles various tag types,
 * and resolves all references (especially $L lazy references).
 *
 * ## Usage
 *
 * ```kotlin
 * val resolver = FlightPayloadResolver()
 * resolver.parsePayloads(nextFPushes)
 * val resolved = resolver.getResolvedRoot()
 * ```
 *
 * ## Architecture
 *
 * The resolver is designed to be extensible:
 * - Add new tag handlers by implementing [TagHandler] and registering them
 * - Add new reference types by extending [ReferenceResolver]
 * - Parse custom row formats by customizing [RowParser]
 *
 * ## Main Use Case: $L References
 *
 * The primary purpose is to resolve `$L` (lazy) references which point
 * to components that should be lazily loaded. These are recursively
 * resolved to build the complete component tree.
 */
class FlightPayloadResolver(
    /**
     * Custom tag handler registry. If not provided, uses default handlers.
     */
    private val tagRegistry: TagHandlerRegistry = TagHandlerRegistry(),

    /**
     * Maximum recursion depth for reference resolution.
     * Prevents infinite loops in circular references.
     */
    private val maxDepth: Int = 100
) {

    /**
     * Map of row ID to parsed row value.
     */
    private val rows = mutableMapOf<String, RowValue>()

    /**
     * Pending text row ID waiting for data in next push.
     * Set when we encounter a text tag with length encoding but no data.
     */
    private var pendingTextRowId: String? = null

    /**
     * Parse all __next_f pushes and build the row map.
     *
     * Expected format:
     * ```json
     * [
     *   [1, "0:\"$L1\"\n"],
     *   [1, "1:I[\"path\",[]]\n"],
     *   [1, "2:[\"$\",\"div\",null,{}]\n"]
     * ]
     * ```
     *
     * Special case for multi-push text:
     * ```json
     * [
     *   [1, "d1:T7db,\n"],    // Text tag with no data after comma
     *   [1, "Actual text..."]  // Next push contains the text
     * ]
     * ```
     *
     * @param pushes JsonArray of push events from __next_f
     */
    fun parsePayloads(pushes: JsonArray) {
        for (push in pushes) {
            val arr = push.jsonArray
            if (arr.isEmpty()) continue

            val type = arr[0].jsonPrimitive.content.toIntOrNull() ?: continue

            // Type 1 = row data as string
            if (type == 1 && arr.size >= 2) {
                val payload = arr[1].jsonPrimitive.content
                parsePayload(payload)
            }
        }
    }

    /**
     * Parse a single payload string containing one or more rows.
     *
     * @param payload The payload string with newline-separated rows
     */
    private fun parsePayload(payload: String) {
        // Check if we have a pending text row from previous push
        if (pendingTextRowId != null) {
            // This payload is the text content for the pending row
            rows[pendingTextRowId!!] = RowValue.Text(payload)
            pendingTextRowId = null
            return
        }

        val parsedRows = RowParser.parseRows(payload)

        for (row in parsedRows) {
            val rowValue = parseRowValue(row)
            rows[row.id] = rowValue

            // Check if this is a text row waiting for data in next push
            if (rowValue is RowValue.Text && rowValue.value.isEmpty() && row.tag == 'T') {
                // Text tag with no data - expect data in next push
                pendingTextRowId = row.id
            }
        }
    }

    /**
     * Parse a single row into a [RowValue].
     */
    private fun parseRowValue(row: ParsedRow): RowValue {
        return when {
            // Has a tag - use tag handler
            row.tag != null -> tagRegistry.parse(row.tag, row.data)

            // No tag - assume JSON model
            else -> {
                try {
                    val json = Json.parseToJsonElement(row.data)
                    RowValue.Model(json)
                } catch (e: Exception) {
                    // Failed to parse JSON, store as unknown
                    RowValue.Unknown('?', row.data)
                }
            }
        }
    }

    /**
     * Resolve all references in an element recursively.
     *
     * This is the main method for resolving `$L` and other references.
     *
     * @param element The element to resolve
     * @return The fully resolved element
     */
    fun resolve(element: JsonElement): JsonElement {
        val resolver = ReferenceResolver(rows, maxDepth)
        return resolver.resolve(element)
    }

    /**
     * Get the fully resolved root element (row 0).
     *
     * This is typically what you want to render as it contains
     * the complete component tree with all `$L` references resolved.
     *
     * @return The resolved root element, or null if row 0 doesn't exist
     */
    fun getResolvedRoot(): JsonElement? {
        val rootValue = rows["0"] ?: return null

        // Convert root value to JSON
        val rootJson = when (rootValue) {
            is RowValue.Model -> rootValue.json
            is RowValue.Text -> kotlinx.serialization.json.JsonPrimitive(rootValue.value)
            is RowValue.Module -> kotlinx.serialization.json.JsonPrimitive(
                "[Module: ${rootValue.path}]"
            )
            is RowValue.Error -> kotlinx.serialization.json.JsonPrimitive(
                "[Error: ${rootValue.message}]"
            )
            is RowValue.Hint -> kotlinx.serialization.json.JsonPrimitive(
                "[Hint: ${rootValue.code}]"
            )
            is RowValue.DebugInfo -> rootValue.data
            is RowValue.Unknown -> kotlinx.serialization.json.JsonPrimitive(
                "[Unknown: ${rootValue.tag}]"
            )
        }

        // Resolve all references
        return resolve(rootJson)
    }

    /**
     * Get a specific row by ID (resolved).
     *
     * @param rowId The row ID to retrieve
     * @return The resolved row value, or null if not found
     */
    fun getResolvedRow(rowId: String): JsonElement? {
        val rowValue = rows[rowId] ?: return null

        val json = when (rowValue) {
            is RowValue.Model -> rowValue.json
            is RowValue.Text -> kotlinx.serialization.json.JsonPrimitive(rowValue.value)
            else -> return null
        }

        return resolve(json)
    }

    /**
     * Get all parsed rows (unresolved).
     *
     * Useful for debugging or inspecting the raw parsed data.
     *
     * @return Map of row ID to RowValue
     */
    fun getAllRows(): Map<String, RowValue> = rows.toMap()

    /**
     * Register a custom tag handler.
     *
     * This allows extending the resolver with new tag types.
     *
     * Example:
     * ```kotlin
     * class CustomTagHandler : TagHandler {
     *     override val tag: Char = 'X'
     *     override fun parse(data: String): RowValue {
     *         // Custom parsing logic
     *     }
     * }
     *
     * resolver.registerTagHandler(CustomTagHandler())
     * ```
     *
     * @param handler The tag handler to register
     */
    fun registerTagHandler(handler: TagHandler) {
        tagRegistry.register(handler)
    }

    /**
     * Clear all parsed rows.
     *
     * Useful for reusing the resolver instance.
     */
    fun clear() {
        rows.clear()
        pendingTextRowId = null
    }
}
