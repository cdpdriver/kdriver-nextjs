package dev.kdriver.nextjs

import kotlinx.serialization.json.*

class FlightPayloadResolver {

    // Map of row ID -> parsed content
    private val rows = mutableMapOf<String, JsonElement>()

    /**
     * Parse all the __next_f pushes and build the row map
     */
    fun parsePayloads(pushes: JsonArray) {
        for (push in pushes) {
            val arr = push.jsonArray
            val type = arr[0].jsonPrimitive.int

            if (type == 1) {
                // Type 1 = row data as string
                val payload = arr[1].jsonPrimitive.content
                parseRowData(payload)
            }
        }
    }

    private fun parseRowData(payload: String) {
        // Each payload can contain multiple lines
        for (line in payload.split("\n")) {
            if (line.isBlank()) continue

            // Format: id:typeCode:jsonData  OR  id:jsonData
            val colonIndex = line.indexOf(':')
            if (colonIndex == -1) continue

            val rowId = line.substring(0, colonIndex)
            val rest = line.substring(colonIndex + 1)

            // Check for type code (I, HL, etc.)
            val secondColon = rest.indexOf(':')
            val jsonData = if (secondColon != -1 && secondColon < 3) {
                // Has type code like "I:" or "HL:"
                rest.substring(secondColon + 1)
            } else {
                rest
            }

            if (jsonData.isNotBlank()) {
                try {
                    rows[rowId] = Json.parseToJsonElement(jsonData)
                } catch (e: Exception) {
                    // Some rows contain non-JSON data
                }
            }
        }
    }

    /**
     * Resolve all references in an element recursively
     */
    fun resolve(element: JsonElement): JsonElement {
        return when (element) {
            is JsonPrimitive -> resolveReference(element)
            is JsonArray -> JsonArray(element.map { resolve(it) })
            is JsonObject -> JsonObject(element.mapValues { resolve(it.value) })
            else -> element
        }
    }

    private fun resolveReference(primitive: JsonPrimitive): JsonElement {
        if (!primitive.isString) return primitive

        val value = primitive.content
        if (!value.startsWith("$")) return primitive

        return when {
            // Special literals
            value == "\$undefined" -> JsonNull
            value == "\$Infinity" -> JsonPrimitive(Double.POSITIVE_INFINITY)
            value == "\$-Infinity" -> JsonPrimitive(Double.NEGATIVE_INFINITY)
            value == "\$NaN" -> JsonPrimitive(Double.NaN)

            // Lazy reference: $Lxx
            value.startsWith("\$L") -> {
                val rowId = value.substring(2)
                rows[rowId]?.let { resolve(it) } ?: primitive
            }

            // Promise reference: $@xx
            value.startsWith("\$@") -> {
                val rowId = value.substring(2)
                rows[rowId]?.let { resolve(it) } ?: primitive
            }

            // Server function: $Fxx (usually keep as-is or handle specially)
            value.startsWith("\$F") -> primitive

            // Direct reference: $xx (just digits after $)
            value.matches(Regex("^\\\$[0-9a-fA-F]+$")) -> {
                val rowId = value.substring(1)
                rows[rowId]?.let { resolve(it) } ?: primitive
            }

            else -> primitive
        }
    }

    /**
     * Get the fully resolved root element
     */
    fun getResolvedRoot(): JsonElement? {
        return rows["0"]?.let { resolve(it) }
    }
}
