package pw.dotdash.configurate.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import java.io.BufferedReader

internal fun <T> deserializeConfig(source: String, deserializer: DeserializationStrategy<T>): T {
    val node: CommentedConfigurationNode = loadHoconNode(source)
    return ConfigurationNodeParser().parse(node, deserializer)
}

private fun loadHoconNode(source: String): CommentedConfigurationNode =
    HoconConfigurationLoader.builder().source() { BufferedReader(source.reader()) }.build().load()

class ObjectsTest {

    @Serializable
    data class Simple(val a: Int)

    @Serializable
    data class ConfigObjectInner(val e: String, val f: Float = 1.1f)

    @Serializable
    data class ConfigObject(val a: Int, val b: ConfigObjectInner)

    @Serializable
    data class WithList(val a: Int, val b: List<Int>)

    @Serializable
    data class WithMap(val x: Map<String, Int>)

    @Serializable
    data class NestedObject(val x: List<Simple>)

    @Serializable
    data class Complex(
        val i: Int,
        val s: String,
        val iList: List<Int>,
        val inner: List<Simple>,
        val ll: List<List<String>>,
        val m: Map<String, ConfigObjectInner>
    )

    @Serializable
    data class VeryComplex(
        val l: List<Map<String, List<Simple?>>?>,
        val m: Map<String, NestedObject?>?
    )

    private val configString: String = """
        i = 42
        s = "foo"
        iList = [1, 2, 3]
        inner = [{ a = 100500 }]
        ll = [[a, b], [x, z]]
        m {
            kek {
                e = foo
                f = 5.6
            }
            bar {
                e = baz
            }
        }
    """.trimIndent()

    private val complexConfigString: String = """
        l = [ { x = [ { a = 42 }, null ] }, null ]
        m {
            x = null
            y {
                x = [ { a = 43 } ]
            }
        }
    """.trimIndent()

    @Test
    fun `complex config`() {
        val obj: Complex = deserializeConfig(configString, Complex.serializer())

        assertEquals(42, obj.i)
        assertEquals("foo", obj.s)
        assertEquals(listOf(1, 2, 3), obj.iList)
        assertEquals(listOf(Simple(100500)), obj.inner)
        assertEquals(listOf(listOf("a", "b"), listOf("x", "z")), obj.ll)
        assertEquals(mapOf("kek" to ConfigObjectInner("foo", 5.6f), "bar" to ConfigObjectInner("baz")), obj.m)
    }

    @Test
    fun `very complex config`() {
        val obj: VeryComplex = deserializeConfig(complexConfigString, VeryComplex.serializer())

        assertEquals(listOf(mapOf("x" to listOf(Simple(42)))), obj.l)
        assertEquals(mapOf("y" to NestedObject(listOf(Simple(43)))), obj.m)
    }

    @Test
    fun `simple config`() {
        val obj: Simple = deserializeConfig("a = 42", Simple.serializer())
        assertEquals(Simple(42), obj)
    }

    @Test
    fun `config with object`() {
        val obj: ConfigObject = deserializeConfig("a = 42, b { e = foo }", ConfigObject.serializer())

        assertEquals(42, obj.a)
        assertEquals("foo", obj.b.e)
        assertEquals(1.1f, obj.b.f)
    }

    @Test
    fun `config with list`() {
        val obj: WithList = deserializeConfig("a = 42, b = [1, 2, 3,]", WithList.serializer())

        assertEquals(42, obj.a)
        assertEquals(listOf(1, 2, 3), obj.b)
    }

    @Test
    fun `config with nested object`(){
        val obj: NestedObject = deserializeConfig("x = [{ a = 42 }, { a = 43 }, { a = 44 }]", NestedObject.serializer())

        assertEquals(listOf(42, 43, 44).map(::Simple), obj.x)
    }

    @Test
    fun `config with map`() {
        val obj: WithMap = deserializeConfig("x = { a = 42, b = 43, c = 44 }", WithMap.serializer())

        assertEquals(mapOf("a" to 42, "b" to 43, "c" to 44), obj.x)
    }
}