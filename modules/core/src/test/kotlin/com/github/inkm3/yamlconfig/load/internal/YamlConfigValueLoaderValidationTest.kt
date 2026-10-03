package com.github.inkm3.yamlconfig.load.internal

import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.integer
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.load
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.mapping
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.sequence
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.string
import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YamlConfigValueLoaderValidationTest {
    @Test
    fun invalidKnownUserValueIsNotReplacedByADefault() {
        val error = assertFailsWith<SerializationException> {
            load<LoadConfig>(mapping("database" to mapping("port" to integer(3306))),
                mapping("database" to mapping("port" to string("bad"))))
        }
        assertTrue(error.message.orEmpty().endsWith(" at $.database.port"), error.message)
    }

    @Test
    fun invalidUserStructureIsNotReplacedByADefault() {
        val error = assertFailsWith<SerializationException> {
            load<LoadConfig>(mapping("database" to mapping()), mapping("database" to sequence()))
        }
        assertTrue(error.message.orEmpty().endsWith(" at $.database"), error.message)
    }

    @Test
    fun invalidSelectedDefaultStillFailsButShadowedDefaultIsNotDecoded() {
        val defaults = mapping("database" to mapping("port" to string("bad")))
        assertFailsWith<SerializationException> { load<LoadConfig>(defaults, mapping()) }
        assertEquals(5432, load<LoadConfig>(defaults,
            mapping("database" to mapping("port" to integer(5432)))).database.port)
    }

    @Test
    fun unknownPropertiesAreIgnoredWithoutMutatingEitherInput() {
        val defaults = mapping("server-name" to string("yaml"), "future-default" to sequence())
        val user = mapping("database" to mapping("extra" to string("keep")),
            "servers" to sequence(mapping("future" to integer(1))), "future-user" to mapping())
        val originals = defaults.toString() to user.toString()
        val value = load<LoadConfig>(defaults, user)
        assertEquals("yaml", value.name)
        assertEquals(listOf(LoadDatabase()), value.servers)
        assertEquals(originals, defaults.toString() to user.toString())
    }

    @Test
    fun configLoadingDoesNotChangeTheCallersStrictFormat() {
        val format = YamlSerialization(ignoreUnknownKeys = false, encodeDefaults = false)
        val user = mapping("unknown" to string("keep"))
        assertEquals(LoadConfig(), load<LoadConfig>(user = user, format = format))
        assertFalse(format.ignoreUnknownKeys)
        assertFalse(format.encodeDefaults)
        assertFailsWith<SerializationException> { format.decodeFromNode<LoadConfig>(user) }
    }

    @Test
    fun objectOverlayMustNotHideInvalidNonStringUserKeys() {
        val user = YamlMappingNode(mapOf(YamlMapKey("1", YamlScalarKind.INTEGER) to string("bad-key")))
        val defaults = mapping("server-name" to string("yaml"))
        assertFailsWith<SerializationException> { load<LoadConfig>(defaults, user) }
        assertFailsWith<SerializationException> { load<LoadConfig>(null, user) }
    }

    @Test
    fun errorsInsideAtomicCollectionsKeepTheFullPath() {
        val error = assertFailsWith<SerializationException> {
            load<LoadConfig>(user = mapping("servers" to sequence(mapping("port" to string("bad")))))
        }
        assertTrue(error.message.orEmpty().endsWith(" at $.servers[0].port"), error.message)
    }

    @Test
    fun wrongRootAndMissingRootFailAtRootPath() {
        val missing = assertFailsWith<SerializationException> { load<Int>() }
        assertTrue(missing.message.orEmpty().endsWith(" at $"), missing.message)
        assertFailsWith<SerializationException> { load<LoadConfig>(mapping(), sequence()) }
    }
}
