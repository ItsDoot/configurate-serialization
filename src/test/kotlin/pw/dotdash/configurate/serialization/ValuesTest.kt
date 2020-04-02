package pw.dotdash.configurate.serialization

import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test

class ValuesTest {

    @Serializable
    data class Numbers(val b: Byte, val s: Short, val i: Int, val l: Long, val f: Float, val d: Double)

    enum class Choice { A, B, C }

    @Serializable
    data class Strings(val c: Char, val s: String)

    @Serializable
    data class Other(val b: Boolean, val e: Choice, val u: Unit = Unit)

    @Serializable
    data class WithDefault(val i: Int = 5, val s: String = "foo")

    @Serializable
    data class WithNullable(val i: Int? = null, val s: String? = null)

    @Serializable
    data class WithNullableList(val i1: List<Int?>, val i2: List<String>? = null, val i3: List<WithNullable?>? = null)

    @Test
    fun `deserialize numbers`() {
        val obj: Numbers = deserializeConfig(
            "b = 42, s = 1337, i = 100500, l = 4294967294, f = 0.0, d = -0.123",
            Numbers.serializer()
        )

        assertEquals(42.toByte(), obj.b)
        assertEquals(1337.toShort(), obj.s)
        assertEquals(100500, obj.i)
        assertEquals(4294967294L, obj.l)
        assertEquals(0.0f, obj.f)
        assertEquals(-0.123, obj.d, 1e-9)
    }

    @Test
    fun `deserialize string types`() {
        val obj: Strings = deserializeConfig("c = f, s = foo", Strings.serializer())

        assertEquals('f', obj.c)
        assertEquals("foo", obj.s)
    }

    @Test
    fun `deserialize other types`() {
        val obj: Other = deserializeConfig("e = A, b = true", Other.serializer())

        assertEquals(Choice.A, obj.e)
        assertEquals(true, obj.b)
    }

    @Test
    fun `deserialize default values`() {
        val obj: WithDefault = deserializeConfig("", WithDefault.serializer())

        assertEquals(5, obj.i)
        assertEquals("foo", obj.s)
    }

    @Test
    fun `overwrite default values`() {
        val obj: WithDefault = deserializeConfig("i = 42, s = bar", WithDefault.serializer())

        assertEquals(42, obj.i)
        assertEquals("bar", obj.s)
    }

    @Test
    fun `deserialize nullable types`() {
        val obj: WithNullable = deserializeConfig("i = 10, s = null", WithNullable.serializer())

        assertEquals(10, obj.i)
        assertEquals(null, obj.s)
    }

    @Test
    fun `deserialize complex nullable values`() {
        val obj: WithNullableList = deserializeConfig(
            """i1 = [1, 3]
               i2 = null
               i3 = [{ i = 10, s = bar }]""".trimIndent(),
            WithNullableList.serializer()
        )

        assertEquals(listOf(1, 3), obj.i1)
        assertEquals(null, obj.i2)
        assertEquals(listOf(WithNullable(10, "bar")), obj.i3)
    }
}