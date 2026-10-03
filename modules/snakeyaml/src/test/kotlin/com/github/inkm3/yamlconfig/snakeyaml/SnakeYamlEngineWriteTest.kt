package com.github.inkm3.yamlconfig.snakeyaml

import com.github.inkm3.yamlconfig.exception.YamlWriteException
import com.github.inkm3.yamlconfig.node.*
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.FailingYamlOutput
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import com.github.inkm3.yamlconfig.spi.YamlEditor
import com.github.inkm3.yamlconfig.spi.editor.YamlMappingEditor
import com.github.inkm3.yamlconfig.spi.editor.YamlSequenceEditor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SnakeYamlEngineWriteTest {
    private val engine = SnakeYamlEngine()

    @Test
    fun writesEditedDocumentAndCommitsTransaction() {
        val document = engine.parse(StringYamlInput("name: old"))
        val editor = document.editor()
        editor.set(
            YamlPath.root().child("name"),
            YamlScalarNode("new", YamlScalarKind.STRING),
        )

        val output = MemoryYamlSource()
        engine.write(editor, output)

        assertEquals(1, output.commitCount)
        assertTrue(output.text.contains("name:"))
        assertTrue(output.text.contains("new"))

        val reparsed = engine.parse(StringYamlInput(output.text))
        assertEquals(editor.root, reparsed.root)
    }

    @Test
    fun writingEmptyRootProducesEmptyTextAndStillCommits() {
        val document = engine.parse(StringYamlInput("value: 1"))
        val editor = document.editor()
        editor.remove(YamlPath.root())

        val output = MemoryYamlSource("previous")
        engine.write(editor, output)

        assertEquals("", output.text)
        assertEquals(1, output.commitCount)
    }

    @Test
    fun newStringValuesUseDoubleQuotedStyle() {
        val document = engine.parse(StringYamlInput("{}"))
        val editor = document.editor()
        editor.mapping(YamlPath.root()).insert(
            YamlMapKey("value"),
            YamlScalarNode("hello", YamlScalarKind.STRING),
        )

        val output = MemoryYamlSource()
        engine.write(editor, output)

        assertTrue(output.text.contains("value: \"hello\""), output.text)
    }

    @Test
    fun safeNewStringKeysArePlain() {
        val document = engine.parse(StringYamlInput("{}"))
        val editor = document.editor()
        editor.mapping(YamlPath.root()).insert(
            YamlMapKey("server_name"),
            YamlScalarNode("value", YamlScalarKind.STRING),
        )

        val output = MemoryYamlSource()
        engine.write(editor, output)

        assertTrue(output.text.contains("server_name:"), output.text)
        assertTrue(!output.text.contains("\"server_name\":"), output.text)
    }

    @Test
    fun ambiguousNewStringKeysAreDoubleQuoted() {
        val document = engine.parse(StringYamlInput("{}"))
        val editor = document.editor()
        editor.mapping(YamlPath.root()).insert(
            YamlMapKey("true"),
            YamlScalarNode("value", YamlScalarKind.STRING),
        )

        val output = MemoryYamlSource()
        engine.write(editor, output)

        assertTrue(output.text.contains("\"true\":"), output.text)
    }

    @Test
    fun nonStringNewKeysArePlain() {
        val document = engine.parse(StringYamlInput("{}"))
        val editor = document.editor()
        editor.mapping(YamlPath.root()).insert(
            YamlMapKey("255", YamlScalarKind.INTEGER),
            YamlScalarNode("value", YamlScalarKind.STRING),
        )

        val output = MemoryYamlSource()
        engine.write(editor, output)

        assertTrue(output.text.contains("255:"), output.text)
    }

    @Test
    fun foreignEditorIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            engine.write(ForeignEditor(), MemoryYamlSource())
        }
    }

    @Test
    fun ioFailureIsWrappedInYamlWriteException() {
        val document = engine.parse(StringYamlInput("value: 1"))
        val exception = assertFailsWith<YamlWriteException> {
            engine.write(document.editor(), FailingYamlOutput("broken-output"))
        }

        assertEquals("broken-output", exception.outputDescription)
    }

    private class ForeignEditor : YamlEditor {
        override val root: YamlNode? = YamlMappingNode(emptyMap())

        override fun set(path: YamlPath, node: YamlNode): Unit = Unit
        override fun remove(path: YamlPath): Unit = Unit
        override fun mapping(path: YamlPath): YamlMappingEditor = error("unused")
        override fun sequence(path: YamlPath): YamlSequenceEditor = error("unused")
        override fun fork(): YamlEditor = this
    }
}
