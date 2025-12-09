package dev.kdriver.nextjs.rsc.handlers

import dev.kdriver.nextjs.rsc.RowValue
import dev.kdriver.nextjs.rsc.TagHandler
import kotlinx.serialization.json.Json

/**
 * Handler for debug info (tag: D).
 *
 * Format: JSON debug metadata
 * Example: D{"name": "Component", "stack": "..."}
 */
class DebugInfoTagHandler : TagHandler {
    override val tag: Char = 'D'

    override fun parse(data: String): RowValue {
        return try {
            val json = Json.parseToJsonElement(data)
            RowValue.DebugInfo(json)
        } catch (e: Exception) {
            RowValue.Unknown(tag, data)
        }
    }
}
