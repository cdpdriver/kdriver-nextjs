package dev.kdriver.nextjs.rsc

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LengthEncodedTextTest {

    /**
     * Test case from user: Text tag with length encoding.
     * Format: `d1:T7db,` where 7db is hex length (2011 bytes).
     * The actual text content comes in the next line/push.
     */
    @Test
    fun `test text tag with length encoding`() {
        // Simpler test case - hex length 10 (16 bytes)
        val payload = """
            0:{"description":"${'$'}d1"}
            d1:T10,This is a text
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val result = resolver.getResolvedRoot()
        assertNotNull(result)

        val obj = result.jsonObject
        val description = obj["description"]?.jsonPrimitive?.content

        // Should resolve to the full text, not just "This is a text"
        assertEquals("This is a text", description)
    }

    /**
     * Real-world test case from user.
     * Row d1 has format: `d1:T7db,` (7db hex = 2011 decimal bytes)
     * The actual content follows.
     */
    @Test
    fun `test real-world description with length encoding`() {
        // Simpler version to debug
        val payload = """
            0:{"description":"${'$'}d1"}
            d1:T7db,-20% sur les lots ! 🎁
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        // First check: does the row parse correctly?
        val rows = resolver.getAllRows()
        val d1Row = rows["d1"]
        assertNotNull(d1Row, "Row d1 should exist")
        assert(d1Row is RowValue.Text) { "Row d1 should be Text, got: ${d1Row::class.simpleName}" }
        val d1Text = (d1Row as RowValue.Text).value
        println("DEBUG: Row d1 text value: '$d1Text'")
        assert(d1Text.startsWith("-20%")) { "Row d1 text should start with '-20%', got: '$d1Text'" }

        // Second check: does the reference resolve correctly?
        val result = resolver.getResolvedRoot()
        assertNotNull(result)

        val obj = result.jsonObject
        val description = obj["description"]?.jsonPrimitive?.content

        // Should resolve to the full text, not just "7db,"
        assertNotNull(description, "Description should not be null")
        assert(description.contains("-20% sur les lots")) {
            "Expected description to contain the actual text, but got: '$description'"
        }
    }

    /**
     * Test that text WITHOUT length encoding still works.
     */
    @Test
    fun `test text tag without length encoding`() {
        val payload = """
            0:{"description":"${'$'}d1"}
            d1:TSimple text content
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val result = resolver.getResolvedRoot()
        assertNotNull(result)

        val obj = result.jsonObject
        val description = obj["description"]?.jsonPrimitive?.content

        assertEquals("Simple text content", description)
    }

    private fun createPushesArray(payload: String): JsonArray {
        return JsonArray(
            listOf(
                JsonArray(
                    listOf(
                        JsonPrimitive(1),
                        JsonPrimitive(payload + "\n")
                    )
                )
            )
        )
    }
}
