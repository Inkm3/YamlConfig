package com.github.inkm3.yamlconfig.save.serialized.structure

import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlKnownValuePlanner
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlSourceAlignment
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlSourceAlignmentMemo
import com.github.inkm3.yamlconfig.testsupport.TestYamlEditor
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YamlAlignmentPlanScopeTest {
    @Test fun retainedAndForcedPlansCanReuseOnlyTheSameSourceAndPolicy() {
        val descriptor = serializer<List<Int>>().descriptor
        val baseline = sequenceOf(i(1), i(2))
        val source = sequenceOf(YamlScalarNode("+1", YamlScalarKind.INTEGER), i(2))
        val expected = sequenceOf(i(2), i(1))
        var collectionChecks = 0
        val memo = YamlSourceAlignmentMemo { d, b, u, omitted ->
            if (d === descriptor && b === baseline && u === source) collectionChecks++
            YamlSourceAlignment.matches(d, b, u, omitted)
        }
        for (force in listOf(false, true)) {
            val patches = YamlKnownValuePlanner.plan(descriptor, baseline, expected, source, force,
                retainOmitted = true, alignment = memo)
            val editor = TestYamlEditor(source)
            patches.forEach { it.apply(editor) }
            assertTrue(YamlSourceAlignment.matches(descriptor, expected, editor.root))
            assertEquals(source.toString(), sequenceOf(YamlScalarNode("+1", YamlScalarKind.INTEGER), i(2)).toString())
        }
        assertEquals(1, collectionChecks)
        YamlKnownValuePlanner.plan(descriptor, baseline, expected, source, false,
            retainOmitted = false, alignment = memo)
        assertEquals(2, collectionChecks)
    }

    @Test fun independentPlanContextsRepeatTheOriginalAlignmentCheck() {
        val descriptor = serializer<List<Int>>().descriptor
        val baseline = sequenceOf(i(1), i(2))
        val source = sequenceOf(YamlScalarNode("+1", YamlScalarKind.INTEGER), i(2))
        val expected = sequenceOf(i(2), i(1))
        var collectionChecks = 0
        repeat(2) {
            val memo = YamlSourceAlignmentMemo { d, b, u, omitted ->
                if (d === descriptor && b === baseline && u === source) collectionChecks++
                YamlSourceAlignment.matches(d, b, u, omitted)
            }
            YamlKnownValuePlanner.plan(descriptor, baseline, expected, source, alignment = memo)
        }
        assertEquals(2, collectionChecks)
    }
}
