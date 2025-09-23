package dev.kdriver.nextjs

import kotlinx.serialization.json.JsonElement

/**
 * Interface for capturing and fetching push events from a Next.js application.
 */
interface CapturedPushes {

    /**
     * Fetches all captured push events as a list of JSON elements.
     *
     * @return A list of JSON elements representing the captured push events.
     */
    suspend fun fetchAll(): List<JsonElement>

}
