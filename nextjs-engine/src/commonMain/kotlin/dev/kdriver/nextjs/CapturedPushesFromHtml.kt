package dev.kdriver.nextjs

/**
 * Implementation of [AbstractCapturedPushesFromHtml] that takes HTML content as a constructor parameter.
 */
class CapturedPushesFromHtml(
    /**
     * The HTML content from which to extract `__next_f` push events.
     */
    val html: String,
) : AbstractCapturedPushesFromHtml() {

    override suspend fun provideHtml(): String = html

}
