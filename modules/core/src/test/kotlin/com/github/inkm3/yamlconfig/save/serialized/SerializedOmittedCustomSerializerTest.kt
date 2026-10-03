package com.github.inkm3.yamlconfig.save.serialized

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.testsupport.TestYamlSource
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.modules.SerializersModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SerializedOmittedCustomSerializerTest {
    // No model annotations: use the existing explicit serializer/module APIs.
    private data class Config(val port: Int = 25565, val base: Int = 10, val derived: Int = base + 1)
    private object Codec : KSerializer<Config> {
        override val descriptor = buildClassSerialDescriptor("ManualOmittedConfig") {
            element<Int>("port", isOptional = true)
            element<Int>("base", isOptional = true)
            element<Int>("derived", isOptional = true)
        }
        override fun serialize(encoder: Encoder, value: Config) = encoder.encodeStructure(descriptor) {
            if (value.port != 25565) encodeIntElement(descriptor, 0, value.port)
            encodeIntElement(descriptor, 1, value.base)
            encodeIntElement(descriptor, 2, value.derived)
        }
        override fun deserialize(decoder: Decoder): Config = decoder.decodeStructure(descriptor) {
            var port = 25565
            var base = 10
            var derived: Int? = null
            while (true) when (val index = decodeElementIndex(descriptor)) {
                0 -> port = decodeIntElement(descriptor, 0)
                1 -> base = decodeIntElement(descriptor, 1)
                2 -> derived = decodeIntElement(descriptor, 2)
                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unexpected index: $index")
            }
            Config(port, base, derived ?: (base + 1))
        }
    }

    @Test fun explicitSerializerCanPreserveAnOptionalOmissionWithoutModelAnnotations() {
        for (mode in YamlSaveMode.entries) {
            val f = SaveFixture(stringMappingOf("port" to i(25565), "base" to i(100)))
            val config = yamlConfig(f.engine, f.source, Codec,
                defaultsSource = TestYamlSource(stringMappingOf("port" to i(30000))), saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(base = 10)
            session.save()
            assertEquals(Config(25565, 10, 101), config.load().value)
        }
    }

    @Test fun moduleRegistrationStillUsesTheSuppliedSerializerDuringCandidateValidation() {
        val f = SaveFixture(stringMappingOf("port" to i(25565), "base" to i(100)))
        val format = YamlSerialization(serializersModule = SerializersModule { contextual(Config::class, Codec) })
        val config = yamlConfig<Config>(f.engine, f.source, serialization = format,
            defaultsSource = TestYamlSource(stringMappingOf("port" to i(30000))))
        val session = config.load()
        session.value = session.value.copy(base = 10)
        session.save()
        assertEquals(Config(25565, 10, 101), config.load().value)
    }

    @Test fun arbitrarySerializerOmissionsDoNotExposeValuesForThePlannerToInvent() {
        val original = stringMappingOf("port" to i(40000))
        val f = SaveFixture(original)
        val config = yamlConfig(f.engine, f.source, Codec,
            defaultsSource = TestYamlSource(stringMappingOf("port" to i(30000))))
        val session = config.load()
        session.value = Config()
        assertFailsWith<SerializationException> { session.save() }
        assertEquals(original, f.source.rootNode)
        assertEquals(0, f.writes)
    }
}
