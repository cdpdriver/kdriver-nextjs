package dev.kdriver.nextjs

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AbstractCapturedPushesTest {

    @Test
    fun testDeprecatedFetchAll_parsesMultipleJsonElementsFromPayloads() = runTest {
        val sut = object : AbstractCapturedPushes() {
            override suspend fun provideNextF(): JsonArray {
                return Serialization.json.parseToJsonElement(
                    """
                    [
                      ["signal","{\"alpha\":1}"],
                      ["other","prefix {\"beta\":2} middle [{\"gamma\":3}] suffix"],
                      [["wrapped","{\"delta\":4}"]]
                    ]
                    """
                ).jsonArray
            }
        }

        val results = sut.fetchAll()

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
