package dev.kdriver.nextjs

import dev.kaccelero.serializers.Serialization
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/**
 * Abstract implementation of [CapturedPushes] that extracts `__next_f` push events from provided HTML content.
 */
abstract class AbstractCapturedPushesFromHtml : AbstractCapturedPushes() {

    /**
     * Provides the HTML content from which to extract `__next_f` push events.
     */
    abstract suspend fun provideHtml(): String

    override suspend fun provideNextF(): JsonArray {
        return JsonArray(extractNextFJsonObjects(provideHtml()))
    }

    private fun extractNextFJsonObjects(html: String): List<JsonElement> {
        val results = mutableListOf<JsonElement>()

        val scriptRegex = Regex("<script[^>]*>([\\s\\S]*?)</script>", RegexOption.IGNORE_CASE)
        val pushRegex = Regex("self\\.__next_f\\.push\\(\\s*(\\[[^)]*])\\s*\\)")

        for (match in scriptRegex.findAll(html)) {
            val scriptContent = match.groupValues[1]

            for (push in pushRegex.findAll(scriptContent)) {
                val pushContent = push.groupValues[1]
                results += Serialization.json.parseToJsonElement(pushContent)
            }
        }

        return results
    }

}
