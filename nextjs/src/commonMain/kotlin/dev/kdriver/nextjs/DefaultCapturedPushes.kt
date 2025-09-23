package dev.kdriver.nextjs

import dev.kaccelero.serializers.Serialization
import dev.kdriver.core.tab.ReadyState
import dev.kdriver.core.tab.Tab
import dev.kdriver.core.tab.evaluate
import kotlinx.serialization.json.*

/**
 * Default implementation of the [CapturedPushes] interface for capturing and fetching push events from a Next.js application.
 *
 * @param tab The [Tab] instance representing the browser tab to interact with.
 */
class DefaultCapturedPushes(private val tab: Tab) : CapturedPushes {

    override suspend fun <R> capture(block: suspend CapturedPushes.() -> R): R {
        return block()
    }

    override suspend fun fetchAll(): List<JsonElement> {
        runCatching { tab.waitForReadyState(ReadyState.COMPLETE) }
        val raw = tab.evaluate<String>("JSON.stringify(self.__next_f)") ?: "[]"
        val jsonArray = Serialization.json.parseToJsonElement(raw).jsonArray
        val results = mutableListOf<JsonElement>()
        for (push in jsonArray) {
            var args = push.jsonArray
            if (args.size == 1 && args[0] is JsonArray) args = args[0].jsonArray
            if (args.size >= 2) {
                val payload = args[1].jsonPrimitive.contentOrNull
                if (payload != null) {
                    val objects = extractJsonObjects(payload)
                    for (obj in objects) {
                        try {
                            results += Serialization.json.parseToJsonElement(obj)
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
        return results
    }

    private fun extractJsonObjects(text: String): List<String> {
        val results = mutableListOf<String>()
        var depth = 0
        var start = -1
        for ((i, c) in text.withIndex()) {
            when (c) {
                '{', '[' -> {
                    if (depth == 0) start = i
                    depth++
                }

                '}', ']' -> {
                    depth--
                    if (depth == 0 && start != -1) {
                        results.add(text.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return results
    }

}
