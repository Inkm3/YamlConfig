package com.github.inkm3.yamlconfig.save.internal.session

internal fun interface YamlSessionSaver<T> {
    fun save(current: T)
}
