package pw.dotdash.configurate.serialization

import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.internal.TaggedDecoder
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.getContextualOrDefault
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.SimpleConfigurationNode
import ninja.leaping.configurate.Types

class ConfigurationNodeParser(override val context: SerialModule = EmptyModule) : SerialFormat {

    @ImplicitReflectionSerializer
    inline fun <reified T : Any> parse(node: ConfigurationNode): T =
        parse(node, context.getContextualOrDefault(T::class))

    fun <T> parse(node: ConfigurationNode, deserializer: DeserializationStrategy<T>): T =
        NodeReader(node).decode(deserializer)

    @OptIn(InternalSerializationApi::class)
    private abstract inner class NodeDecoder<T> : TaggedDecoder<T>() {
        override val context: SerialModule
            get() = this@ConfigurationNodeParser.context

        abstract fun getTaggedConfigurationNode(tag: T): ConfigurationNode

        private fun getCheckedString(node: ConfigurationNode): String =
            node.string ?: throw SerializationException("${node.key} could not be converted into a String.")

        private fun getCheckedInt(node: ConfigurationNode): Int =
            node.getValue<Int?>(Types::asInt, null)
                ?: throw SerializationException("${node.key} could not be converted into an Integer.")

        override fun decodeTaggedString(tag: T): String {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return getCheckedString(node)
        }

        override fun decodeTaggedChar(tag: T): Char {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            val str: String = getCheckedString(node)
            if (str.length != 1) {
                throw SerializationException("${node.key} could not be converted into a Char.")
            }
            return str[0]
        }

        override fun decodeTaggedBoolean(tag: T): Boolean {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return node.getValue<Boolean?>(Types::asBoolean, null)
                ?: throw SerializationException("${node.key} could not be converted into a Boolean.")
        }

        override fun decodeTaggedByte(tag: T): Byte {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            val int: Int = getCheckedInt(node)
            if (int !in Byte.MIN_VALUE..Byte.MAX_VALUE) {
                throw SerializationException("${node.key} could not be converted into a Byte.")
            }
            return int.toByte()
        }

        override fun decodeTaggedShort(tag: T): Short {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            val int: Int = getCheckedInt(node)
            if (int !in Short.MIN_VALUE..Short.MAX_VALUE) {
                throw SerializationException("${node.key} could not be converted into a Short.")
            }
            return int.toShort()
        }

        override fun decodeTaggedInt(tag: T): Int {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return getCheckedInt(node)
        }

        override fun decodeTaggedLong(tag: T): Long {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return node.getValue<Long?>(Types::asLong, null)
                ?: throw SerializationException("${node.key} could not be converted into a Long.")
        }

        override fun decodeTaggedFloat(tag: T): Float {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return node.getValue<Float?>(Types::asFloat, null)
                ?: throw SerializationException("${node.key} could not be converted into a Float.")
        }

        override fun decodeTaggedDouble(tag: T): Double {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return node.getValue<Double?>(Types::asDouble, null)
                ?: throw SerializationException("${node.key} could not be converted into a Double.")
        }

        override fun decodeTaggedEnum(tag: T, enumDescription: SerialDescriptor): Int {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            val name: String = getCheckedString(node)
            return enumDescription.getElementIndexOrThrow(name)
        }

        override fun decodeTaggedNotNullMark(tag: T): Boolean {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return !node.isVirtual && node.value != null
        }

        override fun decodeTaggedValue(tag: T): Any {
            val node: ConfigurationNode = getTaggedConfigurationNode(tag)
            return node.value
                ?: throw SerializationException("${node.key} could not be converted to Any (no value found).")
        }
    }

    private inner class NodeReader(val node: ConfigurationNode) : NodeDecoder<String>() {
        private var index: Int = -1

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            while (++index < descriptor.elementsCount) {
                val name: String = descriptor.getTag(index)
                if (!node.getNode(name).isVirtual) {
                    return index
                }
            }
            return READ_DONE
        }

        private fun composeName(parentName: String, childName: String): String =
            if (parentName.isEmpty()) childName else "$parentName.$childName"

        override fun SerialDescriptor.getTag(index: Int): String =
            composeName(currentTagOrNull ?: "", getElementName(index))

        override fun getTaggedConfigurationNode(tag: String): ConfigurationNode =
            this.node.getNode(tag.split('.'))

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder =
            when {
                descriptor.kind == StructureKind.LIST || descriptor is PolymorphicKind -> {
                    ListNodeReader(node.getNode(currentTag).childrenList)
                }
                descriptor.kind == StructureKind.CLASS || descriptor.kind == StructureKind.OBJECT -> {
                    if (index > -1) NodeReader(node.getNode(currentTag)) else this
                }
                descriptor.kind == StructureKind.MAP -> {
                    MapNodeReader(node.getNode(currentTag).childrenMap)
                }
                else -> this
            }
    }

    private inner class ListNodeReader(val children: List<ConfigurationNode>) : NodeDecoder<Int>() {
        private var index: Int = -1

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder =
            when {
                descriptor.kind == StructureKind.LIST || descriptor is PolymorphicKind -> {
                    ListNodeReader(children[currentTag].childrenList)
                }
                descriptor.kind == StructureKind.CLASS || descriptor.kind == StructureKind.OBJECT -> {
                    NodeReader(children[currentTag])
                }
                descriptor.kind == StructureKind.MAP -> {
                    MapNodeReader(children[currentTag].childrenMap)
                }
                else -> this
            }

        override fun SerialDescriptor.getTag(index: Int): Int = index

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            index++
            return if (index < children.size) index else READ_DONE
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

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder =
            when {
                descriptor.kind == StructureKind.LIST || descriptor is PolymorphicKind -> {
                    ListNodeReader(values[currentTag / 2].childrenList)
                }
                descriptor.kind == StructureKind.CLASS || descriptor.kind == StructureKind.OBJECT -> {
                    NodeReader(values[currentTag / 2])
                }
                descriptor.kind == StructureKind.MAP -> {
                    MapNodeReader(values[currentTag / 2].childrenMap)
                }
                else -> this
            }

        override fun SerialDescriptor.getTag(index: Int): Int = index

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            index++
            return if (index < indexSize) index else READ_DONE
        }

        override fun getTaggedConfigurationNode(tag: Int): ConfigurationNode {
            val index = tag / 2
            return if (tag % 2 == 0) SimpleConfigurationNode.root().apply { value = keys[index] } else values[index]
        }
    }

    companion object {
        @ImplicitReflectionSerializer
        inline fun <reified T : Any> parse(node: ConfigurationNode): T =
            ConfigurationNodeParser().parse(node)

        fun <T> parse(node: ConfigurationNode, deserializer: DeserializationStrategy<T>): T =
            ConfigurationNodeParser().parse(node, deserializer)
    }
}