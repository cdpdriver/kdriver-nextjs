package dev.kdriver.nextjs

import dev.kdriver.core.tab.Tab

/**
 * Extension function to capture push events in a Next.js application using the [CapturedPushes] interface.
 *
 * @param block The suspend function block to execute while capturing push events.
 *
 * @return The result of the block execution.
 */
suspend fun <T> Tab.capturePushes(block: suspend CapturedPushes.() -> T): T {
    return DefaultCapturedPushes(this).capture(block)
}
