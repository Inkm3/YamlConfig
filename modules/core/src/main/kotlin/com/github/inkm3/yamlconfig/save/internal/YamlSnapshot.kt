package com.github.inkm3.yamlconfig.save.internal

import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.schema.YamlSchema

internal object YamlSnapshot {
    internal fun <T> copy(schema: YamlSchema<T>, value: T): T {
        return schema.decode(schema.encode(value), YamlPath.root())
    }
}