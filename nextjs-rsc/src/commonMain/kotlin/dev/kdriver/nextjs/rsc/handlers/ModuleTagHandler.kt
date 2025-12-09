package dev.kdriver.nextjs.rsc.handlers

import dev.kdriver.nextjs.rsc.RowValue
import dev.kdriver.nextjs.rsc.TagHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Handler for module imports (tag: I).
 *
 * Format: ["path", ["export1", "export2"], "name"]
 * Example: I["(app)/page", ["default"], ""]
 */
class ModuleTagHandler : TagHandler {
    override val tag: Char = 'I'

    override fun parse(data: String): RowValue {
        return try {
            val json = Json.parseToJsonElement(data).jsonArray
            RowValue.Module(
                path = json[0].jsonPrimitive.content,
                exports = json[1].jsonArray.map { it.jsonPrimitive.content },
                name = json.getOrNull(2)?.jsonPrimitive?.content ?: ""
            )
        } catch (e: Exception) {
            RowValue.Unknown(tag, data)
        }
    }
}
