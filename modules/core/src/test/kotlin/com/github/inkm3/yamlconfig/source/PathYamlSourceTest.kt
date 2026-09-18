package com.github.inkm3.yamlconfig.source

import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PathYamlSourceTest {
    @Test fun missingFileReadsAsEmptyWithoutCreatingFile() {
        val dir = Files.createTempDirectory("yamlconfig-core-test")
        try {
            val path = dir.resolve("config.yml")
            val source = PathYamlSource(path)
            assertEquals("", source.openReader().use { it.readText() })
            assertFalse(path.exists())
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test fun committedTransactionReplacesTargetAndCreatesParentDirectories() {
        val dir = Files.createTempDirectory("yamlconfig-core-test")
        try {
            val path = dir.resolve("nested/config.yml")
            val source = PathYamlSource(path)
            source.beginWrite().use { tx ->
                tx.writer.write("value: 1\n")
                tx.commit()
            }
            assertEquals("value: 1\n", path.readText())
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test fun uncommittedTransactionDoesNotModifyExistingFile() {
        val dir = Files.createTempDirectory("yamlconfig-core-test")
        try {
            val path = dir.resolve("config.yml")
            path.writeText("old\n")
            val source = PathYamlSource(path)
            source.beginWrite().use { tx -> tx.writer.write("new\n") }
            assertEquals("old\n", path.readText())
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
