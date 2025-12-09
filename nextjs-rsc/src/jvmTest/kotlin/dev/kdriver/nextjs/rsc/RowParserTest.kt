package dev.kdriver.nextjs.rsc

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RowParserTest {

    @Test
    fun `parse simple model row without tag`() {
        val row = RowParser.parseRow("""0:["$","div",null,{}]""")

        assertNotNull(row)
        assertEquals("0", row.id)
        assertNull(row.tag)
        assertEquals("""["$","div",null,{}]""", row.data)
    }

    @Test
    fun `parse row with module tag`() {
        val row = RowParser.parseRow("""1:I["path",["exports"],"name"]""")

        assertNotNull(row)
        assertEquals("1", row.id)
        assertEquals('I', row.tag)
        assertEquals("""["path",["exports"],"name"]""", row.data)
    }

    @Test
    fun `parse row with tag and colon separator`() {
        val row = RowParser.parseRow("""2:H:{"key":"value"}""")

        assertNotNull(row)
        assertEquals("2", row.id)
        assertEquals('H', row.tag)
        assertEquals("""{"key":"value"}""", row.data)
    }

    @Test
    fun `parse row with text tag`() {
        val row = RowParser.parseRow("""3:TLarge text content""")

        assertNotNull(row)
        assertEquals("3", row.id)
        assertEquals('T', row.tag)
        assertEquals("Large text content", row.data)
    }

    @Test
    fun `parse row with error tag`() {
        val row = RowParser.parseRow("""4:E{"message":"Error"}""")

        assertNotNull(row)
        assertEquals("4", row.id)
        assertEquals('E', row.tag)
        assertEquals("""{"message":"Error"}""", row.data)
    }

    @Test
    fun `parse row with hex id`() {
        val row = RowParser.parseRow("""1a:["$","p",null,{}]""")

        assertNotNull(row)
        assertEquals("1a", row.id)
        assertNull(row.tag)
        assertEquals("""["$","p",null,{}]""", row.data)
    }

    @Test
    fun `parse row with lazy reference`() {
        val row = RowParser.parseRow("""0:"${'$'}L1"""")

        assertNotNull(row)
        assertEquals("0", row.id)
        assertNull(row.tag)
        assertEquals(""""${'$'}L1"""", row.data)
    }

    @Test
    fun `parse blank line returns null`() {
        val row = RowParser.parseRow("")
        assertNull(row)
    }

    @Test
    fun `parse line without colon returns null`() {
        val row = RowParser.parseRow("invalid")
        assertNull(row)
    }

    @Test
    fun `parse multiple rows`() {
        val payload = """
            0:"${'$'}L1"
            1:I["path",[],""]
            2:["${'$'}","div",null,{}]
        """.trimIndent()

        val rows = RowParser.parseRows(payload)

        assertEquals(3, rows.size)
        assertEquals("0", rows[0].id)
        assertEquals("1", rows[1].id)
        assertEquals("2", rows[2].id)
    }

    @Test
    fun `parse rows ignores blank lines`() {
        val payload = """
            0:"${'$'}L1"

            1:["${'$'}","div",null,{}]
        """.trimIndent()

        val rows = RowParser.parseRows(payload)

        assertEquals(2, rows.size)
    }

    @Test
    fun `parse row with data that looks like tag`() {
        // "Test" should not be parsed as tag 'T' because 'e' follows
        val row = RowParser.parseRow("""0:Testing data""")

        assertNotNull(row)
        assertEquals("0", row.id)
        assertNull(row.tag)
        assertEquals("Testing data", row.data)
    }
}
