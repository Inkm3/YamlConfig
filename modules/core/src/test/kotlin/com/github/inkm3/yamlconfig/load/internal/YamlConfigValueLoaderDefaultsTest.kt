package com.github.inkm3.yamlconfig.load.internal

import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.integer
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.load
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.mapping
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.string
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class YamlConfigValueLoaderDefaultsTest {
    @Serializable
    private data class RequiredConfig(val token: String)
    @Serializable
    private data class RequiredDefault(@Required val port: Int = 25565)
    @Serializable
    private data class RequiredChild(val database: LoadDatabase)
    @Serializable
    private data class Dependent(val base: Int = 10, val derived: Int = base + 1)
    @Serializable
    private object Singleton

    @Test
    fun emptyDocumentUsesRootConstructorDefaults() {
        assertEquals(LoadConfig(), load<LoadConfig>())
        assertSame(Singleton, load<Singleton>())
    }

    @Test
    fun userThenYamlThenKotlinDefaultPriority() {
        val defaults = mapping("server-name" to string("yaml"))
        assertEquals("user", load<LoadConfig>(defaults, mapping("server-name" to string("user"))).name)
        assertEquals("yaml", load<LoadConfig>(defaults, mapping()).name)
        assertEquals("kotlin-name", load<LoadConfig>(mapping(), mapping()).name)
    }

    @Test
    fun requiredPropertyCanBeAssembledFromEitherYaml() {
        assertEquals(RequiredConfig("abc"), load<RequiredConfig>(mapping("token" to string("abc")), mapping()))
        assertEquals(RequiredConfig("xyz"), load<RequiredConfig>(mapping(), mapping("token" to string("xyz"))))
    }

    @Test
    fun requiredPropertiesAreNotInventedIncludingRequiredDefaultAndChild() {
        assertFailsWith<SerializationException> { load<RequiredConfig>() }
        assertFailsWith<SerializationException> { load<RequiredDefault>() }
        assertFailsWith<SerializationException> { load<RequiredChild>() }
    }

    @Test
    fun missingParentAndPresentEmptyObjectKeepDifferentConstructorDefaults() {
        assertEquals(LoadDatabase("prod", 3306), load<LoadConfig>(user = mapping()).database)
        assertEquals(LoadDatabase("localhost", 3306),
            load<LoadConfig>(user = mapping("database" to mapping())).database)
    }

    @Test
    fun nestedYamlDefaultsMergeWithUserBeforeDecoding() {
        val defaults = mapping("database" to mapping("host" to string("yaml-host")))
        assertEquals(LoadDatabase("yaml-host", 5432),
            load<LoadConfig>(defaults, mapping("database" to mapping("port" to integer(5432)))).database)
        assertEquals(LoadDatabase("yaml-host", 3306),
            load<LoadConfig>(defaults, mapping("database" to mapping())).database)
    }

    @Test
    fun dependentConstructorDefaultUsesEffectiveSiblingNotEitherInputAlone() {
        assertEquals(Dependent(100, 101), load<Dependent>(mapping("base" to integer(100)), mapping()))
        assertEquals(Dependent(200, 201), load<Dependent>(mapping("base" to integer(100)),
            mapping("base" to integer(200))))
        assertEquals(Dependent(200, 999), load<Dependent>(mapping("derived" to integer(999)),
            mapping("base" to integer(200))))
    }

    @Test
    fun serializedNameIsUsedAndKotlinPropertyNameIsOnlyUnknownData() {
        val defaults = mapping("server-name" to string("yaml"))
        assertEquals("yaml", load<LoadConfig>(defaults, mapping("name" to string("not-an-alias"))).name)
    }

    @Test
    fun partialDefaultsNeedNotBeDecodableWithoutTheUser() {
        assertEquals(RequiredConfig("abc"), load<RequiredConfig>(mapping("extra" to string("x")),
            mapping("token" to string("abc"))))
    }
}
