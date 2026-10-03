package com.github.inkm3.yamlconfig.load.internal

import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.integer
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.load
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.mapping
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.nullNode
import com.github.inkm3.yamlconfig.load.internal.LoadFixtures.string
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class YamlConfigValueLoaderNullTest {
    @Serializable
    private data class NullableConfig(val database: LoadDatabase? = null)
    @Serializable
    private data class RequiredNullable(val message: String?)

    @Test
    fun explicitScalarNullOverridesYamlAndKotlinDefaults() {
        val defaults = mapping("message" to string("yaml"))
        assertEquals("yaml", load<LoadConfig>(defaults, mapping()).message)
        assertNull(load<LoadConfig>(defaults, mapping("message" to nullNode())).message)
        assertNull(load<LoadConfig>(mapping("message" to nullNode()), mapping()).message)
    }

    @Test
    fun nullableObjectDistinguishesMissingNullAndEmpty() {
        assertNull(load<NullableConfig>().database)
        assertNull(load<NullableConfig>(user = mapping("database" to nullNode())).database)
        assertEquals(LoadDatabase(), load<NullableConfig>(user = mapping("database" to mapping())).database)
    }

    @Test
    fun nullableObjectMergesOnlyWhenBothValuesAreMappings() {
        val defaults = mapping("database" to mapping("host" to string("yaml")))
        assertEquals(LoadDatabase("yaml", 5432),
            load<NullableConfig>(defaults, mapping("database" to mapping("port" to integer(5432)))).database)
        assertNull(load<NullableConfig>(defaults, mapping("database" to nullNode())).database)
        assertEquals(LoadDatabase(), load<NullableConfig>(mapping("database" to nullNode()),
            mapping("database" to mapping())).database)
    }

    @Test
    fun nullableDoesNotMakeARequiredFieldOptional() {
        assertFailsWith<SerializationException> { load<RequiredNullable>() }
        assertEquals(RequiredNullable(null), load<RequiredNullable>(user = mapping("message" to nullNode())))
    }

    @Test
    fun missingRootIsNotAnExplicitNullOrAnInventedNullableObject() {
        assertNull(load<Int?>(integer(1), nullNode()))
        assertNull(load<LoadConfig?>(mapping(), nullNode()))
        assertFailsWith<SerializationException> { load<Int?>() }
        assertFailsWith<SerializationException> { load<LoadConfig?>() }
        assertFailsWith<SerializationException> { load<LoadConfig>(mapping(), nullNode()) }
    }
}
