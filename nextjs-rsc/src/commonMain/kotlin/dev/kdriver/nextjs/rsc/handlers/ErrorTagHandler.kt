package dev.kdriver.nextjs.rsc.handlers

import dev.kdriver.nextjs.rsc.RowValue
import dev.kdriver.nextjs.rsc.TagHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Handler for errors (tag: E).
 *
 * Format: {"message": "...", "stack": "...", "digest": "..."}
 * Example: E{"message": "Not found", "stack": "..."}
 */
class ErrorTagHandler : TagHandler {
    override val tag: Char = 'E'

    override fun parse(data: String): RowValue {
        return try {
            val json = Json.parseToJsonElement(data).jsonObject
            RowValue.Error(
                message = json["message"]?.jsonPrimitive?.content
                    ?: "Unknown error",
                stack = json["stack"]?.jsonPrimitive?.content,
                digest = json["digest"]?.jsonPrimitive?.content
            )
        } catch (e: Exception) {
            RowValue.Error(
                message = "Failed to parse error: ${e.message}",
                stack = null,
                digest = null
            )
        }
    }
}
