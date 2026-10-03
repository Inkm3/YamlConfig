package com.github.inkm3.yamlconfig.save.serialized.performance.reduction

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** This is a reducer compatibility probe, not a new public property-alias API. */
@OptIn(ExperimentalSerializationApi::class, SealedSerializationApi::class)
class YamlRemovalAliasProtectionTest {
    @Serializable private data class Child(val value: Int = 0)
    @Serializable private data class Config(val child: Child = Child(), val items: List<Child> = emptyList(), val port: Int = 1)
    private val canonical = serializer<Config>().descriptor
    private val descriptor = object : SerialDescriptor by canonical {
        override fun getElementIndex(name: String): Int = canonical.getElementIndex(name.removePrefix("legacy-"))
    }

    private fun compare(root: YamlNode): YamlNode? {
        val case = ReductionCase("alias", descriptor, root) { true }
        val before = ReductionFixtures.run(case, true, true)
        val after = ReductionFixtures.run(case, false, true)
        assertEquals(before.observations, after.observations)
        assertEquals(before.candidate.root.toString(), after.candidate.root.toString())
        return after.candidate.root
    }

    @Test fun unknownAliasChildProtectsItsParent() {
        val alias = stringMappingOf("value" to i(0), "plugin" to s("keep"))
        assertEquals(stringMappingOf("legacy-child" to alias),
            compare(stringMappingOf("legacy-child" to alias, "port" to i(1))))
    }

    @Test fun canonicalChildDoesNotHideUnknownDataInAnotherAlias() {
        val alias = stringMappingOf("plugin" to s("keep"))
        assertEquals(stringMappingOf("legacy-child" to alias), compare(stringMappingOf(
            "child" to stringMappingOf("value" to i(0)), "legacy-child" to alias, "port" to i(1))))
    }

    @Test fun aliasCollectionContentsAreInspectedForUnknownData() {
        val items = sequenceOf(stringMappingOf("value" to i(0)), stringMappingOf("plugin" to s("keep")))
        assertEquals(stringMappingOf("legacy-items" to items),
            compare(stringMappingOf("legacy-items" to items, "port" to i(1))))
    }

    @Test fun cleanAliasDoesNotBecomeAnExtraIndividualRemovalCandidate() {
        assertNull(compare(stringMappingOf("legacy-child" to stringMappingOf("value" to i(0)))))
        val root = stringMappingOf("legacy-child" to stringMappingOf("value" to i(0)), "port" to i(1))
        val case = ReductionCase("reject-root", descriptor, root) { it != null }
        val before = ReductionFixtures.run(case, true, true)
        val after = ReductionFixtures.run(case, false, true)
        assertEquals(before.observations, after.observations)
        assertEquals(stringMappingOf("legacy-child" to stringMappingOf("value" to i(0))), after.candidate.root)
    }
}
