package com.github.inkm3.yamlconfig.schema

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.path.YamlPath

public abstract class YamlKeySchema<T> protected constructor(): YamlSchema<T>()  {

    public abstract fun decodeKey(key: YamlMapKey, path: YamlPath): T
    public abstract fun encodeKey(value: T): YamlMapKey

}