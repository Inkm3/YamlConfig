package com.github.inkm3.yamlconfig.schema

import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.testsupport.*
import com.github.inkm3.yamlconfig.yamlConfig
import kotlin.test.*

class NullableAndObjectSchemaTest {
    class Config {
        var name: String = "factory-name"
        var port: Int = 25565
        var description: String? = "factory-description"
    }

    private val schema = yamlObject(::Config) {
        field("name", Config::name, str())
        field("port", Config::port, int())
        optionalField("description", Config::description, nullable(str()))
    }

    @Test fun nullableDistinguishesNullFromMissingAtDecodeLevel() {
        val nullable = nullable(str())
        assertNull(nullable.decode(n(), YamlPath.root()))
        assertEquals(n(), nullable.encode(null))
        assertEquals("x", nullable.decode(s("x"), YamlPath.root()))
    }

    @Test fun objectDecodeUsesFactoryValueForMissingOptionalField() {
        val decoded = schema.decode(stringMappingOf(
            "name" to s("server"),
            "port" to i(30000),
        ), YamlPath.root())
        assertEquals("server", decoded.name)
        assertEquals(30000, decoded.port)
        assertEquals("factory-description", decoded.description)
    }

    @Test fun objectDecodeRejectsMissingRequiredField() {
        val error = assertFailsWith<YamlSchemaException> {
            schema.decode(stringMappingOf("name" to s("server")), YamlPath.root())
        }
        assertEquals(YamlPath.root().child("port"), error.path)
    }

    @Test fun objectEncodeOmitsUnchangedOptionalFactoryValue() {
        val config = Config().apply {
            name = "server"
            port = 30000
        }
        val encoded = assertIs<YamlMappingNode>(schema.encode(config))
        assertFalse(encoded.containsKey("description"))
        assertEquals(s("server"), encoded["name"])
        assertEquals(i(30000), encoded["port"])
    }

    @Test fun loadMergesPartialDefaultsPerObjectField() {
        val engine = TestYamlEngine()
        val defaults = TestYamlSource(stringMappingOf(
            "name" to s("default-name"),
        ), "defaults")
        val user = TestYamlSource(stringMappingOf(
            "port" to i(30000),
        ), "user")

        val value = yamlConfig(engine, user, schema, defaults).load().value
        assertEquals("default-name", value.name)
        assertEquals(30000, value.port)
        assertEquals("factory-description", value.description)
    }

    @Test fun userStructuralErrorDoesNotFallbackToDefaults() {
        val engine = TestYamlEngine()
        val defaults = TestYamlSource(stringMappingOf(
            "name" to s("default-name"),
            "port" to i(25565),
        ))
        val user = TestYamlSource(stringMappingOf(
            "name" to sequenceOf(),
        ))

        assertFailsWith<YamlSchemaException> {
            yamlConfig(engine, user, schema, defaults).load()
        }
    }

    @Test fun explicitNullOverridesDefaultValue() {
        val defaults = TestYamlSource(stringMappingOf(
            "name" to s("default-name"),
            "port" to i(25565),
            "description" to s("default-description"),
        ))
        val user = TestYamlSource(stringMappingOf(
            "description" to n(),
        ))
        val value = yamlConfig(TestYamlEngine(), user, schema, defaults).load().value
        assertNull(value.description)
    }
}
