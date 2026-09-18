package com.github.inkm3.yamlconfig.testsupport

import com.github.inkm3.yamlconfig.exception.YamlEditException
import com.github.inkm3.yamlconfig.node.*
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.source.YamlInput
import com.github.inkm3.yamlconfig.source.YamlOutput
import com.github.inkm3.yamlconfig.source.YamlSource
import com.github.inkm3.yamlconfig.source.YamlWriteTransaction
import com.github.inkm3.yamlconfig.spi.YamlDocument
import com.github.inkm3.yamlconfig.spi.YamlEditor
import com.github.inkm3.yamlconfig.spi.YamlEngine
import com.github.inkm3.yamlconfig.spi.editor.YamlMappingEditor
import com.github.inkm3.yamlconfig.spi.editor.YamlSequenceEditor
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter

internal sealed interface TestEditOperation {
    data class Set(val path: YamlPath) : TestEditOperation
    data class Remove(val path: YamlPath) : TestEditOperation
    data class MappingReplace(val path: YamlPath, val key: YamlMapKey) : TestEditOperation
    data class MappingInsert(val path: YamlPath, val key: YamlMapKey) : TestEditOperation
    data class MappingRemove(val path: YamlPath, val key: YamlMapKey) : TestEditOperation
    data class SequenceReplace(val path: YamlPath, val index: Int) : TestEditOperation
    data class SequenceInsert(val path: YamlPath, val index: Int) : TestEditOperation
    data class SequenceRemove(val path: YamlPath, val index: Int) : TestEditOperation
    data class SequenceMove(val path: YamlPath, val fromIndex: Int, val toIndex: Int) : TestEditOperation
}

internal class TestYamlSource(
    internal var rootNode: YamlNode?,
    override val description: String = "test-source",
) : YamlSource {
    internal var lastOperations: List<TestEditOperation> = emptyList()

    override fun openReader(): Reader = StringReader("")

    override fun beginWrite(): YamlWriteTransaction {
        val writer = StringWriter()
        return object : YamlWriteTransaction {
            override val writer = writer
            override fun commit() = Unit
            override fun close() = Unit
        }
    }
}

internal class TestYamlEngine : YamlEngine {
    internal var failNextWrite: Boolean = false

    override fun parse(input: YamlInput): YamlDocument {
        val source = input as? TestYamlSource
            ?: error("TestYamlEngine only supports TestYamlSource")
        return TestYamlDocument(source.rootNode)
    }

    override fun write(editor: YamlEditor, output: YamlOutput) {
        if (failNextWrite) {
            failNextWrite = false
            error("simulated write failure")
        }

        val source = output as? TestYamlSource
            ?: error("TestYamlEngine only supports TestYamlSource")
        val testEditor = editor as? TestYamlEditor
            ?: error("Unexpected editor implementation")
        source.rootNode = testEditor.root
        source.lastOperations = testEditor.operations.toList()
    }
}

internal class TestYamlDocument(
    override val root: YamlNode?,
) : YamlDocument {
    override fun editor(): YamlEditor = TestYamlEditor(root)
}

