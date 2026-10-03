package com.github.inkm3.yamlconfig.spi

import com.github.inkm3.yamlconfig.source.YamlInput
import com.github.inkm3.yamlconfig.source.YamlOutput

public interface YamlEngine {
    public fun parse(input: YamlInput): YamlDocument

    public fun write(editor: YamlEditor, output: YamlOutput): Unit
}