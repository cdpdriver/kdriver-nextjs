package dev.kdriver.nextjs

import kotlinx.serialization.json.JsonElement

/**
 * Interface for capturing and fetching push events from a Next.js application.
 */
interface CapturedPushes {

    /**
     * Captures push events during the execution of the provided block.
     *
     * @param block The suspend function block to execute while capturing push events.
     *
     * @return The result of the block execution.
     */
    suspend fun <R> capture(block: suspend CapturedPushes.() -> R): R

    /**
     * Fetches all captured push events as a list of JSON elements.
     *
     * @return A list of JSON elements representing the captured push events.
     */
    suspend fun fetchAll(): List<JsonElement>

}
