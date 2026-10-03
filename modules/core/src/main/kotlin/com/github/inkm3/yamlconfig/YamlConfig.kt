package com.github.inkm3.yamlconfig

public class YamlConfig<T : Any> internal constructor(
    private val loadSession: () -> YamlConfigSession<T>,
) {
    public fun load(): YamlConfigSession<T> = loadSession()
}
