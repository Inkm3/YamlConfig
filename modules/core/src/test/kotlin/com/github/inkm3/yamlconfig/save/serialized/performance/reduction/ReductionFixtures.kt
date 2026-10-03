package com.github.inkm3.yamlconfig.save.serialized.performance.reduction

import com.github.inkm3.yamlconfig.load.internal.YamlConfigValueLoader
import com.github.inkm3.yamlconfig.node.*
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlMinimalOverrideReducer
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlSaveCandidate
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.serializer

internal data class ReductionCase(val name: String, val descriptor: SerialDescriptor,
    val initial: YamlNode?, val accepts: (YamlNode?) -> Boolean)
internal data class ReductionRun(val candidate: YamlSaveCandidate, val checks: Int, val observations: List<String>)

@OptIn(ExperimentalSerializationApi::class)
internal object ReductionFixtures {
    @Serializable private data class Nested(val a: Int = 1, val b: Int = 2)
    @Serializable private data class Config(val token: String, val a: Int = 1,
        val b: Int = a + 1, val nested: Nested = Nested(), val items: List<Nested> = emptyList())

    fun run(case: ReductionCase, reference: Boolean, record: Boolean = false): ReductionRun {
        var checks = 0
        val observations = mutableListOf<String>()
        val callback = { root: YamlNode? ->
            checks++
            if (record) observations.add(root.toString())
            case.accepts(root)
        }
        val candidate = if (reference) {
            val patches = ReferenceOverrideReducer.reduce(case.descriptor, case.initial, callback)
            YamlSaveCandidate(patches.fold(case.initial) { node, patch -> patch.applyTo(node) }, patches)
        } else {
            // Production caller now uses the already validated root without replay.
            val reduction = YamlMinimalOverrideReducer.reduceToResult(case.descriptor, case.initial, callback)
            YamlSaveCandidate(reduction.root, reduction.removals)
        }
        return ReductionRun(candidate, checks, observations)
    }

    fun objectDescriptor(name: String, fields: List<Pair<String, SerialDescriptor>>): SerialDescriptor =
        buildClassSerialDescriptor(name) { fields.forEach { (key, type) -> element(key, type, isOptional = true) } }

    fun wide(size: Int, accept: Boolean): ReductionCase {
        val descriptor = objectDescriptor("Wide$size", (0 until size).map { "k$it" to serializer<Int>().descriptor })
        val initial = stringMappingOf(*(0 until size).map { "k$it" to i(it) }.toTypedArray())
        return ReductionCase("wide-$size-${if (accept) "remove" else "reject"}", descriptor, initial) {
            accept && (it as? YamlMappingNode)?.get("k0") != null
        }
    }

    fun deep(depth: Int, dropRoot: Boolean): ReductionCase {
        var node: YamlNode = i(1)
        var descriptor = serializer<Int>().descriptor
        repeat(depth) { level ->
            node = stringMappingOf("value" to i(level), "child" to node)
            descriptor = objectDescriptor("Depth$level", listOf("value" to serializer<Int>().descriptor, "child" to descriptor))
        }
        return ReductionCase("deep-$depth-${if (dropRoot) "root" else "leaves"}", descriptor, node) {
            dropRoot || (it is YamlMappingNode && it["child"] != null)
        }
    }

    fun cases(): List<ReductionCase> {
        val format = YamlSerialization(encodeDefaults = true, ignoreUnknownKeys = true)
        val loader = YamlConfigValueLoader(serializer<Config>(), format)
        val expected = format.encodeToNode(Config("keep"))
        val itemType = serializer<List<Nested>>().descriptor
        val items = sequenceOf(*(0 until 128).map { stringMappingOf("a" to i(it), "extra" to s("keep")) }.toTypedArray())
        val collectionDescriptor = objectDescriptor("UnknownCollection", listOf("items" to itemType, "port" to serializer<Int>().descriptor))
        return listOf(
            ReductionCase("scalar-reject", serializer<Int>().descriptor, i(1)) { false },
            wide(64, false), wide(64, true), deep(24, true), deep(24, false),
            ReductionCase("unknown-collection", collectionDescriptor, stringMappingOf("items" to items, "port" to i(1))) { true },
            ReductionCase("codec-defaults", serializer<Config>().descriptor, expected) {
                try { format.encodeToNode(loader.load(null, it)) == expected }
                catch (_: kotlinx.serialization.SerializationException) { false }
            },
        )
    }
}
