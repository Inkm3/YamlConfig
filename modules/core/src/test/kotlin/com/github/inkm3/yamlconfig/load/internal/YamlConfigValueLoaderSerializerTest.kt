package com.github.inkm3.yamlconfig.load.internal

import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.integer
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.load
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.mapping
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.string
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import kotlinx.serialization.Contextual
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

@OptIn(ExperimentalSerializationApi::class)
class YamlConfigValueLoaderSerializerTest {
    private data class Endpoint(val host: String, val port: Int)
    @Serializable
    private data class ContextConfig(@Contextual val endpoint: Endpoint)
    @Serializable
    @JvmInline
    private value class WrappedDatabase(val value: LoadDatabase)
    @Serializable
    private data class Annotated(@EncodeDefault(EncodeDefault.Mode.NEVER) val port: Int = 1)

    private class EndpointSerializer : KSerializer<Endpoint> {
        override val descriptor = serializer<LoadDatabase>().descriptor
        override fun deserialize(decoder: Decoder): Endpoint {
            val value = decoder.decodeSerializableValue(serializer<LoadDatabase>())
            return Endpoint(value.host, value.port)
        }
        override fun serialize(encoder: Encoder, value: Endpoint): Unit = error("Load must not encode")
    }

    @Test
    fun contextualModuleIsPreservedWhenConfigDecodingIgnoresUnknowns() {
        val format = YamlSerialization(serializersModule = SerializersModule {
            contextual(Endpoint::class, EndpointSerializer())
        })
        val user = mapping("endpoint" to mapping("host" to string("db"), "future" to integer(1)))
        assertEquals(ContextConfig(Endpoint("db", 3306)), load<ContextConfig>(user = user, format = format))
    }

    @Test
    fun contextualPropertiesAreAtomicWithoutGuessingTheirRuntimeShape() {
        val format = YamlSerialization(serializersModule = SerializersModule {
            contextual(Endpoint::class, EndpointSerializer())
        })
        val defaults = mapping("endpoint" to mapping("host" to string("yaml")))
        val user = mapping("endpoint" to mapping("port" to integer(5432)))
        assertEquals(ContextConfig(Endpoint("localhost", 5432)), load<ContextConfig>(defaults, user, format))
    }

    @Test
    fun inlineValuesAreAtomicEvenWhenTheirRepresentationIsAMapping() {
        val defaults = mapping("host" to string("yaml"))
        val user = mapping("port" to integer(5432))
        assertSame(user, YamlNodeOverlay.overlay(serializer<WrappedDatabase>().descriptor, defaults, user))
        assertEquals(WrappedDatabase(LoadDatabase("localhost", 5432)), load<WrappedDatabase>(defaults, user))
        assertFailsWith<SerializationException> { load<WrappedDatabase>() }
    }

    @Test
    fun readOnlyDeserializerDoesNotRequireAnEncodingStrategy() {
        val deserializer = object : DeserializationStrategy<String> {
            override val descriptor = PrimitiveSerialDescriptor("ReadOnly", PrimitiveKind.STRING)
            override fun deserialize(decoder: Decoder): String = "decoded:" + decoder.decodeString()
        }
        val loader = YamlConfigValueLoader(deserializer, YamlSerialization.Default)
        assertEquals("decoded:user", loader.load(string("default"), string("user")))
    }

    @Test
    fun decodeOccursExactlyOnceAfterOverlayAndNeverEncodes() {
        var count = 0
        val deserializer = object : DeserializationStrategy<LoadConfig> {
            override val descriptor = serializer<LoadConfig>().descriptor
            override fun deserialize(decoder: Decoder): LoadConfig {
                count++
                return decoder.decodeSerializableValue(serializer<LoadConfig>())
            }
        }
        val value = YamlConfigValueLoader(deserializer, YamlSerialization.Default).load(
            mapping("server-name" to string("yaml")), mapping("database" to mapping("port" to integer(5432))),
        )
        assertEquals("yaml", value.name)
        assertEquals(5432, value.database.port)
        assertEquals(1, count)
    }

    @Test
    fun encodingOptionsAndAnnotationsDoNotNormalizeAwayInputValues() {
        val format = YamlSerialization(encodeDefaults = false)
        assertEquals(Annotated(1), load<Annotated>(mapping("port" to integer(2)),
            mapping("port" to integer(1)), format))
        assertEquals(Annotated(2), load<Annotated>(mapping("port" to integer(2)), mapping(), format))
    }

    @Test
    fun reusedLoaderDoesNotRetainInputStateBetweenCalls() {
        val loader = YamlConfigValueLoader(serializer<LoadConfig>(), YamlSerialization.Default)
        repeat(20) { index ->
            assertEquals("server-$index", loader.load(null, mapping("server-name" to string("server-$index"))).name)
            assertEquals(LoadConfig(), loader.load(null, null))
        }
    }
}
