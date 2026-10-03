package com.github.inkm3.yamlconfig.snakeyaml

import com.github.inkm3.yamlconfig.node.*
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import kotlin.test.Test
import kotlin.test.assertTrue

class SnakeYamlPresentationTest {
    private val engine = SnakeYamlEngine()

    @Test
    fun replacingScalarPreservesSingleQuotedStyle() {
        val document = engine.parse(StringYamlInput("value: 'old'"))
        val editor = document.editor()
        editor.set(
            YamlPath.root().child("value"),
            YamlScalarNode("new", YamlScalarKind.STRING),
        )

        val output = write(editor)
        assertTrue(output.contains("'new'"), output)
    }

    @Test
    fun replacingScalarPreservesInlineComment() {
        val document = engine.parse(StringYamlInput("value: old # keep me"))
        val editor = document.editor()
        editor.set(
            YamlPath.root().child("value"),
            YamlScalarNode("new", YamlScalarKind.STRING),
        )

        val output = write(editor)
        assertTrue(output.contains("# keep me"), output)
    }

    @Test
    fun replacingMappingValuePreservesOriginalKeyStyle() {
        val document = engine.parse(StringYamlInput("'port': 25565"))
        val editor = document.editor()
        editor.mapping(YamlPath.root()).replace(
            YamlMapKey("port"),
            YamlScalarNode("30000", YamlScalarKind.INTEGER),
        )

        val output = write(editor)
        assertTrue(output.contains("'port':"), output)
        assertTrue(output.contains("30000"), output)
    }

    @Test
    fun replacingSequencePreservesFlowStyle() {
        val document = engine.parse(StringYamlInput("items: [a, b]"))
        val editor = document.editor()
        editor.set(
            YamlPath.root().child("items"),
            YamlSequenceNode(
                listOf(
                    YamlScalarNode("x", YamlScalarKind.STRING),
                    YamlScalarNode("y", YamlScalarKind.STRING),
                )
            ),
        )

        val output = write(editor)
        assertTrue(output.contains("items: ["), output)
    }

    @Test
    fun replacingMappingPreservesFlowStyle() {
        val document = engine.parse(StringYamlInput("value: {a: 1, b: 2}"))
        val editor = document.editor()
        editor.set(
            YamlPath.root().child("value"),
            YamlMappingNode(
                linkedMapOf(
                    YamlMapKey("x") to YamlScalarNode("3", YamlScalarKind.INTEGER),
                )
            ),
        )

        val output = write(editor)
        assertTrue(output.contains("value: {"), output)
    }

    @Test
    fun sequenceMoveMovesPresentationWithNativeNode() {
        val document = engine.parse(
            StringYamlInput(
                """
                items:
                  # comment-a
                  - A
                  # comment-b
                  - B
                  # comment-c
                  - C
                """.trimIndent()
            )
        )
        val editor = document.editor()
        editor.sequence(YamlPath.root().child("items")).move(2, 0)

        val output = write(editor)
        val commentC = output.indexOf("# comment-c")
        val valueC = output.indexOf("C", startIndex = commentC.coerceAtLeast(0))
        val commentA = output.indexOf("# comment-a")

        assertTrue(commentC >= 0, output)
        assertTrue(valueC > commentC, output)
        assertTrue(commentA > valueC, output)
    }

    private fun write(editor: com.github.inkm3.yamlconfig.spi.YamlEditor): String {
        val output = MemoryYamlSource()
        engine.write(editor, output)
        return output.text
    }
}
