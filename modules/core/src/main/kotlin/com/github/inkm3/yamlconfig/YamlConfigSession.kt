package com.github.inkm3.yamlconfig

import com.github.inkm3.yamlconfig.save.internal.session.YamlSessionSaver

public class YamlConfigSession<T> internal constructor(
    public var value: T,
    private val saver: YamlSessionSaver<T>,
) {
    public fun save(): Unit = saver.save(value)
}
