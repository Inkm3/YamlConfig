package com.github.inkm3.yamlconfig.save.serialized

import com.github.inkm3.yamlconfig.YamlConfig
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import com.github.inkm3.yamlconfig.yamlConfig
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
import kotlin.test.assertTrue

class SerializedConfigSessionTest {
    @Serializable
    private data class MutableConfig(var port: Int = 1, val names: MutableList<String> = mutableListOf())
    private data class Wrapped(val value: Int)

    @Test fun immutablePublicConfigLoadsAndSavesThroughSerializer() {
        val f = SaveFixture(stringMappingOf("server-name" to s("user"), "port" to i(1)))
        val config: YamlConfig<SavedConfig> = yamlConfig(f.engine, f.source)
        val session = config.load()
        assertEquals("user", session.value.name)
        session.value = session.value.copy(port = 2)
        session.save()
        assertEquals(2, config.load().value.port)
        assertEquals(1, f.writes)
    }

    @Test fun explicitSerializerOverloadIsUsable() {
        val f = SaveFixture(i(1))
        val config = yamlConfig(f.engine, f.source, serializer<Int>())
        val session = config.load()
        session.value = 2
        session.save()
        assertEquals(2, config.load().value)
    }

    @Test fun unchangedSaveDoesNotWriteAndSecondSaveIsNoOp() {
        val f = SaveFixture(stringMappingOf("port" to i(1)))
        val session = yamlConfig<SavedConfig>(f.engine, f.source).load()
        session.save()
        assertEquals(0, f.writes)
        session.value = session.value.copy(port = 2)
        session.save()
        session.save()
        assertEquals(1, f.writes)
    }

    @Test fun mutablePropertiesAndCollectionsUseDetachedBaseline() {
        val f = SaveFixture(null)
        val config = yamlConfig<MutableConfig>(f.engine, f.source)
        val session = config.load()
        session.value.port = 2
        session.value.names.add("a")
        session.save()
        assertEquals(MutableConfig(2, mutableListOf("a")), config.load().value)
        session.value.names.add("b")
        session.save()
        assertEquals(listOf("a", "b"), config.load().value.names)
    }

    @Test fun writeFailureDoesNotAcceptBaselineAndCanBeRetried() {
        val original = stringMappingOf("port" to i(1))
        val f = SaveFixture(original)
        val config = yamlConfig<SavedConfig>(f.engine, f.source)
        val session = config.load()
        session.value = session.value.copy(port = 2)
        f.delegate.failNextWrite = true
        assertFailsWith<IllegalStateException> { session.save() }
        assertEquals(original, f.source.rootNode)
        session.save()
        assertEquals(2, config.load().value.port)
        session.save()
        assertEquals(2, f.writes)
    }

    @Test fun revertingAfterFailedWriteLeavesOriginalStateAndDoesNotWrite() {
        val f = SaveFixture(stringMappingOf("port" to i(1)))
        val session = yamlConfig<SavedConfig>(f.engine, f.source).load()
        val initial = session.value
        session.value = initial.copy(port = 2)
        f.delegate.failNextWrite = true
        assertFailsWith<IllegalStateException> { session.save() }
        session.value = initial
        session.save()
        assertEquals(1, f.writes)
    }

    @Test fun serializationFailureNeverTouchesOutput() {
        val f = SaveFixture(stringMappingOf("port" to i(1)))
        var reject = false
        val generated = serializer<SavedConfig>()
        val custom = object : KSerializer<SavedConfig> by generated {
            override fun serialize(encoder: Encoder, value: SavedConfig) {
                if (reject) throw SerializationException("refused")
                generated.serialize(encoder, value)
            }
        }
        val session = yamlConfig(f.engine, f.source, custom).load()
        session.value = session.value.copy(port = 2)
        reject = true
        assertFailsWith<SerializationException> { session.save() }
        assertEquals(0, f.writes)
        reject = false
        session.save()
        assertEquals(1, f.writes)
    }

    @Test fun moduleAwareReifiedOverloadRetainsCustomSerializer() {
        val custom = object : KSerializer<Wrapped> {
            override val descriptor = PrimitiveSerialDescriptor("Wrapped", PrimitiveKind.INT)
            override fun serialize(encoder: Encoder, value: Wrapped) = encoder.encodeInt(value.value)
            override fun deserialize(decoder: Decoder): Wrapped = Wrapped(decoder.decodeInt())
        }
        val format = YamlSerialization(SerializersModule { contextual(Wrapped::class, custom) })
        val f = SaveFixture(i(1))
        val config = yamlConfig<Wrapped>(f.engine, f.source, serialization = format)
        val session = config.load()
        session.value = Wrapped(2)
        session.save()
        assertEquals(Wrapped(2), config.load().value)
    }

    @Test fun knownUpdateDoesNotDeleteUnknownRootOrChildEntries() {
        val unknown = stringMappingOf("plugin" to s("keep"))
        val f = SaveFixture(stringMappingOf("future" to unknown,
            "database" to stringMappingOf("host" to s("db"), "port" to i(1), "plugin" to s("x"))))
        val session = yamlConfig<SavedConfig>(f.engine, f.source).load()
        session.value = session.value.copy(database = session.value.database.copy(port = 2))
        session.save()
        val text = f.source.rootNode.toString()
        assertTrue(text.contains("keep"), text)
        assertTrue(text.contains("plugin"), text)
        assertTrue(text.contains("value=x"), text)
    }
}
