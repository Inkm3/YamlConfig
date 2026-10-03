package com.github.inkm3.yamlconfig.save.internal.session

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.source.YamlOutput
import com.github.inkm3.yamlconfig.spi.YamlEditor
import com.github.inkm3.yamlconfig.spi.YamlEngine
import com.github.inkm3.yamlconfig.testsupport.TestYamlEditor
import com.github.inkm3.yamlconfig.testsupport.TestYamlEngine
import com.github.inkm3.yamlconfig.testsupport.TestYamlSource
import com.github.inkm3.yamlconfig.testsupport.i
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class YamlSaveTransactionTest {
    private class Fixture {
        val source = TestYamlSource(i(1))
        val original = TestYamlEditor(i(1))
        var forks = 0
        var writes = 0
        var failFork = false
        var failWrite = false
        val delegate = TestYamlEngine()
        val engine = object : YamlEngine by delegate {
            override fun write(editor: YamlEditor, output: YamlOutput) {
                writes++
                check(!failWrite) { "write failed" }
                delegate.write(editor, output)
            }
        }
        val editor = object : YamlEditor by original {
            override fun fork(): YamlEditor {
                forks++
                check(!failFork) { "fork failed" }
                return original.fork()
            }
        }
        val transaction = YamlSaveTransaction(1, editor, engine, source)
        fun plan(value: Int): YamlPreparedSave<Int> = YamlPreparedSave(value) { it.set(YamlPath.root(), i(value)) }
        fun assertState(value: Int) {
            transaction.save { baseline, root ->
                assertEquals(value, baseline)
                assertEquals(i(value), root)
                null
            }
        }
    }

    @Test fun successAdvancesBaselineAndEditorTogether() {
        val f = Fixture()
        f.transaction.save { baseline, root ->
            assertEquals(1, baseline)
            assertEquals(i(1), root)
            f.plan(2)
        }
        f.assertState(2)
        assertEquals(i(2), f.source.rootNode)
        assertEquals(i(1), f.original.root)
    }

    @Test fun nullPlanDoesNotForkOrWrite() {
        val f = Fixture()
        f.transaction.save { _, _ -> null }
        assertEquals(0, f.forks)
        assertEquals(0, f.writes)
        f.assertState(1)
    }

    @Test fun preparationFailureLeavesStateUntouchedAndRetryWorks() {
        val f = Fixture()
        assertFailsWith<IllegalStateException> { f.transaction.save { _, _ -> error("prepare failed") } }
        assertEquals(0, f.forks)
        assertEquals(0, f.writes)
        f.assertState(1)
        f.transaction.save { _, _ -> f.plan(2) }
        f.assertState(2)
    }

    @Test fun forkFailureLeavesStateUntouched() {
        val f = Fixture()
        f.failFork = true
        assertFailsWith<IllegalStateException> { f.transaction.save { _, _ -> f.plan(2) } }
        assertEquals(0, f.writes)
        f.assertState(1)
        f.failFork = false
        f.transaction.save { _, _ -> f.plan(2) }
        f.assertState(2)
    }

    @Test fun editFailureDiscardsPartialChanges() {
        val f = Fixture()
        assertFailsWith<IllegalStateException> {
            f.transaction.save { _, _ -> YamlPreparedSave(2) {
                it.set(YamlPath.root(), i(99))
                error("edit failed")
            } }
        }
        assertEquals(0, f.writes)
        assertEquals(i(1), f.original.root)
        f.assertState(1)
    }

    @Test fun validationFailureBeforeWriteDiscardsWorkingEditor() {
        val f = Fixture()
        assertFailsWith<IllegalArgumentException> {
            f.transaction.save { _, _ -> YamlPreparedSave(2) {
                it.set(YamlPath.root(), i(2))
                require(it.root == i(3)) { "semantic validation failed" }
            } }
        }
        assertEquals(0, f.writes)
        f.assertState(1)
    }

    @Test fun failedWriteKeepsPreviousBaselineForRetry() {
        val f = Fixture()
        f.failWrite = true
        assertFailsWith<IllegalStateException> { f.transaction.save { _, _ -> f.plan(2) } }
        assertEquals(i(1), f.source.rootNode)
        f.assertState(1)
        f.failWrite = false
        f.transaction.save { baseline, _ -> assertEquals(1, baseline); f.plan(2) }
        f.assertState(2)
        assertEquals(2, f.writes)
    }

    @Test fun exceptionIdentityIsNotLost() {
        val f = Fixture()
        val error = IllegalArgumentException("original")
        val thrown = assertFailsWith<IllegalArgumentException> {
            f.transaction.save { _, _ -> throw error }
        }
        assertSame(error, thrown)
    }

    @Test fun reentrantSaveFailsWithoutCommittingEitherOperation() {
        val f = Fixture()
        assertFailsWith<IllegalStateException> {
            f.transaction.save { _, _ -> f.transaction.save { _, _ -> f.plan(3) }; f.plan(2) }
        }
        assertEquals(0, f.writes)
        f.assertState(1)
        f.transaction.save { _, _ -> f.plan(2) }
        f.assertState(2)
    }

    @Test fun nextSaveStartsFromLastSuccessfulEditor() {
        val f = Fixture()
        f.transaction.save { _, _ -> f.plan(2) }
        f.failWrite = true
        assertFailsWith<IllegalStateException> { f.transaction.save { _, _ -> f.plan(3) } }
        f.assertState(2)
        f.failWrite = false
        f.transaction.save { baseline, root ->
            assertEquals(2, baseline)
            assertEquals(i(2), root)
            f.plan(4)
        }
        f.assertState(4)
    }
}
