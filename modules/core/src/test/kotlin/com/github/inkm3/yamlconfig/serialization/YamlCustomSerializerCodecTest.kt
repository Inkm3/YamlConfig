package com.github.inkm3.yamlconfig.serialization

import com.github.inkm3.yamlconfig.serialization.CodecFixtures.integer
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.mapping
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.string
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class YamlCustomSerializerCodecTest {
    private data class Token(val text: String)
    private object TokenSerializer : KSerializer<Token> {
        override val descriptor = PrimitiveSerialDescriptor("Token", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: Token) = encoder.encodeString(value.text)
        override fun deserialize(decoder: Decoder): Token = Token(decoder.decodeString())
    }

    @Serializable
    private data class ContextualConfig(@Contextual val token: Token)

    @Serializable
    @JvmInline
    private value class Port(val value: Int)

    @Serializable
    private data class InlineConfig(val port: Port)

    @Serializable
    private sealed class Animal {
        @Serializable
        data class Dog(val name: String) : Animal()
    }

    private val format = YamlSerialization(serializersModule = SerializersModule {
        contextual(Token::class, TokenSerializer)
    })

    @Test
    fun explicitCustomSerializerRoundTrips() {
        val token = Token("abc")
        assertEquals(string("abc"), format.encodeToNode(TokenSerializer, token))
        assertEquals(token, format.decodeFromNode(TokenSerializer, string("abc")))
    }

    @Test
    fun reifiedRootLookupUsesTheConfiguredSerializersModule() {
        assertEquals(string("abc"), format.encodeToNode(Token("abc")))
        assertEquals(Token("abc"), format.decodeFromNode<Token>(string("abc")))
    }

    @Test
    fun contextualPropertyUsesTheSameModuleInBothDirections() {
        val value = ContextualConfig(Token("abc"))
        assertEquals(mapping("token" to string("abc")), format.encodeToNode(value))
        assertEquals(value, format.decodeFromNode<ContextualConfig>(format.encodeToNode(value)))
    }

    @Test
    fun ordinarySignedInlineValueClassRoundTrips() {
        assertEquals(integer(30000), format.encodeToNode(Port(30000)))
        assertEquals(Port(30000), format.decodeFromNode<Port>(integer(30000)))
        val value = InlineConfig(Port(30000))
        assertEquals(mapping("port" to integer(30000)), format.encodeToNode(value))
        assertEquals(value, format.decodeFromNode<InlineConfig>(format.encodeToNode(value)))
    }

    @Test
    fun unsignedTypesAreExplicitlyRejectedRatherThanReinterpretedAsSigned() {
        assertFailsWith<SerializationException> { format.encodeToNode(UInt.MAX_VALUE) }
        assertFailsWith<SerializationException> { format.decodeFromNode<UInt>(integer(-1)) }
    }

    @Test
    fun polymorphicSerializationIsNotAccidentallyTreatedAsAnOrdinaryObject() {
        assertFailsWith<SerializationException> { format.encodeToNode<Animal>(Animal.Dog("a")) }
        assertFailsWith<SerializationException> {
            format.decodeFromNode<Animal>(mapping("type" to string("Dog"), "value" to mapping("name" to string("a"))))
        }
    }
}
