package com.github.inkm3.yamlconfig.path

import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlSequenceNode

internal object YamlPathResolver {

    internal fun lookup(root: YamlNode?, path: YamlPath): YamlNodeLookupResult {
        if (root == null) {
            return YamlNodeLookupResult.Missing(path)
        }

        var currentNode: YamlNode = root
        var currentPath = YamlPath.root()

        for (segment in path.segments) {
            when (segment) {
                is YamlPath.Segment.Key -> {
                    if (currentNode !is YamlMappingNode) {
                        return YamlNodeLookupResult.ExpectedMapping(
                            currentPath,
                            currentNode
                        )
                    }

                    currentPath = currentPath.child(segment.key)
                    currentNode = currentNode[segment.key]
                        ?: return YamlNodeLookupResult.Missing(currentPath)

                }

                is YamlPath.Segment.Index -> {
                    if (currentNode !is YamlSequenceNode) {
                        return YamlNodeLookupResult.ExpectedSequence(
                            currentPath,
                            currentNode
                        )
                    }

                    val index = segment.index
                    if (index !in currentNode.elements.indices) {
                        return YamlNodeLookupResult.IndexOutOfBounds(
                            currentPath,
                            index,
                            currentNode.elements.size,
                        )
                    }

                    currentPath = currentPath.child(index)
                    currentNode = currentNode.elements[segment.index]
                }
            }
        }

        return YamlNodeLookupResult.Found(currentNode)
    }

}