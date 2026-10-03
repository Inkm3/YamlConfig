package com.github.inkm3.yamlconfig.snakeyaml

import com.github.inkm3.yamlconfig.node.*
import com.github.inkm3.yamlconfig.snakeyaml.internal.SnakeYamlNodeCopy
import com.github.inkm3.yamlconfig.snakeyaml.internal.SnakeYamlNodeDecoder
import com.github.inkm3.yamlconfig.snakeyaml.internal.SnakeYamlNodeEncoder
import com.github.inkm3.yamlconfig.snakeyaml.internal.SnakeYamlScalarStyle
import org.snakeyaml.engine.v2.common.ScalarStyle
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.SequenceNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class SnakeYamlInternalCodecTest {
    @Test
    fun logicalTreeRoundTripsThroughNativeNodes() {
        val logical = YamlMappingNode(
            linkedMapOf(
                YamlMapKey("name") to YamlScalarNode("server", YamlScalarKind.STRING),
                YamlMapKey("enabled") to YamlScalarNode("true", YamlScalarKind.BOOLEAN),
                YamlMapKey("items") to YamlSequenceNode(
                    listOf(
                        YamlScalarNode("1", YamlScalarKind.INTEGER),
                        YamlScalarNode("2.5", YamlScalarKind.FLOAT),
                    )
                ),
            )
        )

        val native = SnakeYamlNodeEncoder.encode(logical)
        val decoded = SnakeYamlNodeDecoder.decode(native)
        assertEquals(logical, decoded)
    }

    @Test
    fun newStringValuesAreDoubleQuotedAtNativeLevel() {
        val native = SnakeYamlNodeEncoder.encode(
            YamlScalarNode("hello", YamlScalarKind.STRING)
        ) as org.snakeyaml.engine.v2.nodes.ScalarNode

        assertEquals(ScalarStyle.DOUBLE_QUOTED, native.scalarStyle)
    }

    @Test
    fun keyStylePolicyDistinguishesSafeAndAmbiguousStrings() {
        assertEquals(ScalarStyle.PLAIN, SnakeYamlScalarStyle.keyStyle(YamlMapKey("server_name")))
        assertEquals(ScalarStyle.DOUBLE_QUOTED, SnakeYamlScalarStyle.keyStyle(YamlMapKey("true")))
        assertEquals(ScalarStyle.DOUBLE_QUOTED, SnakeYamlScalarStyle.keyStyle(YamlMapKey("1")))
        assertEquals(
            ScalarStyle.PLAIN,
            SnakeYamlScalarStyle.keyStyle(YamlMapKey("1", YamlScalarKind.INTEGER)),
        )
    }

    @Test
    fun nodeCopyDeepCopiesSequenceAndMappingChildren() {
        val native = SnakeYamlNodeEncoder.encode(
            YamlMappingNode(
                linkedMapOf(
                    YamlMapKey("items") to YamlSequenceNode(
                        listOf(YamlScalarNode("value", YamlScalarKind.STRING))
                    )
                )
            )
        ) as MappingNode

        val copied = SnakeYamlNodeCopy.copy(native) as MappingNode
        val originalSequence = native.value.single().valueNode as SequenceNode
        val copiedSequence = copied.value.single().valueNode as SequenceNode

        assertNotSame(native, copied)
        assertNotSame(originalSequence, copiedSequence)
        assertNotSame(originalSequence.value.single(), copiedSequence.value.single())
        assertEquals(SnakeYamlNodeDecoder.decode(native), SnakeYamlNodeDecoder.decode(copied))
    }
}
