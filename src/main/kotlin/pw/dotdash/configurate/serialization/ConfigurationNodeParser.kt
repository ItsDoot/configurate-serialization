package pw.dotdash.configurate.serialization

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.internal.TaggedDecoder
import kotlinx.serialization.modules.*
import org.spongepowered.configurate.BasicConfigurationNode
import org.spongepowered.configurate.ConfigurationNode

class ConfigurationNodeParser(override val serializersModule: SerializersModule = EmptySerializersModule) :
    SerialFormat {

    @OptIn(UnsafeSerializationApi::class)
    inline fun <reified T : Any> parse(node: ConfigurationNode): T =
        parse(node, serializersModule.getContextual(T::class) ?: T::class.serializer())

    fun <T> parse(node: ConfigurationNode, deserializer: DeserializationStrategy<T>): T =
        NodeReader(node).decodeSerializableValue(deserializer)

    @OptIn(InternalSerializationApi::class)
    private abstract inner class NodeDecoder<T> : TaggedDecoder<T>() {
        override val serializersModule: SerializersModule
            get() = this@ConfigurationNodeParser.serializersModule

        abstract fun getTaggedConfigurationNode(tag: T): ConfigurationNode

        private fun getCheckedString(node: ConfigurationNode): String =
            node.string ?: throw SerializationException("${node.key()} could not be converted into a String.")

        private fun getCheckedInt(node: ConfigurationNode): Int =
            node.get(Int::class.java)
                ?: throw SerializationException("${node.key()} could not be converted into an Integer.")

        override fun decodeTaggedString(tag: T): String {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return getCheckedString(node)
        }

        override fun decodeTaggedChar(tag: T): Char {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            val str: String = getCheckedString(node)
            if (str.length != 1) {
                throw SerializationException("${node.key()} could not be converted into a Char.")
            }
            return str[0]
        }

        override fun decodeTaggedBoolean(tag: T): Boolean {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return node.get(Boolean::class.java)
                ?: throw SerializationException("${node.key()} could not be converted into a Boolean.")
        }

        override fun decodeTaggedByte(tag: T): Byte {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            val int: Int = getCheckedInt(node)
            if (int !in Byte.MIN_VALUE..Byte.MAX_VALUE) {
                throw SerializationException("${node.key()} could not be converted into a Byte.")
            }
            return int.toByte()
        }

        override fun decodeTaggedShort(tag: T): Short {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            val int: Int = getCheckedInt(node)
            if (int !in Short.MIN_VALUE..Short.MAX_VALUE) {
                throw SerializationException("${node.key()} could not be converted into a Short.")
            }
            return int.toShort()
        }

        override fun decodeTaggedInt(tag: T): Int {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return getCheckedInt(node)
        }

        override fun decodeTaggedLong(tag: T): Long {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return node.get(Long::class.java)
                ?: throw SerializationException("${node.key()} could not be converted into a Long.")
        }

        override fun decodeTaggedFloat(tag: T): Float {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return node.get(Float::class.java)
                ?: throw SerializationException("${node.key()} could not be converted into a Float.")
        }

        override fun decodeTaggedDouble(tag: T): Double {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return node.get(Double::class.java)
                ?: throw SerializationException("${node.key()} could not be converted into a Double.")
        }

        override fun decodeTaggedEnum(tag: T, enumDescription: SerialDescriptor): Int {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            val name: String = getCheckedString(node)
            return enumDescription.getElementIndex(name)
        }

        override fun decodeTaggedNotNullMark(tag: T): Boolean {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return !node.virtual() && node.raw() != null
        }

        override fun decodeTaggedValue(tag: T): Any {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return node.raw()
                ?: throw SerializationException("${node.key()} could not be converted to Any (no value found).")
        }
    }

    private inner class NodeReader(val node: ConfigurationNode) : NodeDecoder<String>() {
        private var index: Int = -1

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            while (++index < descriptor.elementsCount) {
                val name: String = descriptor.getTag(index)
                if (!node.node(name).virtual()) {
                    return index
                }
            }
            return CompositeDecoder.DECODE_DONE
        }

        private fun composeName(parentName: String, childName: String): String =
            if (parentName.isEmpty()) childName else "$parentName.$childName"

        @OptIn(InternalSerializationApi::class)
        override fun SerialDescriptor.getTag(index: Int): String =
            composeName(currentTagOrNull ?: "", getElementName(index))

        override fun getTaggedConfigurationNode(tag: String): ConfigurationNode =
            this.node.node(tag.split('.'))

        @OptIn(InternalSerializationApi::class)
        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder =
            when {
                descriptor.kind == StructureKind.LIST || descriptor is PolymorphicKind -> {
                    ListNodeReader(node.node(currentTag).childrenList())
                }
                descriptor.kind == StructureKind.CLASS || descriptor.kind == StructureKind.OBJECT -> {
                    if (index > -1) NodeReader(node.node(currentTag)) else this
                }
                descriptor.kind == StructureKind.MAP -> {
                    MapNodeReader(node.node(currentTag).childrenMap())
                }
                else -> this
            }
    }

    private inner class ListNodeReader(val children: List<ConfigurationNode>) : NodeDecoder<Int>() {
        private var index: Int = -1

        @OptIn(InternalSerializationApi::class)
        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder =
            when {
                descriptor.kind == StructureKind.LIST || descriptor is PolymorphicKind -> {
                    ListNodeReader(children[currentTag].childrenList())
                }
                descriptor.kind == StructureKind.CLASS || descriptor.kind == StructureKind.OBJECT -> {
                    NodeReader(children[currentTag])
                }
                descriptor.kind == StructureKind.MAP -> {
                    MapNodeReader(children[currentTag].childrenMap())
                }
                else -> this
            }

        @OptIn(InternalSerializationApi::class)
        override fun SerialDescriptor.getTag(index: Int): Int = index

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            index++
            return if (index < children.size) index else CompositeDecoder.DECODE_DONE
        }

        override fun getTaggedConfigurationNode(tag: Int): ConfigurationNode = children[tag]
    }

    private inner class MapNodeReader(children: Map<Any, ConfigurationNode>) : NodeDecoder<Int>() {

        private var index: Int = -1
        private val keys: List<String>
        private val values: List<ConfigurationNode>

        init {
            val entries = children.entries.toList()
            keys = entries.map { it.key.toString() }
            values = entries.map(Map.Entry<Any, ConfigurationNode>::value)
        }

        private val indexSize = values.size * 2

        @OptIn(InternalSerializationApi::class)
        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder =
            when {
                descriptor.kind == StructureKind.LIST || descriptor is PolymorphicKind -> {
                    ListNodeReader(values[currentTag / 2].childrenList())
                }
                descriptor.kind == StructureKind.CLASS || descriptor.kind == StructureKind.OBJECT -> {
                    NodeReader(values[currentTag / 2])
                }
                descriptor.kind == StructureKind.MAP -> {
                    MapNodeReader(values[currentTag / 2].childrenMap())
                }
                else -> this
            }

        @OptIn(InternalSerializationApi::class)
        override fun SerialDescriptor.getTag(index: Int): Int = index

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            index++
            return if (index < indexSize) index else CompositeDecoder.DECODE_DONE
        }

        override fun getTaggedConfigurationNode(tag: Int): ConfigurationNode {
            val index = tag / 2
            return if (tag % 2 == 0) BasicConfigurationNode.root()
                .apply { this[String::class.java] = keys[index] } else values[index]
        }
    }

    companion object {
        inline fun <reified T : Any> parse(node: ConfigurationNode): T =
            ConfigurationNodeParser().parse(node)

        fun <T> parse(node: ConfigurationNode, deserializer: DeserializationStrategy<T>): T =
            ConfigurationNodeParser().parse(node, deserializer)
    }
}
