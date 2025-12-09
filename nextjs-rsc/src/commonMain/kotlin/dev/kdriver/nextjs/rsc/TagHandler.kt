package dev.kdriver.nextjs.rsc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

/**
 * Registry for tag handlers. Allows easy addition of new handlers.
 */
class TagHandlerRegistry {
    private val handlers = mutableMapOf<Char, TagHandler>()

    init {
        // Register built-in handlers
        register(ModuleTagHandler())
        register(HintTagHandler())
        register(ErrorTagHandler())
        register(TextTagHandler())
        register(DebugInfoTagHandler())
    }

    /**
     * Register a new tag handler.
     */
    fun register(handler: TagHandler) {
        handlers[handler.tag] = handler
    }

    /**
     * Get handler for a specific tag.
     */
    fun getHandler(tag: Char): TagHandler? = handlers[tag]

    /**
     * Parse data using the appropriate handler.
     */
    fun parse(tag: Char, data: String): RowValue {
        return handlers[tag]?.parse(data)
            ?: RowValue.Unknown(tag, data)
    }
}

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

/**
 * Handler for text chunks (tag: T).
 *
 * Format: plain text or with length encoding
 * Example: T:10,Hello World
 */
class TextTagHandler : TagHandler {
    override val tag: Char = 'T'

    override fun parse(data: String): RowValue {
        // For now, treat entire data as text
        // TODO: Handle length-based encoding if needed
        return RowValue.Text(data)
    }
}

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
