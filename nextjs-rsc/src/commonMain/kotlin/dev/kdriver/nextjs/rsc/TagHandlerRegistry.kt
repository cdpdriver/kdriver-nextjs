package dev.kdriver.nextjs.rsc

import dev.kdriver.nextjs.rsc.handlers.*

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
