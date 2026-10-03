package com.github.inkm3.yamlconfig.save.serialized.performance.reduction

import com.github.inkm3.yamlconfig.node.*
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlMinimalOverrideReducer
import com.github.inkm3.yamlconfig.save.internal.serialized.reduction.YamlRemovalCandidates
import com.github.inkm3.yamlconfig.testsupport.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.serializer
import kotlin.test.*

// Test-only delegating probes observe descriptor work; production only consumes
// descriptors through the supported API and does not implement this interface.
@OptIn(ExperimentalSerializationApi::class, SealedSerializationApi::class)
class YamlRemovalCandidatesTest {
    @Serializable private data class Child(val b: Int = 2)
    @Serializable private data class Config(val a: Int = 1, val child: Child = Child(), val list: List<Child> = emptyList())
    private fun names(paths: List<List<YamlMapKey>>) = paths.map { it.map(YamlMapKey::value) }

    @Test fun parentThenDescriptorOrderDoesNotDependOnUserEntryOrder() {
        val root = stringMappingOf("child" to stringMappingOf("b" to i(2)), "a" to i(1))
        assertEquals(listOf(emptyList(), listOf("a"), listOf("child"), listOf("child", "b")),
            names(YamlRemovalCandidates.collect(serializer<Config>().descriptor, root)))
    }

    @Test fun unknownDescendantProtectsAncestorsButNotKnownSiblings() {
        val root = stringMappingOf("a" to i(1), "child" to stringMappingOf("b" to i(2), "unknown" to i(3)))
        assertEquals(listOf(listOf("a"), listOf("child", "b")),
            names(YamlRemovalCandidates.collect(serializer<Config>().descriptor, root)))
    }

    @Test fun collectionsAreWholeCandidatesAndUnknownElementsProtectThem() {
        val descriptor = serializer<Config>().descriptor
        assertEquals(listOf(emptyList(), listOf("list")), names(YamlRemovalCandidates.collect(descriptor,
            stringMappingOf("list" to sequenceOf(stringMappingOf("b" to i(2)))))))
        assertEquals(emptyList(), YamlRemovalCandidates.collect(descriptor,
            stringMappingOf("list" to sequenceOf(stringMappingOf("b" to i(2), "extra" to i(3))))))
    }

    @Test fun missingFieldsDoNotResolveTheirChildDescriptors() {
        val wrapped = object : SerialDescriptor by serializer<Config>().descriptor {
            override fun getElementDescriptor(index: Int): SerialDescriptor = error("Missing child was expanded")
        }
        assertEquals(listOf(emptyList()), YamlRemovalCandidates.collect(wrapped, stringMappingOf()))
    }

    @Test fun descriptorTraversalDoesNotRepeatAfterAcceptedRemovals() {
        fun reads(accepts: Boolean): Int {
            var count = 0
            val descriptor = object : SerialDescriptor by serializer<Config>().descriptor {
                override fun getElementName(index: Int): String { count++; return serializer<Config>().descriptor.getElementName(index) }
                override fun getElementDescriptor(index: Int): SerialDescriptor { count++; return serializer<Config>().descriptor.getElementDescriptor(index) }
                override fun getElementIndex(name: String): Int { count++; return serializer<Config>().descriptor.getElementIndex(name) }
            }
            YamlMinimalOverrideReducer.reduceToResult(descriptor,
                stringMappingOf("a" to i(1), "child" to stringMappingOf("b" to i(2)))) { root ->
                accepts && root != null
            }
            return count
        }
        assertTrue(reads(false) > 0)
        assertEquals(reads(false), reads(true))
    }

    @Test fun returnedRootIsTheLastAcceptedCandidateNotAReconstructedCopy() {
        val initial = stringMappingOf("a" to i(1), "child" to stringMappingOf("b" to i(2)))
        var accepted: YamlNode? = initial
        val result = YamlMinimalOverrideReducer.reduceToResult(serializer<Config>().descriptor, initial) {
            val ok = it != null
            if (ok) accepted = it
            ok
        }
        assertTrue(result.removals.isNotEmpty())
        assertSame(accepted, result.root)
        assertSame(initial, YamlMinimalOverrideReducer.reduceToResult(serializer<Config>().descriptor, initial) { false }.root)
    }

    @Test fun missingRootDoesNotResolveDescriptorOrCallValidator() {
        val descriptor = object : SerialDescriptor by serializer<Config>().descriptor {
            override val kind get() = error("Missing root was inspected")
        }
        val result = YamlMinimalOverrideReducer.reduceToResult(descriptor, null) { error("Missing root was validated") }
        assertNull(result.root)
        assertTrue(result.removals.isEmpty())
    }
}
