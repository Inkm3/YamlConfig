package com.github.inkm3.yamlconfig.source

import java.io.Closeable
import java.io.Writer

public interface YamlWriteTransaction: Closeable {

    public val writer: Writer

    public fun commit(): Unit

    override fun close(): Unit

}