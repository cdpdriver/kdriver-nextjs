package dev.kdriver.nextjs

import dev.kdriver.nextjs.rsc.FlightPayloadResolver
import kotlinx.serialization.json.*

/**
 * Abstract implementation of [CapturedPushes] that provides common functionality for capturing and fetching push events.
 */
abstract class AbstractCapturedPushes : CapturedPushes {

    override suspend fun resolvedNextF(): JsonElement {
        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(provideNextF())
        return resolver.getResolvedRoot() ?: error("No resolved root found")
    }

    @Deprecated("Use resolvedNextF() for single resolved element.")
    override suspend fun fetchAll(): List<JsonElement> {
        val jsonArray = provideNextF()
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
