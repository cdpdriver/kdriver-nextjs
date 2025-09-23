package dev.kdriver.nextjs

import dev.kdriver.core.tab.Tab

/**
 * Extension function to capture push events in a Next.js application using the [CapturedPushes] interface.
 *
 * @param block The suspend function block to execute while capturing push events.
 *
 * @return The result of the block execution.
 */
suspend fun <T> Tab.capturePushesFromJs(block: suspend CapturedPushes.() -> T): T {
    return CapturedPushesFromKdriverJs(this).block()
}

/**
 * Extension function to capture push events from HTML content in a Next.js application using the [CapturedPushes] interface.
 *
 * @param block The suspend function block to execute while capturing push events.
 *
 * @return The result of the block execution.
 */
suspend fun <T> Tab.capturePushesFromHtml(block: suspend CapturedPushes.() -> T): T {
    return CapturedPushesFromKdriverHtml(this).block()
}
