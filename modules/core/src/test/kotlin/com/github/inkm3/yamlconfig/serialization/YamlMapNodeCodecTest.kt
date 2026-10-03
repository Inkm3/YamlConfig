package com.github.inkm3.yamlconfig.serialization

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.integer
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.mapping
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.nullNode
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.roundTrip
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.string
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class YamlMapNodeCodecTest {
    @Serializable
    private enum class Key { @SerialName("first-key") FIRST }

    private data class CollidingKey(val id: Int)
    private object CollidingKeySerializer : KSerializer<CollidingKey> {
        override val descriptor = PrimitiveSerialDescriptor("CollidingKey", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: CollidingKey) = encoder.encodeString("same")
        override fun deserialize(decoder: Decoder): CollidingKey {
            decoder.decodeString()
            return CollidingKey(0)
        }
    }

    private val format = YamlSerialization.Default

    @Test
    fun stringKeysRoundTrip() {
        roundTrip(linkedMapOf("lobby" to 25565, "survival" to 25566), mapping("lobby" to integer(25565), "survival" to integer(25566)))
    }

    @Test
    fun numericKeysKeepTheirYamlKinds() {
        val node = YamlMappingNode(linkedMapOf(YamlMapKey("1", YamlScalarKind.INTEGER) to string("one")))
        roundTrip(mapOf(1 to "one"), node)
        assertEquals(mapOf(1 to "one"), format.decodeFromNode<Map<Int, String>>(
            YamlMappingNode(mapOf(YamlMapKey("0x1", YamlScalarKind.INTEGER) to string("one"))),
        ))
    }

    @Test
    fun booleanAndEnumKeysAreScalarKeys() {
        roundTrip(mapOf(true to 1), YamlMappingNode(mapOf(YamlMapKey("true", YamlScalarKind.BOOLEAN) to integer(1))))
        roundTrip(mapOf(Key.FIRST to 1), mapping("first-key" to integer(1)))
    }

    @Test
    fun nullableKeysAndValuesRemainExplicit() {
        val node = YamlMappingNode(linkedMapOf(YamlMapKey("null", YamlScalarKind.NULL) to nullNode(), YamlMapKey("x") to string("y")))
        roundTrip(mapOf<String?, String?>(null to null, "x" to "y"), node)
    }

    @Test
    fun structuredKeysAreRejectedInsteadOfStringified() {
        assertFailsWith<SerializationException> { format.encodeToNode(mapOf(listOf(1, 2) to "invalid")) }
        assertFailsWith<SerializationException> { format.decodeFromNode<Map<List<Int>, String>>(mapping("x" to string("invalid"))) }
    }

    @Test
    fun scalarKeyKindIsNotCoercedToTheRequestedKotlinType() {
        assertFailsWith<SerializationException> { format.decodeFromNode<Map<Int, String>>(mapping("1" to string("one"))) }
    }

    @Test
    fun rawKeysThatDecodeToTheSameIntegerAreRejected() {
        val node = YamlMappingNode(linkedMapOf(
            YamlMapKey("1", YamlScalarKind.INTEGER) to string("first"),
            YamlMapKey("+1", YamlScalarKind.INTEGER) to string("second"),
        ))
        assertFailsWith<SerializationException> { format.decodeFromNode<Map<Int, String>>(node) }
    }

    @Test
    fun differentNullSpellingsCannotOverwriteTheSameDecodedKey() {
        val node = YamlMappingNode(linkedMapOf(
            YamlMapKey("null", YamlScalarKind.NULL) to string("first"),
            YamlMapKey("~", YamlScalarKind.NULL) to string("second"),
        ))
        assertFailsWith<SerializationException> { format.decodeFromNode<Map<String?, String>>(node) }
    }

    @Test
    fun customKeySerializerCannotSilentlyCollapseTwoEncodedKeys() {
        val serializer = MapSerializer(CollidingKeySerializer, String.serializer())
        assertFailsWith<SerializationException> {
            format.encodeToNode(serializer, linkedMapOf(CollidingKey(1) to "a", CollidingKey(2) to "b"))
        }
    }

    @Test
    fun customDecodedKeyCollisionIsDetected() {
        val serializer = MapSerializer(CollidingKeySerializer, String.serializer())
        assertFailsWith<SerializationException> { format.decodeFromNode(serializer, mapping("a" to string("x"), "b" to string("y"))) }
    }
}
