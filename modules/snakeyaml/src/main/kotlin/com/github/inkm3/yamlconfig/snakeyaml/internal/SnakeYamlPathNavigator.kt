package com.github.inkm3.yamlconfig.snakeyaml.internal

import com.github.inkm3.yamlconfig.exception.YamlEditException
import com.github.inkm3.yamlconfig.path.YamlPath
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.NodeTuple
import org.snakeyaml.engine.v2.nodes.SequenceNode

internal object SnakeYamlPathNavigator {

    internal data class Target(
        val parent: Node,
        val parentPath: YamlPath,
        val segment: YamlPath.Segment,
    )

    internal fun forSet(root: Node, path: YamlPath): Target {
        require(!path.isRoot)

        var current = root
        var currentPath = YamlPath.root()

        val lastIndex = path.segments.lastIndex
        for (index in 0 until lastIndex) {
            val segment = path.segments[index]
            val next = path.segments[index + 1]
            when (segment) {
                is YamlPath.Segment.Key -> {
                    val mapping = current as? MappingNode
                        ?: throw SnakeYamlNodes.expectedMapping(currentPath, current)

                    val tupleIndex = SnakeYamlNodes.findKeyIndex(mapping, segment.key)

                    current = if (tupleIndex >= 0) {
                        mapping.value[tupleIndex].valueNode
                    } else {
                        if (next !is YamlPath.Segment.Key) {
                            throw YamlEditException(
                                currentPath.child(segment.key),
                                "Cannot automatically create a sequence while setting $path",
                            )
                        }

                        val child = SnakeYamlNodes.newMapping()
                        mapping.value.add(
                            NodeTuple(
                                SnakeYamlNodeEncoder.encodeKey(segment.key),
                                child
                            )
                        )

                        child
                    }

                    currentPath = currentPath.child(segment.key)
                }

                is YamlPath.Segment.Index -> {
                    val sequence = current as? SequenceNode
                        ?: throw SnakeYamlNodes.expectedSequence(currentPath, current)

                    if (segment.index >= sequence.value.size) {
                        throw indexOutOfBounds(
                            currentPath.child(segment.index),
                            segment.index,
                            sequence.value.size,
                        )
                    }

                    current = sequence.value[segment.index]
                    currentPath = currentPath.child(segment.index)
                }
            }
        }

        return Target(current, currentPath, path.segments.last())
    }

    internal fun findTarget(root: Node, path: YamlPath): Target? {
        require(!path.isRoot)

        var current = root
        var currentPath = YamlPath.root()

        val lastIndex = path.segments.lastIndex
        for (index in 0 until lastIndex) {
            val segment = path.segments[index]
            when (segment) {
                is YamlPath.Segment.Key -> {
                    val mapping = current as? MappingNode
                        ?: throw SnakeYamlNodes.expectedMapping(currentPath, current)

                    val tupleIndex = SnakeYamlNodes.findKeyIndex(mapping, segment.key)
                    if (tupleIndex < 0) {
                        return null
                    }

                    current = mapping.value[tupleIndex].valueNode
                    currentPath = currentPath.child(segment.key)
                }

                is YamlPath.Segment.Index -> {
                    val sequence = current as? SequenceNode
                        ?: throw SnakeYamlNodes.expectedSequence(currentPath, current)

                    if (segment.index >= sequence.value.size) {
                        return null
                    }

                    current = sequence.value[segment.index]
                    currentPath = currentPath.child(segment.index)
                }
            }
        }

        return Target(current, currentPath, path.segments.last())
    }

    internal fun requireNode(root: Node?, path: YamlPath): Node {
        var current = root
            ?: throw YamlEditException(
                path,
                "Cannot resolve $path because the YAML document is empty"
            )

        if (path.isRoot) {
            return current
        }

        var currentPath = YamlPath.root()
        for (segment in path.segments) {
            when (segment) {
                is YamlPath.Segment.Key -> {
                    val mapping = current as? MappingNode
                        ?: throw SnakeYamlNodes.expectedMapping(
                            currentPath,
                            current
                        )

                    val nextPath = currentPath.child(segment.key)
                    val index = SnakeYamlNodes.findKeyIndex(
                        mapping,
                        segment.key
                    )
                    if (index < 0) {
                        throw YamlEditException(
                            nextPath,
                            "Mapping key does not exist at $nextPath",
                        )
                    }


                    current = mapping.value[index].valueNode
                    currentPath = nextPath
                }

                is YamlPath.Segment.Index -> {
                    val sequence = current as? SequenceNode
                        ?: throw SnakeYamlNodes.expectedSequence(
                            currentPath,
                            current
                        )

                    val nextPath = currentPath.child(segment.index)
                    if (segment.index >= sequence.value.size) {
                        throw indexOutOfBounds(
                            nextPath,
                            segment.index,
                            sequence.value.size,
                        )
                    }

                    current = sequence.value[segment.index]
                    currentPath = nextPath
                }
            }
        }

        return current
    }

    private fun indexOutOfBounds(
        path: YamlPath,
        index: Int,
        size: Int
    ): YamlEditException {
        return YamlEditException(
            path,
            "Sequence index $index is out of bounds at $path (size: $size)",
        )
    }
}