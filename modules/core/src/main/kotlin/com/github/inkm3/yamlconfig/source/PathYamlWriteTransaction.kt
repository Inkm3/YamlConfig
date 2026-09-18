package com.github.inkm3.yamlconfig.source

import java.io.Writer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

internal class PathYamlWriteTransaction(
    private val target: Path,
    charset: Charset = StandardCharsets.UTF_8,
): YamlWriteTransaction {


    private val temp: Path

    override val writer: Writer

    private var committed: Boolean = false

    init {
        val parent = target.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }

        temp = Files.createTempFile(parent, ".yaml-config-", ".tmp")

        writer = Files.newBufferedWriter(temp, charset)
    }

    override fun commit(): Unit {
        writer.close()

        try {
            Files.move(
                temp,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                temp,
                target,
                StandardCopyOption.REPLACE_EXISTING
            )
        }

        committed = true
    }

    override fun close(): Unit {
        try {
            writer.close()
        } finally {
            if (!committed) {
                Files.deleteIfExists(temp)
            }
        }
    }
}