package com.github.inkm3.yamlconfig.source

import java.io.Reader
import java.io.StringReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

public class PathYamlSource public constructor(
    path: Path,
    private val charset: Charset = Charsets.UTF_8,
): YamlSource {

    public val path: Path = path.toAbsolutePath().normalize()

    override val description: String get() = path.toString()

    override fun openReader(): Reader {
        if (Files.notExists(path)) {
            return StringReader("")
        }

        return Files.newBufferedReader(path, charset)
    }

    override fun beginWrite(): YamlWriteTransaction {
        return PathYamlWriteTransaction(path, charset)
    }
}