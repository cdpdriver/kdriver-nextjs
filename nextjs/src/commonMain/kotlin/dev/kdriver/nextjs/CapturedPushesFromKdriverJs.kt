package dev.kdriver.nextjs

import dev.kaccelero.serializers.Serialization
import dev.kdriver.core.tab.ReadyState
import dev.kdriver.core.tab.Tab
import dev.kdriver.core.tab.evaluate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray

/**
 * Implementation of [CapturedPushes] that retrieves the `__next_f` JavaScript variable from a KDriver [Tab].
 *
 * @param tab The [Tab] instance representing the browser tab to interact with.
 */
class CapturedPushesFromKdriverJs(private val tab: Tab) : AbstractCapturedPushes(), CapturedPushes {

    override suspend fun provideNextF(): JsonArray {
        runCatching { tab.waitForReadyState(ReadyState.COMPLETE) }
        val raw = tab.evaluate<String>("JSON.stringify(self.__next_f)") ?: "[]"
        return Serialization.json.parseToJsonElement(raw).jsonArray
    }

}
