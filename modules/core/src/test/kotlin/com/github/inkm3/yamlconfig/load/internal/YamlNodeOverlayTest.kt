package com.github.inkm3.yamlconfig.load.internal

import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.integer
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.mapping
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.nullNode
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.sequence
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.string
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class YamlNodeOverlayTest {
    private val descriptor = serializer<LoadConfig>().descriptor

    @Serializable
    private data class Recursive(val next: Recursive? = null)

    @Test
    fun bothMissingStayMissing() {
        assertNull(YamlNodeOverlay.overlay(descriptor, null, null))
    }

    @Test
    fun singleInputIsReturnedWithoutCopying() {
        val node = mapping("server-name" to string("user"))
        assertSame(node, YamlNodeOverlay.overlay(descriptor, null, node))
        assertSame(node, YamlNodeOverlay.overlay(descriptor, node, null))
        assertSame(node, YamlNodeOverlay.overlay(descriptor, node, node))
    }

    @Test
    fun scalarUsesUserEvenWhenInvalidForDescriptor() {
        val user = string("invalid")
        assertSame(user, YamlNodeOverlay.overlay(serializer<Int>().descriptor, integer(1), user))
    }

    @Test
    fun explicitNullIsNotMissing() {
        val user = nullNode()
        assertSame(user, YamlNodeOverlay.overlay(descriptor, mapping(), user))
    }

    @Test
    fun collectionRootsAreAtomic() {
        val userMap = mapping()
        val userList = sequence()
        assertSame(userMap, YamlNodeOverlay.overlay(serializer<Map<String, Int>>().descriptor,
            mapping("a" to integer(1)), userMap))
        assertSame(userList, YamlNodeOverlay.overlay(serializer<List<Int>>().descriptor,
            sequence(integer(1)), userList))
    }

    @Test
    fun nestedObjectsMergeOnlyKnownSerializedProperties() {
        val defaults = mapping("server-name" to string("default"),
            "database" to mapping("host" to string("db"), "port" to integer(3306)))
        val user = mapping("database" to mapping("port" to integer(5432)))
        assertEquals(mapping("server-name" to string("default"),
            "database" to mapping("host" to string("db"), "port" to integer(5432))),
            YamlNodeOverlay.overlay(descriptor, defaults, user))
    }

    @Test
    fun emptyObjectInheritsFieldsButDoesNotInventAbsentChildren() {
        val defaults = mapping("database" to mapping("host" to string("db")))
        val user = mapping("database" to mapping())
        assertEquals(defaults, YamlNodeOverlay.overlay(descriptor, defaults, user))
        val empty = mapping()
        assertSame(empty, YamlNodeOverlay.overlay(descriptor, mapping(), empty))
    }

    @Test
    fun noInheritanceReusesUserMapping() {
        val user = mapping("server-name" to string("user"))
        assertSame(user, YamlNodeOverlay.overlay(descriptor,
            mapping("server-name" to string("default")), user))
    }

    @Test
    fun copyOnWriteRetainsUntouchedChildrenAndOriginalInputs() {
        val servers = sequence(mapping("host" to string("keep")))
        val userDb = mapping("port" to integer(5432))
        val user = mapping("database" to userDb, "servers" to servers)
        val defaults = mapping("database" to mapping("host" to string("db")))
        val beforeUser = user.toString()
        val beforeDefaults = defaults.toString()
        val result = assertIs<YamlMappingNode>(YamlNodeOverlay.overlay(descriptor, defaults, user))
        assertNotSame(user, result)
        assertNotSame(userDb, result["database"])
        assertSame(servers, result["servers"])
        assertEquals(beforeUser, user.toString())
        assertEquals(beforeDefaults, defaults.toString())
    }

    @Test
    fun unknownUserDataSurvivesKnownPropertyInheritance() {
        val unknown = sequence(string("plugin-data"))
        val user = mapping("plugin-option" to unknown)
        val result = assertIs<YamlMappingNode>(YamlNodeOverlay.overlay(descriptor,
            mapping("server-name" to string("default")), user))
        assertSame(unknown, result["plugin-option"])
        assertEquals(string("default"), result["server-name"])
    }

    @Test
    fun wrongUserContainerIsNeverRepairedFromDefaults() {
        val user = sequence()
        assertSame(user, YamlNodeOverlay.overlay(descriptor, mapping(), user))
    }

    @Test
    fun recursiveDescriptorsDoNotMaterializeAnInfiniteObjectTree() {
        val user = mapping()
        assertSame(user, YamlNodeOverlay.overlay(serializer<Recursive>().descriptor, mapping(), user))
    }
}
