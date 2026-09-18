package com.github.inkm3.yamlconfig.source

import java.io.Reader

public interface YamlInput {

    public val description: String

    public fun openReader(): Reader

}