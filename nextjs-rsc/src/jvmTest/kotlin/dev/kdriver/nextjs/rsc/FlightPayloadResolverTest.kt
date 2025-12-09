package dev.kdriver.nextjs.rsc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class FlightPayloadResolverTest {

    /**
     * Test basic $L (lazy) reference resolution.
     * This is the MAIN USE CASE!
     */
    @Test
    fun `test basic lazy reference resolution`() {
        val payload = """
            0:"${'$'}L1"
            1:["${'$'}","p",null,{"children":"Hello World"}]
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val result = resolver.getResolvedRoot()
        assertNotNull(result)

        // Should resolve to the array in row 1
        assertIs<JsonArray>(result)
        assertEquals("$", result[0].jsonPrimitive.content)
        assertEquals("p", result[1].jsonPrimitive.content)
    }

    /**
     * Test nested $L references (component tree).
     */
    @Test
    fun `test nested lazy references`() {
        val payload = """
            0:"${'$'}L1"
            1:["${'$'}","div",null,{"className":"container","children":"${'$'}L2"}]
            2:["${'$'}","p",null,{"children":"Hello World"}]
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val result = resolver.getResolvedRoot()
        assertNotNull(result)

        // Root should be the div
        assertIs<JsonArray>(result)
        assertEquals("div", result[1].jsonPrimitive.content)

        // Children should be resolved
        val props = result[3].jsonObject
        val children = props["children"]
        assertNotNull(children)
        assertIs<JsonArray>(children)
        assertEquals("p", children[1].jsonPrimitive.content)
    }

    /**
     * Test multiple $L references in the same element.
     */
    @Test
    fun `test multiple lazy references`() {
        val payload = """
            0:"${'$'}L1"
            1:["${'$'}","html",null,{"children":["${'$'}L2","${'$'}L3"]}]
            2:["${'$'}","head",null,{}]
            3:["${'$'}","body",null,{"children":"Content"}]
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val result = resolver.getResolvedRoot()
        assertNotNull(result)

        val props = result.jsonArray[3].jsonObject
        val children = props["children"]?.jsonArray
        assertNotNull(children)
        assertEquals(2, children.size)

        // Both children should be resolved arrays
        assertIs<JsonArray>(children[0])
        assertIs<JsonArray>(children[1])
    }

    /**
     * Test $L reference with module import.
     */
    @Test
    fun `test lazy reference to module`() {
        val payload = """
            0:"${'$'}L1"
            1:I["(app)/page",["default"],""]
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val result = resolver.getResolvedRoot()
        assertNotNull(result)

        // Should resolve to module placeholder
        assertIs<JsonPrimitive>(result)
        assert(result.content.contains("Module"))
    }

    /**
     * Test direct hex reference ($xx).
     */
    @Test
    fun `test direct hex reference`() {
        val payload = """
            0:"${'$'}2"
            2:["${'$'}","div",null,{"children":"Direct ref"}]
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val result = resolver.getResolvedRoot()
        assertNotNull(result)

        assertIs<JsonArray>(result)
        assertEquals("div", result[1].jsonPrimitive.content)
    }

    /**
     * Test promise reference ($@).
     */
    @Test
    fun `test promise reference`() {
        val payload = """
            0:"${'$'}@1"
            1:["${'$'}","div",null,{"children":"Async content"}]
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val result = resolver.getResolvedRoot()
        assertNotNull(result)

        assertIs<JsonArray>(result)
        assertEquals("div", result[1].jsonPrimitive.content)
    }

    /**
     * Test escaped dollar sign ($$).
     */
    @Test
    fun `test escaped dollar sign`() {
        val payload = """
            0:{"price":"${'$'}${'$'}100"}
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val result = resolver.getResolvedRoot()
        assertNotNull(result)

        val obj = result.jsonObject
        assertEquals("$100", obj["price"]?.jsonPrimitive?.content)
    }

    /**
     * Test special literals.
     */
    @Test
    fun `test special literals`() {
        val payload = """
            0:{"undef":"${'$'}undefined","inf":"${'$'}Infinity","negInf":"${'$'}-Infinity","nan":"${'$'}NaN"}
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val result = resolver.getResolvedRoot()
        assertNotNull(result)

        val obj = result.jsonObject
        // $undefined becomes null in JSON
        assertIs<JsonPrimitive>(obj["undef"])
    }

    /**
     * Test module import (tag I).
     */
    @Test
    fun `test module import tag`() {
        val payload = """
            0:"${'$'}L1"
            1:I["(app)/components/Button",["default","Secondary"],"Button"]
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val rows = resolver.getAllRows()
        val moduleRow = rows["1"]

        assertNotNull(moduleRow)
        assertIs<RowValue.Module>(moduleRow)
        assertEquals("(app)/components/Button", moduleRow.path)
        assertEquals(listOf("default", "Secondary"), moduleRow.exports)
        assertEquals("Button", moduleRow.name)
    }

    /**
     * Test error tag (E).
     */
    @Test
    fun `test error tag`() {
        val payload = """
            0:"${'$'}L1"
            1:E{"message":"Not found","stack":"Error: Not found\n  at ...","digest":"abc123"}
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val rows = resolver.getAllRows()
        val errorRow = rows["1"]

        assertNotNull(errorRow)
        assertIs<RowValue.Error>(errorRow)
        assertEquals("Not found", errorRow.message)
        assertNotNull(errorRow.stack)
        assertEquals("abc123", errorRow.digest)
    }

    /**
     * Test text tag (T).
     */
    @Test
    fun `test text tag`() {
        val payload = """
            0:"${'$'}L1"
            1:TLarge text content here
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val rows = resolver.getAllRows()
        val textRow = rows["1"]

        assertNotNull(textRow)
        assertIs<RowValue.Text>(textRow)
        assertEquals("Large text content here", textRow.value)
    }

    /**
     * Test hint tag (H).
     */
    @Test
    fun `test hint tag`() {
        val payload = """
            0:"${'$'}L1"
            1:H["font","https://fonts.googleapis.com/css2?family=Inter"]
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val rows = resolver.getAllRows()
        val hintRow = rows["1"]

        assertNotNull(hintRow)
        assertIs<RowValue.Hint>(hintRow)
        assertEquals("font", hintRow.code)
    }

    /**
     * Test missing reference (should not crash).
     */
    @Test
    fun `test missing reference returns placeholder`() {
        val payload = """
            0:"${'$'}L99"
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val result = resolver.getResolvedRoot()
        assertNotNull(result)

        // Should return a placeholder for missing reference
        assertIs<JsonPrimitive>(result)
        assert(result.content.contains("not found"))
    }

    /**
     * Test circular reference protection.
     */
    @Test
    fun `test circular reference protection`() {
        val payload = """
            0:"${'$'}L1"
            1:{"ref":"${'$'}L0"}
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        // Should not crash, should hit max depth
        val result = resolver.getResolvedRoot()
        assertNotNull(result)
    }

    /**
     * Test complex real-world example.
     */
    @Test
    fun `test real-world component tree`() {
        val payload = """
            0:"${'$'}L1"
            1:["${'$'}","html",null,{"lang":"en","children":["${'$'}L2","${'$'}L5"]}]
            2:["${'$'}","head",null,{"children":"${'$'}L3"}]
            3:["${'$'}","title",null,{"children":"My App"}]
            4:I["@/components/Navigation",["default"],"Navigation"]
            5:["${'$'}","body",null,{"children":["${'$'}L4","${'$'}L6"]}]
            6:["${'$'}","main",null,{"children":"${'$'}L7"}]
            7:["${'$'}","p",null,{"children":"Hello World"}]
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.parsePayloads(createPushesArray(payload))

        val result = resolver.getResolvedRoot()
        assertNotNull(result)

        // Should be html element
        assertIs<JsonArray>(result)
        assertEquals("html", result[1].jsonPrimitive.content)

        // Check props
        val props = result[3].jsonObject
        assertEquals("en", props["lang"]?.jsonPrimitive?.content)

        // Children should be resolved
        val children = props["children"]?.jsonArray
        assertNotNull(children)
        assertEquals(2, children.size)
    }

    /**
     * Test custom tag handler registration.
     */
    @Test
    fun `test custom tag handler`() {
        class CustomTagHandler : TagHandler {
            override val tag: Char = 'X'
            override fun parse(data: String): RowValue {
                return RowValue.Text("Custom: $data")
            }
        }

        val payload = """
            0:"${'$'}L1"
            1:XCustom data
        """.trimIndent()

        val resolver = FlightPayloadResolver()
        resolver.registerTagHandler(CustomTagHandler())
        resolver.parsePayloads(createPushesArray(payload))

        val rows = resolver.getAllRows()
        val customRow = rows["1"]

        assertNotNull(customRow)
        assertIs<RowValue.Text>(customRow)
        assertEquals("Custom: Custom data", customRow.value)
    }

    /**
     * Helper to create JsonArray of pushes from payload string.
     */
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
