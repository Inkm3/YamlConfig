package com.github.inkm3.yamlconfig.save.serialized.identity

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlScalarNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralEditorApplier
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralNodeEditor
import com.github.inkm3.yamlconfig.save.internal.serialized.structure.YamlStructuralValuePlanner
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.testsupport.TestYamlEditor
import com.github.inkm3.yamlconfig.testsupport.i
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class YamlIdentityOwnershipReplayTest {
    @Serializable
    private data class Item(val id: String, val value: Int)

    @Test
    fun allSmallListsPreserveUniqueOwnersAndNeverTransferMetadataToADifferentIdentity() {
        val format = YamlSerialization(ignoreUnknownKeys = true)
        val serializer = serializer<List<Item>>()
        val alphabet = listOf(Item("a", 0), Item("a", 1), Item("b", 0), Item("b", 1))
        val lists = mutableListOf<List<Item>>(emptyList())
        var level: List<List<Item>> = listOf(emptyList())
        repeat(3) {
            level = level.flatMap { prefix -> alphabet.map { prefix + it } }
            lists.addAll(level)
        }
        val samples = lists.map { it to assertIs<YamlSequenceNode>(format.encodeToNode(serializer, it)) }
        var pairs = 0
        for ((before, baseline) in samples) {
            val source = YamlSequenceNode(baseline.elements.mapIndexed { index, node ->
                val entries = assertIs<YamlMappingNode>(node).entries + (YamlMapKey("owner") to i(index))
                YamlMappingNode(entries)
            })
            val original = source.toString()
            val oldCounts = before.groupingBy { it.id }.eachCount()
            for ((after, current) in samples) {
                val edit = YamlStructuralValuePlanner.plan(serializer.descriptor, baseline, current, source, root = true)
                val result = assertIs<YamlSequenceNode>(if (edit == null) source else YamlStructuralNodeEditor.apply(source, edit))
                val editor = TestYamlEditor(source)
                if (edit != null) YamlStructuralEditorApplier.apply(editor, YamlPath.root(), edit)
                assertEquals(result, editor.root)
                assertEquals(after, format.decodeFromNode(serializer, result))
                assertEquals(original, source.toString())
                val newCounts = after.groupingBy { it.id }.eachCount()
                for (index in after.indices) {
                    val value = after[index]
                    val owner = (assertIs<YamlMappingNode>(result[index])["owner"] as? YamlScalarNode)?.value?.toInt()
                    val unique = oldCounts[value.id] == 1 && newCounts[value.id] == 1
                    if (unique) assertEquals(before.indexOfFirst { it.id == value.id }, owner)
                    if (owner != null) {
                        assertTrue(owner in before.indices)
                        assertEquals(value.id, before[owner].id)
                        assertTrue(unique || value == before[owner], "Ambiguous changed item retained an old owner")
                    }
                }
                pairs++
            }
        }
        assertEquals(7225, pairs)
    }
}
