package dev.kdriver.nextjs

import kotlinx.serialization.json.*
import kotlin.test.*

class JsonExtensionsTest {

    @Test
    fun asJsonObjectOrNull_and_asJsonArrayOrNull_work() {
        val obj = Json.parseToJsonElement("{" + "\"a\":1}")
        val arr = Json.parseToJsonElement("[1,2,3]")
        val str = Json.parseToJsonElement("\"hello\"")

        assertIs<JsonObject>(obj.asJsonObjectOrNull())
        assertNull(obj.asJsonArrayOrNull())

        assertIs<JsonArray>(arr.asJsonArrayOrNull())
        assertNull(arr.asJsonObjectOrNull())

        assertNull(str.asJsonObjectOrNull())
        assertNull(str.asJsonArrayOrNull())
    }

    @Test
    fun asStringOrNull_reads_primitives_text() {
        val s = Json.parseToJsonElement("\"hello\"")
        val n = Json.parseToJsonElement("42")
        val b = Json.parseToJsonElement("true")

        assertEquals("hello", s.asStringOrNull())
        assertEquals("42", n.asStringOrNull())
        assertEquals("true", b.asStringOrNull())
    }

    @Test
    fun asLongOrNull_parses_numbers_and_numeric_strings() {
        val n = Json.parseToJsonElement("123")
        val s = Json.parseToJsonElement("\"456\"")
        val bad = Json.parseToJsonElement("\"abc\"")
        val dbl = Json.parseToJsonElement("1.5")

        assertEquals(123L, n.asLongOrNull())
        assertEquals(456L, s.asLongOrNull())
        assertNull(bad.asLongOrNull())
        // 1.5 is not a Long
        assertNull(dbl.asLongOrNull())
    }

    @Test
    fun asDoubleOrNull_parses_numbers_and_numeric_strings() {
        val n = Json.parseToJsonElement("1.25")
        val i = Json.parseToJsonElement("7")
        val s = Json.parseToJsonElement("\"2.5\"")
        val bad = Json.parseToJsonElement("\"x\"")

        assertEquals(1.25, n.asDoubleOrNull())
        assertEquals(7.0, i.asDoubleOrNull())
        assertEquals(2.5, s.asDoubleOrNull())
        assertNull(bad.asDoubleOrNull())
    }

    @Test
    fun safe_accessors_on_object() {
        val obj = buildJsonObject {
            put("name", JsonPrimitive("neo"))
            put("age", JsonPrimitive(27))
            put("ratio", JsonPrimitive("3.14"))
            put("nested", buildJsonObject { put("x", 1) })
            put("nullish", JsonNull)
        }

        assertEquals("neo", obj.safeString("name"))
        // Numbers and booleans are exposed as their textual representation by asStringOrNull
        assertEquals("27", obj.safeString("age"))
        assertEquals(27L, obj.safeLong("age"))
        assertEquals(3.14, obj.safeDouble("ratio"))
        assertNull(obj.safeLong("missing"))
        assertNull(obj.safeString("nested"))
        assertNull(obj.safeString("nullish"))
    }

    @Test
    fun findAllObjects_collects_all_matching_objects_depth_first() {
        val root = Json.parseToJsonElement(
            """
            {
              "type":"keep",
              "child": {"type":"target", "x":1},
              "arr": [
                {"type":"target", "y":2},
                [{"z":3}, {"type":"other"}],
                {"w":4}
              ]
            }
            """.trimIndent()
        )

        val all = findAllObjects(root, predicate = { it["type"]?.jsonPrimitive?.content == "target" })
        assertEquals(2, all.size)
        assertTrue(all.all { it["type"]?.jsonPrimitive?.content == "target" })

        val none = findAllObjects(null, predicate = { true })
        assertTrue(none.isEmpty())
    }

    @Test
    fun findFirstObject_returns_first_depth_first_match() {
        val root = Json.parseToJsonElement(
            """
            {
              "a": {"mark":"first"},
              "b": [ {"mark":"second"}, {"mark":"third"} ]
            }
            """.trimIndent()
        )

        val found = findFirstObject(root) { obj -> "mark" in obj }
        assertIs<JsonObject>(found)
        assertEquals("first", found["mark"]?.jsonPrimitive?.content)
    }

    @Test
    fun findObjectWithKey_and_findPrimitiveByKey_search_nested_structures() {
        val root = Json.parseToJsonElement(
            """
            {
              "outer": [
                {"inner": {"k": 1}},
                {"other": [{"k": "value"}]}
              ]
            }
            """.trimIndent()
        )

        val withK = root.findObjectWithKey("k")
        assertIs<JsonObject>(withK)
        assertTrue("k" in withK)

        val prim = root.findPrimitiveByKey("k")
        assertIs<JsonPrimitive>(prim)
        // The first depth-first primitive could be number 1
        assertTrue(prim.intOrNull == 1 || prim.content == "value")
    }

}
