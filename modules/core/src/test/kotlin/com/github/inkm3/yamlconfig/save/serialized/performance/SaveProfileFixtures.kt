package com.github.inkm3.yamlconfig.save.serialized.performance

import com.github.inkm3.yamlconfig.load.internal.YamlConfigValueLoader
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlSaveCandidate
import com.github.inkm3.yamlconfig.save.internal.serialized.YamlSaveCandidatePlanner
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.serializer

@Serializable
internal data class ProfileItem(val name: String, val port: Int = 25565)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class ProfileConfig(
    val token: String,
    val base: Int = 10,
    val derived: Int = base + 1,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val port: Int = 25565,
    val items: List<ProfileItem> = emptyList(),
    val labels: Map<String, Int> = emptyMap(),
)

internal data class ProfileOutcome(val candidate: YamlSaveCandidate?, val error: SerializationException?, val reloads: Int)

internal class SaveProfileCase(
    val name: String,
    val descriptor: SerialDescriptor,
    val baseline: YamlNode,
    val expected: YamlNode,
    val user: YamlNode?,
    val mode: YamlSaveMode,
    val normalize: (YamlNode?) -> YamlNode,
) {
    fun run(reference: Boolean): ProfileOutcome {
        var count = 0
        val callback = { root: YamlNode? -> count++; normalize(root) }
        return try {
            val candidate = if (reference) ReferenceCandidatePlanner(descriptor, callback)
                .plan(baseline, expected, user, mode)
            else YamlSaveCandidatePlanner(descriptor, callback).plan(baseline, expected, user, mode)
            ProfileOutcome(candidate, null, count)
        } catch (error: SerializationException) { ProfileOutcome(null, error, count) }
    }
}

internal object SaveProfileFixtures {
    val format = YamlSerialization(encodeDefaults = true, ignoreUnknownKeys = true)

    fun <T> case(name: String, serializer: KSerializer<T>, before: T, after: T, user: YamlNode?,
        defaults: YamlNode? = null, mode: YamlSaveMode = YamlSaveMode.PRESERVE_OVERRIDES): SaveProfileCase {
        val loader = YamlConfigValueLoader(serializer, format)
        return SaveProfileCase(name, serializer.descriptor, format.encodeToNode(serializer, before),
            format.encodeToNode(serializer, after), user, mode) { format.encodeToNode(serializer, loader.load(defaults, it)) }
    }

    fun cases(): List<SaveProfileCase> {
        val ser = serializer<ProfileConfig>()
        val basic = ProfileConfig("token")
        val large = basic.copy(port = 40000, items = (0 until 64).map { ProfileItem("item-$it") },
            labels = (0 until 64).associate { "label-$it" to it })
        val dependent = basic.copy(base = 100, derived = 101)
        return listOf(
            case("scalar-preserve", serializer<Int>(), 1, 2, i(1)),
            case("object-preserve", ser, basic, basic.copy(token = "updated"), format.encodeToNode(basic)),
            case("dependent-repair", ser, dependent, dependent.copy(base = 10),
                stringMappingOf("token" to s("token"), "base" to i(100))),
            case("omitted-retained", ser, basic, basic.copy(token = "updated"),
                stringMappingOf("token" to s("token"), "port" to i(25565)), stringMappingOf("port" to i(30000))),
            case("omitted-unrepresentable", ser, basic.copy(port = 40000), basic,
                stringMappingOf("token" to s("token"), "port" to i(40000)), stringMappingOf("port" to i(30000))),
            case("large-unrepresentable", ser, large, large.copy(port = 25565),
                format.encodeToNode(large), stringMappingOf("port" to i(30000))),
            case("minimal-reduction", ser, large, large, format.encodeToNode(large),
                mode = YamlSaveMode.MINIMAL_DIFFERENCE),
            case("sequence-move-update", serializer<List<ProfileItem>>(), large.items,
                large.items.reversed().mapIndexed { index, item -> if (index == 0) item.copy(port = 30000) else item },
                format.encodeToNode(large.items)),
        )
    }
}
