package dev.kdriver.nextjs

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/**
 * Interface for capturing and fetching push events from a Next.js application.
 */
interface CapturedPushes {

    /**
     * Provides the next set of push events as a [JsonArray].
     *
     * @return A [JsonArray] containing the next push events.
     */
    suspend fun provideNextF(): JsonArray

    /**
     * Resolves the next push event into a single JSON element.
     *
     * @return A [JsonElement] representing the resolved next push event.
     */
    suspend fun resolvedNextF(): JsonElement

    /**
     * Fetches all captured push events as a list of JSON elements.
     *
     * @return A list of JSON elements representing the captured push events.
     */
    @Deprecated("Use resolvedNextF() for single resolved element.")
    suspend fun fetchAll(): List<JsonElement>

}
