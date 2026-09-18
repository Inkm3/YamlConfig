package com.github.inkm3.yamlconfig.path

import com.github.inkm3.yamlconfig.node.YamlNode

internal sealed interface YamlNodeLookupResult {

    data class Found(val node: YamlNode): YamlNodeLookupResult
    data class Missing(val path: YamlPath): YamlNodeLookupResult

    public data class ExpectedMapping(val path: YamlPath, val actual: YamlNode): YamlNodeLookupResult
    public data class ExpectedSequence(val path: YamlPath, val actual: YamlNode): YamlNodeLookupResult

    public data class IndexOutOfBounds(val path: YamlPath, val index: Int, val size: Int): YamlNodeLookupResult

}