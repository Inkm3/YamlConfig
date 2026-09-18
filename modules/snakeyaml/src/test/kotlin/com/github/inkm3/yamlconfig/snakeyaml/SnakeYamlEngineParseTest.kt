package com.github.inkm3.yamlconfig.snakeyaml

import com.github.inkm3.yamlconfig.exception.YamlParseException
import com.github.inkm3.yamlconfig.node.*
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import kotlin.test.*

class SnakeYamlEngineParseTest {
    private val engine = SnakeYamlEngine()

    @Test
    fun emptyInputProducesEmptyDocument() {
        val document = engine.parse(StringYamlInput(""))
        assertNull(document.root)
    }

    @Test
    fun parsesAllSupportedScalarKinds() {
        val document = engine.parse(
            StringYamlInput(
                """
                string: hello
                quotedInteger: "1"
                boolean: true
                integer: 42
                hexadecimal: 0xff
                float: 1.5
                nullValue: null
                """.trimIndent()
            )
        )

        val root = assertIs<YamlMappingNode>(document.root)
        assertEquals(YamlScalarNode("hello", YamlScalarKind.STRING), root["string"])
        assertEquals(YamlScalarNode("1", YamlScalarKind.STRING), root["quotedInteger"])
        assertEquals(YamlScalarNode("true", YamlScalarKind.BOOLEAN), root["boolean"])
        assertEquals(YamlScalarNode("42", YamlScalarKind.INTEGER), root["integer"])
        assertEquals(YamlScalarNode("0xff", YamlScalarKind.INTEGER), root["hexadecimal"])
        assertEquals(YamlScalarNode("1.5", YamlScalarKind.FLOAT), root["float"])
        assertEquals(YamlScalarNode("null", YamlScalarKind.NULL), root["nullValue"])
    }

    @Test
    fun parsesBlockAndFlowCollections() {
        val document = engine.parse(
            StringYamlInput(
                """
                block:
                  - one
                  - two
                flow: [one, two]
                mapping: {a: 1, b: 2}
                """.trimIndent()
            )
        )

        val root = assertIs<YamlMappingNode>(document.root)
        val block = assertIs<YamlSequenceNode>(root["block"])
        val flow = assertIs<YamlSequenceNode>(root["flow"])
        assertEquals(block, flow)

        val mapping = assertIs<YamlMappingNode>(root["mapping"])
        assertEquals(YamlScalarNode("1", YamlScalarKind.INTEGER), mapping["a"])
        assertEquals(YamlScalarNode("2", YamlScalarKind.INTEGER), mapping["b"])
    }

    @Test
    fun mappingKeysKeepTheirScalarKinds() {
        val document = engine.parse(
            StringYamlInput(
                """
                1: integer
                "1": string
                true: boolean
                "true": stringBoolean
                """.trimIndent()
            )
        )

        val root = assertIs<YamlMappingNode>(document.root)
        assertEquals(
            YamlScalarNode("integer", YamlScalarKind.STRING),
            root[YamlMapKey("1", YamlScalarKind.INTEGER)],
        )
        assertEquals(
            YamlScalarNode("string", YamlScalarKind.STRING),
            root[YamlMapKey("1", YamlScalarKind.STRING)],
        )
        assertEquals(
            YamlScalarNode("boolean", YamlScalarKind.STRING),
            root[YamlMapKey("true", YamlScalarKind.BOOLEAN)],
        )
        assertEquals(
            YamlScalarNode("stringBoolean", YamlScalarKind.STRING),
            root[YamlMapKey("true", YamlScalarKind.STRING)],
        )
    }

    @Test
    fun duplicateMappingKeysAreRejected() {
        val exception = assertFailsWith<YamlParseException> {
            engine.parse(
                StringYamlInput(
                    """
                    key: first
                    key: second
                    """.trimIndent(),
                    description = "duplicate-test",
                )
            )
        }

        assertEquals("duplicate-test", exception.inputDescription)
    }

    @Test
    fun complexMappingKeysAreRejected() {
        assertFailsWith<YamlParseException> {
            engine.parse(
                StringYamlInput(
                    """
                    ? [a, b]
                    : value
                    """.trimIndent()
                )
            )
        }
    }

    @Test
    fun unsupportedExplicitTagIsRejected() {
        val exception = assertFailsWith<YamlParseException> {
            engine.parse(StringYamlInput("value: !custom hello"))
        }

        assertTrue(exception.message.orEmpty().contains("Unsupported YAML"))
    }

    @Test
    fun anchorsAreRejected() {
        val exception = assertFailsWith<YamlParseException> {
            engine.parse(
                StringYamlInput(
                    """
                    base: &base
                      value: 1
                    copy: *base
                    """.trimIndent()
                )
            )
        }

        assertTrue(exception.message.orEmpty().contains("anchor", ignoreCase = true))
    }

    @Test
    fun undefinedAliasIsRejected() {
        assertFailsWith<YamlParseException> {
            engine.parse(StringYamlInput("value: *missing"))
        }
    }

    @Test
    fun mergeKeysAreRejected() {
        val exception = assertFailsWith<YamlParseException> {
            engine.parse(
                StringYamlInput(
                    """
                    defaults: &defaults
                      port: 25565
                      
                    server:
                      <<: *defaults
                    """.trimIndent()
                )
            )
        }
    }

    @Test
    fun quotedMergeLikeKeyIsAllowed() {
        val document =
            engine.parse(
                StringYamlInput(
                """
                    server:
                      "<<": value
                    """.trimIndent()
                )
            )

        assertNotNull(
            document.root,
        )
    }

    @Test
    fun singleQuotedMergeLikeKeyIsAllowed() {
        val document =
            engine.parse(
                StringYamlInput(
                """
                server:
                  '<<': value
                """.trimIndent()
                )
            )

        assertNotNull(
            document.root,
        )
    }


    @Test
    fun quotedMergeLikeKeyIsAnOrdinaryStringKey() {
        val document = engine.parse(
            StringYamlInput(
                """
                value:
                  "<<": ordinary
                """.trimIndent()
            )
        )

        val root = assertIs<YamlMappingNode>(document.root)
        val value = assertIs<YamlMappingNode>(root["value"])
        assertEquals(YamlScalarNode("ordinary", YamlScalarKind.STRING), value["<<"])
    }
}
