package com.github.inkm3.yamlconfig.save

import com.github.inkm3.yamlconfig.YamlConfig
import com.github.inkm3.yamlconfig.schema.int
import com.github.inkm3.yamlconfig.schema.yamlObject
import com.github.inkm3.yamlconfig.testsupport.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class YamlConfigSessionTest {
    class Config { var value: Int = 0 }
    private val schema = yamlObject(::Config) { field(Config::value, int()) }

    @Test fun successfulSaveUpdatesBaselineSoSecondSaveIsNoOp() {
        val source = TestYamlSource(stringMappingOf("value" to i(1)))
        val session = YamlConfig(TestYamlEngine(), source, schema).load()
        session.value.value = 2
        session.save()
        source.lastOperations = emptyList()
        session.save()
        assertTrue(source.lastOperations.none { it is TestEditOperation.Set || it is TestEditOperation.Remove })
    }

    @Test fun failedWriteDoesNotAcceptWorkingEditorOrBaseline() {
        val source = TestYamlSource(stringMappingOf("value" to i(1)))
        val engine = TestYamlEngine()
        val session = YamlConfig(engine, source, schema).load()
        session.value.value = 2
        engine.failNextWrite = true
        assertFails { session.save() }
        assertEquals(i(1), (source.rootNode as com.github.inkm3.yamlconfig.node.YamlMappingNode)["value"])

        session.save()
        assertEquals(i(2), (source.rootNode as com.github.inkm3.yamlconfig.node.YamlMappingNode)["value"])
    }
}
