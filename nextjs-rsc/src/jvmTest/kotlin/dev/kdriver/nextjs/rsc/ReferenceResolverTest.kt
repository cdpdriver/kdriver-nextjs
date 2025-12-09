package dev.kdriver.nextjs.rsc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ReferenceResolverTest {

    @Test
    fun `resolve lazy reference`() {
        val rows = mapOf(
            "0" to RowValue.Model(JsonPrimitive("\$L1")),
            "1" to RowValue.Model(JsonArray(listOf(JsonPrimitive("Hello"))))
        )

        val resolver = ReferenceResolver(rows)
        val result = resolver.resolve(rows["0"]!!.let { (it as RowValue.Model).json })

        assertIs<JsonArray>(result)
        assertEquals("Hello", result[0].jsonPrimitive.content)
    }

    @Test
    fun `resolve nested lazy references`() {
        val rows = mapOf(
            "0" to RowValue.Model(JsonPrimitive("\$L1")),
            "1" to RowValue.Model(JsonPrimitive("\$L2")),
            "2" to RowValue.Model(JsonPrimitive("Final value"))
        )

        val resolver = ReferenceResolver(rows)
        val result = resolver.resolve(rows["0"]!!.let { (it as RowValue.Model).json })

        assertIs<JsonPrimitive>(result)
        assertEquals("Final value", result.content)
    }

    @Test
    fun `resolve promise reference`() {
        val rows = mapOf(
            "0" to RowValue.Model(JsonPrimitive("\$@1")),
            "1" to RowValue.Model(JsonPrimitive("Async result"))
        )

        val resolver = ReferenceResolver(rows)
        val result = resolver.resolve(rows["0"]!!.let { (it as RowValue.Model).json })

        assertIs<JsonPrimitive>(result)
        assertEquals("Async result", result.content)
    }

    @Test
    fun `resolve direct hex reference`() {
        val rows = mapOf(
            "0" to RowValue.Model(JsonPrimitive("\$1a")),
            "1a" to RowValue.Model(JsonPrimitive("Hex ref value"))
        )

        val resolver = ReferenceResolver(rows)
        val result = resolver.resolve(rows["0"]!!.let { (it as RowValue.Model).json })

        assertIs<JsonPrimitive>(result)
        assertEquals("Hex ref value", result.content)
    }

    @Test
    fun `resolve escaped dollar sign`() {
        val json = Json.parseToJsonElement("""{"price":"${'$'}${'$'}100"}""")
        val resolver = ReferenceResolver(emptyMap())
        val result = resolver.resolve(json)

        val obj = result.jsonObject
        val priceValue = obj["price"]?.jsonPrimitive?.content
        assertEquals("$100", priceValue)
    }

    @Test
    fun `resolve special literals`() {
        val rows = mapOf(
            "0" to RowValue.Model(
                Json.parseToJsonElement(
                    """["${'$'}undefined","${'$'}Infinity","${'$'}-Infinity","${'$'}NaN"]"""
                )
            )
        )

        val resolver = ReferenceResolver(rows)
        val result = resolver.resolve(rows["0"]!!.let { (it as RowValue.Model).json })

        assertIs<JsonArray>(result)
        // First element should be null (undefined)
        // Others should be infinity/nan
    }

    @Test
    fun `resolve references in nested objects`() {
        val rows = mapOf(
            "0" to RowValue.Model(
                Json.parseToJsonElement("""{"nested":{"ref":"${'$'}L1"}}""")
            ),
            "1" to RowValue.Model(JsonPrimitive("Resolved"))
        )

        val resolver = ReferenceResolver(rows)
        val result = resolver.resolve(rows["0"]!!.let { (it as RowValue.Model).json })

        // Should resolve the nested reference
        assertIs<kotlinx.serialization.json.JsonObject>(result)
    }

    @Test
    fun `resolve references in arrays`() {
        val rows = mapOf(
            "0" to RowValue.Model(
                Json.parseToJsonElement("""["${'$'}L1","${'$'}L2"]""")
            ),
            "1" to RowValue.Model(JsonPrimitive("First")),
            "2" to RowValue.Model(JsonPrimitive("Second"))
        )

        val resolver = ReferenceResolver(rows)
        val result = resolver.resolve(rows["0"]!!.let { (it as RowValue.Model).json })

        assertIs<JsonArray>(result)
        assertEquals(2, result.size)
        assertEquals("First", result[0].jsonPrimitive.content)
        assertEquals("Second", result[1].jsonPrimitive.content)
    }

    @Test
    fun `resolve missing reference returns placeholder`() {
        val rows = mapOf(
            "0" to RowValue.Model(JsonPrimitive("\$L99"))
        )

        val resolver = ReferenceResolver(rows)
        val result = resolver.resolve(rows["0"]!!.let { (it as RowValue.Model).json })

        assertIs<JsonPrimitive>(result)
        assert(result.content.contains("not found"))
    }

    @Test
    fun `resolve module reference`() {
        val rows = mapOf(
            "0" to RowValue.Model(JsonPrimitive("\$L1")),
            "1" to RowValue.Module("@/components/Button", listOf("default"), "Button")
        )

        val resolver = ReferenceResolver(rows)
        val result = resolver.resolve(rows["0"]!!.let { (it as RowValue.Model).json })

        assertIs<JsonPrimitive>(result)
        assert(result.content.contains("Module"))
        assert(result.content.contains("Button"))
    }

    @Test
    fun `resolve error reference`() {
        val rows = mapOf(
            "0" to RowValue.Model(JsonPrimitive("\$L1")),
            "1" to RowValue.Error("Not found", "stack trace", "digest")
        )

        val resolver = ReferenceResolver(rows)
        val result = resolver.resolve(rows["0"]!!.let { (it as RowValue.Model).json })

        assertIs<JsonPrimitive>(result)
        assert(result.content.contains("Error"))
    }

    @Test
    fun `resolve text reference`() {
        val rows = mapOf(
            "0" to RowValue.Model(JsonPrimitive("\$L1")),
            "1" to RowValue.Text("Large text content")
        )

        val resolver = ReferenceResolver(rows)
        val result = resolver.resolve(rows["0"]!!.let { (it as RowValue.Model).json })

        assertIs<JsonPrimitive>(result)
        assertEquals("Large text content", result.content)
    }

    @Test
    fun `max depth protection prevents infinite loop`() {
        val rows = mapOf(
            "0" to RowValue.Model(JsonPrimitive("\$L1")),
            "1" to RowValue.Model(JsonPrimitive("\$L0"))
        )

        val resolver = ReferenceResolver(rows, maxDepth = 10)
        val result = resolver.resolve(rows["0"]!!.let { (it as RowValue.Model).json })

        // Should not crash, should return something
        assertIs<JsonPrimitive>(result)
    }
}
