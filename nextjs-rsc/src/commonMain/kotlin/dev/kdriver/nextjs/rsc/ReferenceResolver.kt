package dev.kdriver.nextjs.rsc

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Resolves RSC references in JSON elements.
 *
 * References are strings starting with `$` followed by a type indicator:
 * - `$Lxx` - Lazy reference (main use case!)
 * - `$@xx` - Promise reference
 * - `$Fxx` - Server function reference
 * - `$Qxx` - Map reference
 * - `$Wxx` - Set reference
 * - `$Bxx` - Blob reference
 * - `$Kxx` - FormData reference
 * - `$Zxx` - Error reference
 * - `$ixx` - Iterator reference
 * - `$xx` - Direct reference (hex digits only)
 * - `$$` - Escaped dollar sign
 * - Special literals: `$undefined`, `$Infinity`, `$-Infinity`, `$NaN`, `$-0`
 * - Typed values: `$D...` (Date), `$n...` (BigInt), `$S...` (Symbol)
 */
class ReferenceResolver(
    private val rows: Map<String, RowValue>,
    private val maxDepth: Int = 100
) {

    /**
     * Resolve all references in an element recursively.
     *
     * @param element The element to resolve
     * @param depth Current recursion depth (for cycle detection)
     * @return The resolved element
     */
    fun resolve(element: JsonElement, depth: Int = 0): JsonElement {
        // Prevent infinite recursion
        if (depth > maxDepth) {
            return JsonPrimitive("[Max depth exceeded]")
        }

        return when (element) {
            is JsonPrimitive -> resolveReference(element, depth)
            is JsonArray -> JsonArray(element.map { resolve(it, depth + 1) })
            is JsonObject -> JsonObject(element.mapValues { resolve(it.value, depth + 1) })
            else -> element
        }
    }

    /**
     * Resolve a primitive value that might be a reference.
     */
    private fun resolveReference(primitive: JsonPrimitive, depth: Int): JsonElement {
        if (!primitive.isString) return primitive

        val value = primitive.content
        if (!value.startsWith("$")) return primitive

        // Handle two-character prefixes first
        if (value.length >= 2) {
            when (value.substring(0, 2)) {
                // Escaped dollar sign
                "$$" -> return JsonPrimitive(value.substring(1))

                // Lazy reference - MAIN USE CASE!
                "\$L" -> return resolveLazyReference(value.substring(2), depth)

                // Promise reference
                "\$@" -> return resolvePromiseReference(value.substring(2), depth)

                // Server function reference
                "\$F" -> return resolveServerFunctionReference(value.substring(2), depth)

                // Map reference
                "\$Q" -> return resolveMapReference(value.substring(2), depth)

                // Set reference
                "\$W" -> return resolveSetReference(value.substring(2), depth)

                // Blob reference
                "\$B" -> return resolveBlobReference(value.substring(2), depth)

                // FormData reference
                "\$K" -> return resolveFormDataReference(value.substring(2), depth)

                // Error reference
                "\$Z" -> return resolveErrorReference(value.substring(2), depth)

                // Iterator reference
                "\$i" -> return resolveIteratorReference(value.substring(2), depth)

                // Date
                "\$D" -> return resolveDateReference(value.substring(2))

                // BigInt
                "\$n" -> return resolveBigIntReference(value.substring(2))

                // Symbol
                "\$S" -> return resolveSymbolReference(value.substring(2))
            }
        }

        // Handle special literals
        return when (value) {
            "\$undefined" -> JsonNull
            "\$Infinity" -> JsonPrimitive(Double.POSITIVE_INFINITY)
            "\$-Infinity" -> JsonPrimitive(Double.NEGATIVE_INFINITY)
            "\$NaN" -> JsonPrimitive(Double.NaN)
            "\$-0" -> JsonPrimitive(-0.0)
            else -> {
                // Check if it's a direct hex reference: $xx
                if (value.matches(Regex("^\\$[0-9a-fA-F]+$"))) {
                    resolveDirectReference(value.substring(1), depth)
                } else {
                    // Unknown reference type, keep as-is
                    primitive
                }
            }
        }
    }

    /**
     * Resolve a lazy reference ($Lxx) - MAIN USE CASE!
     *
     * Lazy references are the most common type and point to components
     * that should be lazily loaded/rendered.
     */
    private fun resolveLazyReference(rowId: String, depth: Int): JsonElement {
        val rowValue = rows[rowId]
        if (rowValue == null) {
            // Row not found, return placeholder
            return JsonPrimitive("\$L$rowId [not found]")
        }

        // Convert row value to JSON and resolve recursively
        val jsonElement = rowValueToJson(rowValue)
        return resolve(jsonElement, depth + 1)
    }

    /**
     * Resolve a promise reference ($@xx).
     */
    private fun resolvePromiseReference(rowId: String, depth: Int): JsonElement {
        val rowValue = rows[rowId]
        if (rowValue == null) {
            return JsonPrimitive("\$@$rowId [not found]")
        }

        val jsonElement = rowValueToJson(rowValue)
        return resolve(jsonElement, depth + 1)
    }

    /**
     * Resolve a server function reference ($Fxx).
     *
     * Server functions are usually kept as-is or marked as functions.
     */
    private fun resolveServerFunctionReference(ref: String, depth: Int): JsonElement {
        // Parse ref which can be "rowId" or "rowId:path:to:field"
        val parts = ref.split(':')
        val rowId = parts[0]

        val rowValue = rows[rowId]
        if (rowValue == null) {
            return JsonPrimitive("[Server Function: \$F$ref]")
        }

        // If there's a path, we'd need to traverse it
        // For now, just return a marker
        return JsonPrimitive("[Server Function: \$F$ref]")
    }

    /**
     * Resolve a Map reference ($Qxx).
     */
    private fun resolveMapReference(rowId: String, depth: Int): JsonElement {
        val rowValue = rows[rowId]
        if (rowValue == null) {
            return JsonPrimitive("[Map: \$Q$rowId not found]")
        }

        val jsonElement = rowValueToJson(rowValue)
        return resolve(jsonElement, depth + 1)
    }

    /**
     * Resolve a Set reference ($Wxx).
     */
    private fun resolveSetReference(rowId: String, depth: Int): JsonElement {
        val rowValue = rows[rowId]
        if (rowValue == null) {
            return JsonPrimitive("[Set: \$W$rowId not found]")
        }

        val jsonElement = rowValueToJson(rowValue)
        return resolve(jsonElement, depth + 1)
    }

    /**
     * Resolve a Blob reference ($Bxx).
     */
    private fun resolveBlobReference(rowId: String, depth: Int): JsonElement {
        return JsonPrimitive("[Blob: \$B$rowId]")
    }

    /**
     * Resolve a FormData reference ($Kxx).
     */
    private fun resolveFormDataReference(rowId: String, depth: Int): JsonElement {
        return JsonPrimitive("[FormData: \$K$rowId]")
    }

    /**
     * Resolve an Error reference ($Zxx).
     */
    private fun resolveErrorReference(rowId: String, depth: Int): JsonElement {
        val rowValue = rows[rowId]
        if (rowValue == null) {
            return JsonPrimitive("[Error: \$Z$rowId not found]")
        }

        return when (rowValue) {
            is RowValue.Error -> JsonPrimitive("[Error: ${rowValue.message}]")
            else -> {
                val jsonElement = rowValueToJson(rowValue)
                resolve(jsonElement, depth + 1)
            }
        }
    }

    /**
     * Resolve an Iterator reference ($ixx).
     */
    private fun resolveIteratorReference(rowId: String, depth: Int): JsonElement {
        return JsonPrimitive("[Iterator: \$i$rowId]")
    }

    /**
     * Resolve a Date reference ($D...).
     */
    private fun resolveDateReference(isoString: String): JsonElement {
        // Store as ISO string in JSON
        return JsonPrimitive("[Date: $isoString]")
    }

    /**
     * Resolve a BigInt reference ($n...).
     */
    private fun resolveBigIntReference(digits: String): JsonElement {
        // Store as string since JSON doesn't support BigInt
        return JsonPrimitive("[BigInt: $digits]")
    }

    /**
     * Resolve a Symbol reference ($S...).
     */
    private fun resolveSymbolReference(description: String): JsonElement {
        return JsonPrimitive("[Symbol: $description]")
    }

    /**
     * Resolve a direct reference ($xx where xx is hex).
     */
    private fun resolveDirectReference(rowId: String, depth: Int): JsonElement {
        val rowValue = rows[rowId]
        if (rowValue == null) {
            return JsonPrimitive("\$$rowId [not found]")
        }

        val jsonElement = rowValueToJson(rowValue)
        return resolve(jsonElement, depth + 1)
    }

    /**
     * Convert a RowValue to JsonElement for resolution.
     */
    private fun rowValueToJson(rowValue: RowValue): JsonElement {
        return when (rowValue) {
            is RowValue.Model -> rowValue.json
            is RowValue.Text -> JsonPrimitive(rowValue.value)
            is RowValue.Module -> JsonPrimitive("[Module: ${rowValue.path}]")
            is RowValue.Error -> JsonPrimitive("[Error: ${rowValue.message}]")
            is RowValue.Hint -> JsonPrimitive("[Hint: ${rowValue.code}]")
            is RowValue.DebugInfo -> rowValue.data
            is RowValue.Unknown -> JsonPrimitive("[Unknown: ${rowValue.tag}]")
        }
    }
}
