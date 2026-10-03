package com.github.inkm3.yamlconfig.load.internal

import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.integer
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.load
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.mapping
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.sequence
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.string
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class YamlConfigValueLoaderCollectionsTest {
    @Test
    fun listOverrideDoesNotInheritAnyDefaultElementOrField() {
        val defaults = mapping("servers" to sequence(mapping("host" to string("yaml")), mapping()))
        val user = mapping("servers" to sequence(mapping("port" to integer(5432))))
        assertEquals(listOf(LoadDatabase("localhost", 5432)), load<LoadConfig>(defaults, user).servers)
    }

    @Test
    fun mapOverrideDoesNotInheritAnyOtherEntry() {
        val defaults = mapping("ports" to mapping("a" to integer(1), "b" to integer(2)))
        val user = mapping("ports" to mapping("a" to integer(3)))
        assertEquals(mapOf("a" to 3), load<LoadConfig>(defaults, user).ports)
    }

    @Test
    fun emptyCollectionsAreExplicitOverrides() {
        val defaults = mapping("ports" to mapping("a" to integer(1)), "servers" to sequence(mapping()))
        val user = mapping("ports" to mapping(), "servers" to sequence())
        assertEquals(emptyMap(), load<LoadConfig>(defaults, user).ports)
        assertEquals(emptyList(), load<LoadConfig>(defaults, user).servers)
    }

    @Test
    fun absentCollectionInheritsAsAWhole() {
        val defaults = mapping("ports" to mapping("a" to integer(1)),
            "servers" to sequence(mapping("host" to string("yaml"))))
        val value = load<LoadConfig>(defaults, mapping())
        assertEquals(mapOf("a" to 1), value.ports)
        assertEquals(listOf(LoadDatabase("yaml", 3306)), value.servers)
    }

    @Test
    fun scalarAndCollectionRootsUseExplicitValuesWithoutInventedDefaults() {
        assertEquals(3, load<Int>(integer(1), integer(3)))
        assertEquals(listOf(1), load<List<Int>>(sequence(integer(1)), null))
        assertEquals(emptyList(), load<List<Int>>(sequence(integer(1)), sequence()))
        assertEquals(emptyMap(), load<Map<String, Int>>(mapping("a" to integer(1)), mapping()))
        assertFailsWith<SerializationException> { load<Int>() }
        assertFailsWith<SerializationException> { load<List<Int>>() }
        assertFailsWith<SerializationException> { load<Map<String, Int>>() }
    }

    @Test
    fun mapValuesAreDataNotObjectOverlayTargets() {
        val defaults = mapping("a" to mapping("host" to string("yaml")))
        val user = mapping("a" to mapping("port" to integer(5432)))
        assertEquals(mapOf("a" to LoadDatabase("localhost", 5432)),
            load<Map<String, LoadDatabase>>(defaults, user))
    }
}
