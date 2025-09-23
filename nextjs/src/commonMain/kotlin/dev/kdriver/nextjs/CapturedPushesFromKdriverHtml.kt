package dev.kdriver.nextjs

import dev.kdriver.core.tab.ReadyState
import dev.kdriver.core.tab.Tab

/**
 * Implementation of [CapturedPushes] that retrieves HTML content from a KDriver [Tab].
 *
 * @property tab The KDriver tab from which to capture HTML content.
 */
class CapturedPushesFromKdriverHtml(private val tab: Tab) : AbstractCapturedPushesFromHtml(), CapturedPushes {

    override suspend fun provideHtml(): String {
        runCatching { tab.waitForReadyState(ReadyState.COMPLETE) }
        return tab.getContent()
    }

}
