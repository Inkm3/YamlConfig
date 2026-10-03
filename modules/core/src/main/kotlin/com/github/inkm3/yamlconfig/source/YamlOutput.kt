package com.github.inkm3.yamlconfig.source

public interface YamlOutput {

    public val description: String

    public fun beginWrite(): YamlWriteTransaction

}