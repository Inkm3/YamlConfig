package com.github.inkm3.yamlconfig.spi

import com.github.inkm3.yamlconfig.node.YamlNode

public interface YamlDocument {
    public val root: YamlNode?
    public fun editor(): YamlEditor
}