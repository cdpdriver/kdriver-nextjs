package dev.kdriver.nextjs.rsc

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MultiPushTextTest {

    /**
     * Test case where text data is in a separate push.
     * This is the real format from the user's example.
     *
     * Push 1: Row definition with length encoding but no data
     * Push 2: Raw text content (no row ID)
     */
    @Test
    fun `test text tag with data in next push`() {
        // Create two separate pushes
        val pushes = JsonArray(
            listOf(
                // Push 1: Row definitions
                JsonArray(
                    listOf(
                        JsonPrimitive(1),
                        JsonPrimitive("0:{\"description\":\"\$d1\"}\nd1:T7db,\n")
                    )
                ),
                // Push 2: Text content (no row ID)
                JsonArray(
                    listOf(
                        JsonPrimitive(1),
                        JsonPrimitive("-20% sur les lots ! 🎁")
                    )
                )
            )
        )

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(pushes)

        val result = resolver.getResolvedRoot()
        assertNotNull(result)

        val obj = result.jsonObject
        val description = obj["description"]?.jsonPrimitive?.content

        println("DEBUG: Description = '$description'")

        // Should resolve to the full text from push 2
        assertNotNull(description, "Description should not be null")
        assertEquals("-20% sur les lots ! 🎁", description)
    }

    /**
     * Real-world example from user: product description
     */
    @Test
    fun `test real-world multi-push description`() {
        val longText = """-20% sur les lots ! 🎁

➡ Marques disponibles dans mon dressing : Ralph Lauren, Carhartt, Levis, Lacoste, The North Face, WRSTBHVR ..."""

        val pushes = JsonArray(
            listOf(
                // Push 1: Row definitions including "d1:T7db," with no text after comma
                JsonArray(
                    listOf(
                        JsonPrimitive(1),
                        JsonPrimitive("38:\"\$34:metadata\"\nd1:T7db,\n")
                    )
                ),
                // Push 2: The actual text content (no row ID!)
                JsonArray(
                    listOf(
                        JsonPrimitive(1),
                        JsonPrimitive(longText)
                    )
                )
            )
        )

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(pushes)

        // Check that row d1 exists and has the text from push 2
        val rows = resolver.getAllRows()
        val d1Row = rows["d1"]

        assertNotNull(d1Row, "Row d1 should exist")
        assert(d1Row is RowValue.Text) { "Row d1 should be Text, got: ${d1Row::class.simpleName}" }

        val d1Text = (d1Row as RowValue.Text).value
        println("DEBUG: Row d1 text = '$d1Text'")

        assert(d1Text.contains("-20% sur les lots")) {
            "Expected text to contain '-20% sur les lots', but got: '$d1Text'"
        }
    }

    /**
     * Test multiple rows with some having multi-push text.
     */
    @Test
    fun `test multiple rows with mixed single and multi-push text`() {
        val pushes = JsonArray(
            listOf(
                // Push 1: Multiple row definitions
                JsonArray(
                    listOf(
                        JsonPrimitive(1),
                        JsonPrimitive("0:{\"a\":\"\$1\",\"b\":\"\$2\"}\n1:TSingle line text\n2:T100,\n")
                    )
                ),
                // Push 2: Text for row 2 (which was defined with length encoding)
                JsonArray(
                    listOf(
                        JsonPrimitive(1),
                        JsonPrimitive("Multi-push text content")
                    )
                )
            )
        )

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(pushes)

        val rows = resolver.getAllRows()

        // Row 1 should have single-line text
        val row1 = rows["1"] as? RowValue.Text
        assertNotNull(row1)
        assertEquals("Single line text", row1.value)

        // Row 2 should have multi-push text
        val row2 = rows["2"] as? RowValue.Text
        assertNotNull(row2)
        assertEquals("Multi-push text content", row2.value)
    }
}
