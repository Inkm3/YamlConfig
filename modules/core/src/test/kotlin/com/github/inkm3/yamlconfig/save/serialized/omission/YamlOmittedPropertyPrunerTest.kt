package com.github.inkm3.yamlconfig.save.serialized.omission

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.internal.serialized.omission.YamlOmittedPropertyPruner
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralEditorApplier
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralNodeEditor
import com.github.inkm3.yamlconfig.testsupport.TestYamlEditor
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.n
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class YamlOmittedPropertyPrunerTest {
    @Serializable private data class Item(val id: String, val port: Int = 1, val derived: Int = 2)
    private val descriptor = serializer<Item>().descriptor
    private fun node(vararg pairs: Pair<String, YamlNode>) = stringMappingOf(*pairs)
    private fun check(descriptor: SerialDescriptor, wanted: YamlNode, observed: YamlNode, raw: YamlNode, result: YamlNode) {
        val before = raw.toString()
        val edit = assertNotNull(YamlOmittedPropertyPruner.plan(descriptor, wanted, observed, raw))
        assertEquals(result, YamlStructuralNodeEditor.apply(raw, edit))
        val editor = TestYamlEditor(raw)
        YamlStructuralEditorApplier.apply(editor, YamlPath.root(), edit)
        assertEquals(result, editor.root)
        assertEquals(before, raw.toString())
    }

    @Test fun removesOnlyReappearingOmittedPropertyAndRetainsOtherRawValues() {
        val wanted = node("id" to s("a"))
        val observed = node("id" to s("a"), "derived" to i(99))
        val raw = node("id" to s("a"), "port" to i(1), "derived" to i(99), "extra" to s("keep"))
        check(descriptor, wanted, observed, raw, node("id" to s("a"), "port" to i(1), "extra" to s("keep")))
    }

    @Test fun missingSourcePropertyCannotBeRemovedFromDefaultsByGuessing() {
        assertNull(YamlOmittedPropertyPruner.plan(descriptor, node("id" to s("a")),
            node("id" to s("a"), "port" to i(99)), node("id" to s("a"))))
    }

    @Test fun requiredPropertyMissingFromEncodingIsNotTreatedAsDefaultOmission() {
        assertNull(YamlOmittedPropertyPruner.plan(descriptor, node(), node("id" to s("a")), node("id" to s("a"))))
    }

    @Test fun explicitNullIsNotAnAbsentProperty() {
        assertNull(YamlOmittedPropertyPruner.plan(descriptor, node("port" to n()),
            node("port" to i(1)), node("port" to i(1))))
    }

    @Test fun unknownDataIsNotAnOmissionCandidate() {
        assertNull(YamlOmittedPropertyPruner.plan(descriptor, node("id" to s("a")),
            node("id" to s("a"), "unknown" to i(9)), node("id" to s("a"), "unknown" to i(9))))
    }

    @Test fun sequenceRepairUsesCurrentIndexWithoutDeletingElements() {
        val wanted = sequenceOf(node("id" to s("b")), node("id" to s("a")))
        val observed = sequenceOf(node("id" to s("b"), "derived" to i(99)), node("id" to s("a")))
        val raw = sequenceOf(node("id" to s("b"), "derived" to i(99), "extra" to s("b")),
            node("id" to s("a"), "extra" to s("a")))
        check(serializer<List<Item>>().descriptor, wanted, observed, raw,
            sequenceOf(node("id" to s("b"), "extra" to s("b")), node("id" to s("a"), "extra" to s("a"))))
    }

    @Test fun sequenceLengthAndSourceOrderMismatchAreNotGuessed() {
        val wanted = sequenceOf(node("id" to s("a")))
        val observed = sequenceOf(node("id" to s("a"), "derived" to i(99)))
        assertNull(YamlOmittedPropertyPruner.plan(serializer<List<Item>>().descriptor,
            wanted, observed, sequenceOf()))
        assertNull(YamlOmittedPropertyPruner.plan(serializer<List<Item>>().descriptor,
            wanted, observed, sequenceOf(node("id" to s("b"), "derived" to i(99)))))
    }

    @Test fun numericMapKeysUseRawKeyPaths() {
        val canonical = YamlMapKey("1", YamlScalarKind.INTEGER)
        val rawKey = YamlMapKey("+1", YamlScalarKind.INTEGER)
        val wanted = YamlMappingNode(mapOf(canonical to node("id" to s("a"))))
        val observed = YamlMappingNode(mapOf(canonical to node("id" to s("a"), "derived" to i(99))))
        val raw = YamlMappingNode(mapOf(rawKey to node("id" to s("a"), "derived" to i(99), "extra" to s("keep"))))
        check(serializer<Map<Int, Item>>().descriptor, wanted, observed, raw,
            YamlMappingNode(mapOf(rawKey to node("id" to s("a"), "extra" to s("keep")))))
    }

    @Test fun removedMapEntriesAreNotOptionalObjectProperties() {
        assertNull(YamlOmittedPropertyPruner.plan(serializer<Map<String, Item>>().descriptor,
            node(), node("a" to node("id" to s("a"))), node("a" to node("id" to s("a")))))
    }

    @Test fun equalEncodingDoesNotTriggerRawDataCleanup() {
        val encoded = node("id" to s("a"))
        assertNull(YamlOmittedPropertyPruner.plan(descriptor, encoded, encoded,
            node("id" to s("a"), "port" to i(1))))
    }
}
