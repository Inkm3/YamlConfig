package com.github.inkm3.yamlconfig.context

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlNodeLookupResult
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.path.YamlPathResolver

internal class YamlReadContext internal constructor(
    private val defaultsRoot: YamlNode?,
    private val userRoot: YamlNode?,
) {

    internal fun lookup(path: YamlPath): YamlNodeLookupResult {
        return when (val userResult = YamlPathResolver.lookup(userRoot, path)) {
            is YamlNodeLookupResult.Missing -> YamlPathResolver.lookup(defaultsRoot, path)
            else -> userResult
        }
    }

}