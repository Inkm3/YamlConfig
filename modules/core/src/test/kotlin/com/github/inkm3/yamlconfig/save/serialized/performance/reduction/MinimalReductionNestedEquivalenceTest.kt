package com.github.inkm3.yamlconfig.save.serialized.performance.reduction

import com.github.inkm3.yamlconfig.node.*
import com.github.inkm3.yamlconfig.testsupport.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class MinimalReductionNestedEquivalenceTest {
    @Serializable private data class Tree(val value: Int = 0, val left: Tree? = null, val right: Tree? = null,
        val items: List<Tree> = emptyList(), val data: Map<String, Tree> = emptyMap())

    private fun compare(root: YamlNode?, seed: Int, keepRoot: Boolean = false) {
        val case = ReductionCase("nested-$seed", serializer<Tree>().descriptor, root) { candidate ->
            (candidate != null || !keepRoot) && (candidate.toString().hashCode() xor seed).ushr(2) % 3 != 0
        }
        val old = ReductionFixtures.run(case, true, true)
        val new = ReductionFixtures.run(case, false, true)
        assertEquals(old.observations, new.observations, case.name)
        assertEquals(old.candidate.root.toString(), new.candidate.root.toString(), case.name)
        assertEquals(old.candidate.patches.size, new.candidate.patches.size, case.name)
        val editor = TestYamlEditor(root)
        new.candidate.patches.forEach { it.apply(editor) }
        assertEquals(new.candidate.root.toString(), editor.root.toString(), case.name)
    }

    @Test fun recursiveDescriptorsWithFiniteNodesKeepTheReferenceTrace() {
        val random = Random(39481)
        fun node(depth: Int): YamlNode {
            val entries = mutableListOf<Pair<String, YamlNode>>("value" to i(random.nextInt(4)))
            if (random.nextBoolean()) entries.add("plugin" to s("keep-${random.nextInt(3)}"))
            if (depth > 0) {
                if (random.nextBoolean()) entries.add("left" to node(depth - 1))
                if (random.nextBoolean()) entries.add("right" to node(depth - 1))
                if (random.nextBoolean()) entries.add("items" to sequenceOf(node(depth - 1)))
                if (random.nextBoolean()) entries.add("data" to stringMappingOf("entry" to node(depth - 1)))
            }
            return stringMappingOf(*entries.shuffled(random).toTypedArray())
        }
        repeat(256) { seed -> compare(node(3), seed, keepRoot = true) }
    }

    @Test fun explicitNullAndAbsentChildrenKeepIdenticalCandidateOrdering() {
        repeat(32) { seed ->
            compare(stringMappingOf("right" to n(), "value" to i(0), "left" to stringMappingOf()), seed)
        }
    }

    @Test fun unknownLastCollectionElementProtectsAllItsAncestors() {
        val clean = stringMappingOf("value" to i(0))
        val unknown = stringMappingOf("value" to i(0), "plugin" to s("keep"))
        val root = stringMappingOf("left" to stringMappingOf("items" to sequenceOf(clean, clean, unknown)),
            "right" to stringMappingOf("data" to stringMappingOf("first" to clean, "last" to unknown)), "value" to i(1))
        repeat(32) { seed -> compare(root, seed) }
    }

    @Test fun sharedNodesAtDifferentPathsDoNotCauseCandidateDeduplication() {
        val shared = stringMappingOf("value" to i(0))
        val root = stringMappingOf("left" to shared, "right" to shared, "items" to sequenceOf(shared))
        repeat(32) { seed -> compare(root, seed) }
    }
}
