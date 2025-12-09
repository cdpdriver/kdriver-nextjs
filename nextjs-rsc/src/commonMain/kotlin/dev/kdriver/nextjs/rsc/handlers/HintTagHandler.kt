package dev.kdriver.nextjs.rsc.handlers

import dev.kdriver.nextjs.rsc.RowValue
import dev.kdriver.nextjs.rsc.TagHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Handler for hints (tag: H).
 *
 * Format: code:data or codedata
 * Example: H["font", "https://fonts.googleapis.com/..."]
 */
class HintTagHandler : TagHandler {
    override val tag: Char = 'H'

    override fun parse(data: String): RowValue {
        return try {
            // Hints can be in format: code + JSON array
            // e.g., "H" + ["font", "url"]
            val json = Json.parseToJsonElement(data).jsonArray
            val code = json.getOrNull(0)?.jsonPrimitive?.content ?: "unknown"
            RowValue.Hint(code, json)
        } catch (e: Exception) {
            // Fallback: treat first char as code
            if (data.isNotEmpty()) {
                val code = data[0].toString()
                val rest = if (data.length > 1) data.substring(1) else "{}"
                try {
                    val json = Json.parseToJsonElement(rest)
                    RowValue.Hint(code, json)
                } catch (e2: Exception) {
                    RowValue.Unknown(tag, data)
                }
            } else {
                RowValue.Unknown(tag, data)
            }
        }
    }
}
