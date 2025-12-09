package dev.kdriver.nextjs

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CapturedPushesFromHtmlTest {

    @Test
    fun testFetchAll_parsesMultipleJsonElementsFromPayloads() = runTest {
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
                  self.__next_f.push([["with-parentheses","{\"title\":\"hello (world)\"}"]]);
                </script>
              </body>
            </html>
        """.trimIndent()

        val sut = CapturedPushesFromHtml(html)

        val results = sut.fetchAll()

        // Expected parsed JSON elements:
        // 1) {"alpha":1}
        // 2) {"beta":2}
        // 3) [{"gamma":3}]
        // 4) {"delta":4}
        // 5) {"title":"hello (world)"}
        assertEquals(5, results.size)

        assertIs<JsonObject>(results[0])
        assertEquals(JsonPrimitive(1), (results[0] as JsonObject)["alpha"])

        assertIs<JsonObject>(results[1])
        assertEquals(JsonPrimitive(2), (results[1] as JsonObject)["beta"])

        assertIs<JsonArray>(results[2])
        assertIs<JsonObject>((results[2] as JsonArray)[0])
        assertEquals(JsonPrimitive(3), ((results[2] as JsonArray)[0] as JsonObject)["gamma"])

        assertIs<JsonObject>(results[3])
        assertEquals(JsonPrimitive(4), (results[3] as JsonObject)["delta"])

        // Ensure the regex works with parentheses in strings
        assertIs<JsonObject>(results[4])
        assertEquals(JsonPrimitive("hello (world)"), (results[4] as JsonObject)["title"])
    }
}
