package dev.kdriver.nextjs

import dev.kdriver.core.tab.ReadyState
import dev.kdriver.core.tab.Tab
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CapturedPushesFromKdriverTest {

    @Test
    fun testFetchAll_parsesMultipleJsonElementsFromPayloads() = runBlocking {
        val tab = mockk<Tab>(relaxed = true)

        val capturedNextFJson = """
            [
              ["signal","{\"alpha\":1}"],
              ["other","prefix {\"beta\":2} middle [{\"gamma\":3}] suffix"],
              [["wrapped","{\"delta\":4}"]]
            ]
        """.trimIndent()

        coEvery { tab.waitForReadyState(ReadyState.COMPLETE) } returns true
        coEvery { tab.rawEvaluate("JSON.stringify(self.__next_f)") } returns JsonPrimitive(capturedNextFJson)

        val results = tab.capturePushesFromJs { fetchAll() }

        // Expected parsed JSON elements:
        // 1) {"alpha":1}
        // 2) {"beta":2}
        // 3) [{"gamma":3}]
        // 4) {"delta":4}
        assertEquals(4, results.size)

        assertIs<JsonObject>(results[0])
        assertEquals(JsonPrimitive(1), (results[0] as JsonObject)["alpha"])

        assertIs<JsonObject>(results[1])
        assertEquals(JsonPrimitive(2), (results[1] as JsonObject)["beta"])

        assertIs<JsonArray>(results[2])
        assertIs<JsonObject>((results[2] as JsonArray)[0])
        assertEquals(JsonPrimitive(3), ((results[2] as JsonArray)[0] as JsonObject)["gamma"])

        assertIs<JsonObject>(results[3])
        assertEquals(JsonPrimitive(4), (results[3] as JsonObject)["delta"])
    }

    @Test
    fun testCapturePushesFromHtml_parsesPushesFromTabContent() = runBlocking {
        val tab = mockk<Tab>(relaxed = true)

        val html = """
            <html>
              <head>
                <script>
                  self.__next_f = [];
                  self.__next_f.push(["signal","{\"alpha\":1}"]); 
                </script>
              </head>
              <body>
                <script>
                  self.__next_f.push(["other","prefix {\"beta\":2} middle [{\"gamma\":3}] suffix"]);
                  self.__next_f.push([["wrapped","{\"delta\":4}"]]);
                </script>
              </body>
            </html>
        """.trimIndent()

        coEvery { tab.waitForReadyState(ReadyState.COMPLETE) } returns true
        coEvery { tab.getContent() } returns html

        val results = tab.capturePushesFromHtml { fetchAll() }

        // Expected parsed JSON elements:
        // 1) {"alpha":1}
        // 2) {"beta":2}
        // 3) [{"gamma":3}]
        // 4) {"delta":4}
        assertEquals(4, results.size)

        assertIs<JsonObject>(results[0])
        assertEquals(JsonPrimitive(1), (results[0] as JsonObject)["alpha"])

        assertIs<JsonObject>(results[1])
        assertEquals(JsonPrimitive(2), (results[1] as JsonObject)["beta"])

        assertIs<JsonArray>(results[2])
        assertIs<JsonObject>((results[2] as JsonArray)[0])
        assertEquals(JsonPrimitive(3), ((results[2] as JsonArray)[0] as JsonObject)["gamma"])

        assertIs<JsonObject>(results[3])
        assertEquals(JsonPrimitive(4), (results[3] as JsonObject)["delta"])
    }

}