internal class TestYamlEditor(
    initialRoot: YamlNode?,
    internal val operations: MutableList<TestEditOperation> = mutableListOf(),
) : YamlEditor {
    private var currentRoot: YamlNode? = initialRoot

    override val root: YamlNode?
        get() = currentRoot

    override fun set(path: YamlPath, node: YamlNode) {
        currentRoot = if (path.isRoot) {
            node
        } else {
            setAt(currentRoot, path.segments, 0, node, path)
        }
        operations += TestEditOperation.Set(path)
    }

    override fun remove(path: YamlPath) {
        if (path.isRoot) {
            currentRoot = null
            operations += TestEditOperation.Remove(path)
            return
        }

        currentRoot = removeAt(currentRoot, path.segments, 0, path)
        operations += TestEditOperation.Remove(path)
    }

    override fun mapping(path: YamlPath): YamlMappingEditor {
        requireMapping(path)
        return MappingEditor(path)
    }

    override fun sequence(path: YamlPath): YamlSequenceEditor {
        requireSequence(path)
        return SequenceEditor(path)
    }

    override fun fork(): YamlEditor = TestYamlEditor(currentRoot)

    private fun requireNode(path: YamlPath): YamlNode {
        var node = currentRoot ?: throw YamlEditException(path, "YAML document is empty")
        var currentPath = YamlPath.root()
        for (segment in path.segments) {
            node = when (segment) {
                is YamlPath.Segment.Key -> {
                    val mapping = node as? YamlMappingNode
                        ?: throw YamlEditException(currentPath, "Expected mapping")
                    currentPath = currentPath.child(segment.key)
                    mapping[segment.key]
                        ?: throw YamlEditException(currentPath, "Missing mapping key")
                }
                is YamlPath.Segment.Index -> {
                    val sequence = node as? YamlSequenceNode
                        ?: throw YamlEditException(currentPath, "Expected sequence")
                    currentPath = currentPath.child(segment.index)
                    if (segment.index !in sequence.elements.indices) {
                        throw YamlEditException(currentPath, "Sequence index out of bounds")
                    }
                    sequence[segment.index]
                }
            }
        }
        return node
    }

    private fun requireMapping(path: YamlPath): YamlMappingNode =
        requireNode(path) as? YamlMappingNode
            ?: throw YamlEditException(path, "Expected mapping")

    private fun requireSequence(path: YamlPath): YamlSequenceNode =
        requireNode(path) as? YamlSequenceNode
            ?: throw YamlEditException(path, "Expected sequence")

    private inner class MappingEditor(
        private val path: YamlPath,
    ) : YamlMappingEditor {
        override fun replace(position: YamlMapKey, node: YamlNode) {
            val mapping = requireMapping(path)
            if (!mapping.containsKey(position)) {
                throw YamlEditException(path.child(position), "Mapping key does not exist")
            }
            val entries = LinkedHashMap(mapping.entries)
            entries[position] = node
            replaceCollection(path, YamlMappingNode(entries))
            operations += TestEditOperation.MappingReplace(path, position)
        }

        override fun insert(position: YamlMapKey, node: YamlNode) {
            val mapping = requireMapping(path)
            if (mapping.containsKey(position)) {
                throw YamlEditException(path.child(position), "Mapping key already exists")
            }
            val entries = LinkedHashMap(mapping.entries)
            entries[position] = node
            replaceCollection(path, YamlMappingNode(entries))
            operations += TestEditOperation.MappingInsert(path, position)
        }

        override fun remove(position: YamlMapKey) {
            val mapping = requireMapping(path)
            if (!mapping.containsKey(position)) return
            val entries = LinkedHashMap(mapping.entries)
            entries.remove(position)
            replaceCollection(path, YamlMappingNode(entries))
            operations += TestEditOperation.MappingRemove(path, position)
        }
    }

    private inner class SequenceEditor(
        private val path: YamlPath,
    ) : YamlSequenceEditor {
        override val size: Int
            get() = requireSequence(path).size

        override fun replace(position: Int, node: YamlNode) {
            val sequence = requireSequence(path)
            if (position !in sequence.elements.indices) {
                throw YamlEditException(path, "Sequence index out of bounds: $position")
            }
            val elements = sequence.elements.toMutableList()
            elements[position] = node
            replaceCollection(path, YamlSequenceNode(elements))
            operations += TestEditOperation.SequenceReplace(path, position)
        }

        override fun insert(position: Int, node: YamlNode) {
            val sequence = requireSequence(path)
            if (position < 0 || position > sequence.size) {
                throw YamlEditException(path, "Sequence insertion index out of bounds: $position")
            }
            val elements = sequence.elements.toMutableList()
            elements.add(position, node)
            replaceCollection(path, YamlSequenceNode(elements))
            operations += TestEditOperation.SequenceInsert(path, position)
        }

        override fun remove(position: Int) {
            val sequence = requireSequence(path)
            if (position !in sequence.elements.indices) return
            val elements = sequence.elements.toMutableList()
            elements.removeAt(position)
            replaceCollection(path, YamlSequenceNode(elements))
            operations += TestEditOperation.SequenceRemove(path, position)
        }

        override fun move(fromIndex: Int, toIndex: Int) {
            val sequence = requireSequence(path)
            if (fromIndex !in sequence.elements.indices || toIndex !in sequence.elements.indices) {
                throw YamlEditException(path, "Sequence move index out of bounds")
            }
            if (fromIndex == toIndex) return
            val elements = sequence.elements.toMutableList()
            val node = elements.removeAt(fromIndex)
            elements.add(toIndex, node)
            replaceCollection(path, YamlSequenceNode(elements))
            operations += TestEditOperation.SequenceMove(path, fromIndex, toIndex)
        }
    }

    private fun replaceCollection(path: YamlPath, node: YamlNode) {
        currentRoot = if (path.isRoot) node else setAt(currentRoot, path.segments, 0, node, path)
    }

    private fun setAt(
        current: YamlNode?,
        segments: List<YamlPath.Segment>,
        offset: Int,
        replacement: YamlNode,
        fullPath: YamlPath,
    ): YamlNode {
        if (offset == segments.size) return replacement

        return when (val segment = segments[offset]) {
            is YamlPath.Segment.Key -> {
                val mapping = when (current) {
                    null -> YamlMappingNode(emptyMap())
                    is YamlMappingNode -> current
                    else -> throw YamlEditException(fullPath, "Expected mapping")
                }
                val child = mapping[segment.key]
                if (child == null && offset + 1 < segments.size && segments[offset + 1] is YamlPath.Segment.Index) {
                    throw YamlEditException(fullPath, "Missing sequence parent")
                }
                val updated = setAt(child, segments, offset + 1, replacement, fullPath)
                val entries = LinkedHashMap(mapping.entries)
                entries[segment.key] = updated
                YamlMappingNode(entries)
            }

            is YamlPath.Segment.Index -> {
                val sequence = current as? YamlSequenceNode
                    ?: throw YamlEditException(fullPath, "Expected sequence")
                if (segment.index !in sequence.elements.indices) {
                    throw YamlEditException(fullPath, "Sequence index out of bounds")
                }
                val elements = sequence.elements.toMutableList()
                elements[segment.index] = setAt(elements[segment.index], segments, offset + 1, replacement, fullPath)
                YamlSequenceNode(elements)
            }
        }
    }

    private fun removeAt(
        current: YamlNode?,
        segments: List<YamlPath.Segment>,
        offset: Int,
        fullPath: YamlPath,
    ): YamlNode? {
        current ?: return null
        val segment = segments[offset]
        val last = offset == segments.lastIndex
        return when (segment) {
            is YamlPath.Segment.Key -> {
                val mapping = current as? YamlMappingNode ?: return current
                if (!mapping.containsKey(segment.key)) return current
                val entries = LinkedHashMap(mapping.entries)
                if (last) {
                    entries.remove(segment.key)
                } else {
                    val child = entries[segment.key]
                    val updated = removeAt(child, segments, offset + 1, fullPath)
                    if (updated != null) entries[segment.key] = updated
                }
                YamlMappingNode(entries)
            }

            is YamlPath.Segment.Index -> {
                val sequence = current as? YamlSequenceNode ?: return current
                if (segment.index !in sequence.elements.indices) return current
                val elements = sequence.elements.toMutableList()
                if (last) {
                    elements.removeAt(segment.index)
                } else {
                    val updated = removeAt(elements[segment.index], segments, offset + 1, fullPath)
                    if (updated != null) elements[segment.index] = updated
                }
                YamlSequenceNode(elements)
            }
        }
    }
}

internal fun s(value: String): YamlScalarNode = YamlScalarNode(value, YamlScalarKind.STRING)
internal fun i(value: Int): YamlScalarNode = YamlScalarNode(value.toString(), YamlScalarKind.INTEGER)
internal fun b(value: Boolean): YamlScalarNode = YamlScalarNode(value.toString(), YamlScalarKind.BOOLEAN)
internal fun n(): YamlScalarNode = YamlScalarNode("null", YamlScalarKind.NULL)
internal fun mappingOf(vararg entries: Pair<YamlMapKey, YamlNode>): YamlMappingNode = YamlMappingNode(linkedMapOf(*entries))
internal fun stringMappingOf(vararg entries: Pair<String, YamlNode>): YamlMappingNode =
    YamlMappingNode(linkedMapOf(*entries.map { YamlMapKey(it.first) to it.second }.toTypedArray()))
internal fun sequenceOf(vararg elements: YamlNode): YamlSequenceNode = YamlSequenceNode(elements.toList())
