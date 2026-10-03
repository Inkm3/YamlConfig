package com.github.inkm3.yamlconfig.save.serialized.structure

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlSequenceStep
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralEdit
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralEditorApplier
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralNodeEditor
import com.github.inkm3.yamlconfig.testsupport.TestYamlEditor
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class YamlStructuralEditTest {
    private fun mapping(vararg changes: Pair<String, YamlStructuralEdit>) =
        YamlStructuralEdit.Mapping(changes.associateTo(linkedMapOf()) { YamlMapKey(it.first) to it.second })

    private fun check(initial: YamlNode?, edit: YamlStructuralEdit, expected: YamlNode?) {
        val before = initial.toString()
        val pure = YamlStructuralNodeEditor.apply(initial, edit)
        val editor = TestYamlEditor(initial)
        YamlStructuralEditorApplier.apply(editor, YamlPath.root(), edit)
        assertEquals(expected, pure)
        assertEquals(expected, editor.root)
        assertEquals(before, initial.toString())
    }

    @Test fun setAndRootRemovalAgreeInBothAppliers() {
        check(null, YamlStructuralEdit.Set(i(2)), i(2))
        check(i(2), YamlStructuralEdit.Set(null), null)
    }

    @Test fun mappingUpdatePreservesUnknownSiblings() {
        check(stringMappingOf("port" to i(1), "future" to s("keep")),
            mapping("port" to YamlStructuralEdit.Set(i(2))),
            stringMappingOf("port" to i(2), "future" to s("keep")))
    }

    @Test fun mappingInsertAndRemoveDoNotReplaceOtherEntries() {
        check(stringMappingOf("a" to i(1), "b" to i(2)),
            mapping("a" to YamlStructuralEdit.Set(null), "c" to YamlStructuralEdit.Set(i(3))),
            stringMappingOf("b" to i(2), "c" to i(3)))
    }

    @Test fun missingMappingIsCreatedOnlyWhenAValueIsWritten() {
        check(null, mapping("absent" to YamlStructuralEdit.Set(null)), null)
        check(null, mapping("new" to YamlStructuralEdit.Set(i(3))), stringMappingOf("new" to i(3)))
    }

    @Test fun mixedSequenceEditsUseEvolvingIndices() {
        val edit = YamlStructuralEdit.Sequence(listOf(
            YamlSequenceStep.Move(2, 0), YamlSequenceStep.Insert(1, s("x")),
            YamlSequenceStep.Update(2, YamlStructuralEdit.Set(s("updated-a"))), YamlSequenceStep.Remove(3),
        ))
        check(sequenceOf(s("a"), s("b"), s("c")), edit, sequenceOf(s("c"), s("x"), s("updated-a")))
    }

    @Test fun moveThenNestedUpdateKeepsDataWithItsElement() {
        val a = stringMappingOf("port" to i(1), "extra" to s("a"))
        val b = stringMappingOf("port" to i(2), "extra" to s("b"))
        val edit = YamlStructuralEdit.Sequence(listOf(
            YamlSequenceStep.Move(1, 0), YamlSequenceStep.Update(0, mapping("port" to YamlStructuralEdit.Set(i(3)))),
        ))
        check(sequenceOf(a, b), edit, sequenceOf(stringMappingOf("port" to i(3), "extra" to s("b")), a))
    }

    @Test fun removingLastElementKeepsExplicitEmptySequence() {
        check(sequenceOf(i(1)), YamlStructuralEdit.Sequence(listOf(YamlSequenceStep.Remove(0))), sequenceOf())
    }

    @Test fun sequencesInsideMappingsAndMappingsInsideSequencesCompose() {
        val edit = mapping("items" to YamlStructuralEdit.Sequence(listOf(
            YamlSequenceStep.Update(0, mapping("ports" to YamlStructuralEdit.Sequence(listOf(
                YamlSequenceStep.Insert(0, i(3)),
            )))),
        )))
        check(stringMappingOf("items" to sequenceOf(stringMappingOf("ports" to sequenceOf(i(1))))), edit,
            stringMappingOf("items" to sequenceOf(stringMappingOf("ports" to sequenceOf(i(3), i(1))))))
    }

    @Test fun invalidSequenceIndicesFailRatherThanSilentlySkipping() {
        val initial = sequenceOf(i(1))
        for (step in listOf(YamlSequenceStep.Remove(1), YamlSequenceStep.Move(0, 1),
            YamlSequenceStep.Insert(2, i(2)), YamlSequenceStep.Update(-1, YamlStructuralEdit.Set(i(3))))) {
            val edit = YamlStructuralEdit.Sequence(listOf(step))
            assertFailsWith<IllegalArgumentException> { YamlStructuralNodeEditor.apply(initial, edit) }
            assertFailsWith<IllegalArgumentException> {
                YamlStructuralEditorApplier.apply(TestYamlEditor(initial), YamlPath.root(), edit)
            }
        }
    }

    @Test fun nullSequenceUpdateIsRejectedByBothAppliers() {
        val edit = YamlStructuralEdit.Sequence(listOf(YamlSequenceStep.Update(0, YamlStructuralEdit.Set(null))))
        assertFailsWith<IllegalArgumentException> { YamlStructuralNodeEditor.apply(sequenceOf(i(1)), edit) }
        assertFailsWith<IllegalArgumentException> {
            YamlStructuralEditorApplier.apply(TestYamlEditor(sequenceOf(i(1))), YamlPath.root(), edit)
        }
    }
}
