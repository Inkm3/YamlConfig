package com.github.inkm3.yamlconfig.snakeyaml

import com.github.inkm3.yamlconfig.exception.YamlConfigException
import com.github.inkm3.yamlconfig.schema.str
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import com.github.inkm3.yamlconfig.source.PathYamlSource
import com.github.inkm3.yamlconfig.source.YamlMissingFilePolicy
import com.github.inkm3.yamlconfig.yamlConfig
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.*

class SnakeYamlMissingFilePolicyTest {

    private val engine = SnakeYamlEngine()

    @Test
    fun doNotCreateLeavesMissingFileUntouched() {
        val directory =
            Files.createTempDirectory("yamlconfig-missing-file-test")

        try {
            val path = directory.resolve("config.yml")

            val config = yamlConfig(
                engine = engine,
                userSource = PathYamlSource(path),
                schema = str(),
                defaultsSource = StringYamlInput(
                    "\"default\"\n",
                    "defaults",
                ),
                missingFilePolicy =
                    YamlMissingFilePolicy.DO_NOT_CREATE,
            )

            val session = config.load()

            assertEquals("default", session.value)
            assertFalse(path.exists())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun createEmptyCreatesEmptyFile() {
        val directory =
            Files.createTempDirectory("yamlconfig-missing-file-test")

        try {
            val path = directory.resolve("config.yml")

            val config = yamlConfig(
                engine = engine,
                userSource = PathYamlSource(path),
                schema = str(),
                defaultsSource = StringYamlInput(
                    "\"default\"\n",
                    "defaults",
                ),
                missingFilePolicy =
                    YamlMissingFilePolicy.CREATE_EMPTY,
            )

            val session = config.load()

            assertEquals("default", session.value)
            assertTrue(path.exists())
            assertEquals("", path.readText())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun copyDefaultsCopiesOriginalYamlContent() {
        val directory =
            Files.createTempDirectory("yamlconfig-missing-file-test")

        try {
            val path = directory.resolve("config.yml")

            val defaults = """
                # Default value

                "default"
            """.trimIndent() + "\n"

            val config = yamlConfig(
                engine = engine,
                userSource = PathYamlSource(path),
                schema = str(),
                defaultsSource = StringYamlInput(
                    defaults,
                    "defaults",
                ),
                missingFilePolicy =
                    YamlMissingFilePolicy.COPY_DEFAULTS,
            )

            val session = config.load()

            assertEquals("default", session.value)
            assertTrue(path.exists())

            // コメント・空行・quoteを含めてそのままコピーされる
            assertEquals(defaults, path.readText())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun copyDefaultsDoesNotOverwriteExistingEmptyFile() {
        val directory =
            Files.createTempDirectory("yamlconfig-missing-file-test")

        try {
            val path = directory.resolve("config.yml")

            path.writeText("")

            val config = yamlConfig(
                engine = engine,
                userSource = PathYamlSource(path),
                schema = str(),
                defaultsSource = StringYamlInput(
                    "\"default\"\n",
                    "defaults",
                ),
                missingFilePolicy =
                    YamlMissingFilePolicy.COPY_DEFAULTS,
            )

            val session = config.load()

            assertEquals("default", session.value)

            // ファイルは存在していたのでCOPY_DEFAULTSしない
            assertEquals("", path.readText())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun copyDefaultsWithoutDefaultsFailsWhenFileIsMissing() {
        val directory =
            Files.createTempDirectory("yamlconfig-missing-file-test")

        try {
            val path = directory.resolve("config.yml")

            val config = yamlConfig(
                engine = engine,
                userSource = PathYamlSource(path),
                schema = str(),
                missingFilePolicy =
                    YamlMissingFilePolicy.COPY_DEFAULTS,
            )

            assertFailsWith<YamlConfigException> {
                config.load()
            }

            assertFalse(path.exists())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun copyDefaultsWithoutDefaultsLoadsExistingFile() {
        val directory =
            Files.createTempDirectory("yamlconfig-missing-file-test")

        try {
            val path = directory.resolve("config.yml")

            path.writeText("\"user\"\n")

            val config = yamlConfig(
                engine = engine,
                userSource = PathYamlSource(path),
                schema = str(),
                missingFilePolicy =
                    YamlMissingFilePolicy.COPY_DEFAULTS,
            )

            val session = config.load()

            assertEquals("user", session.value)
            assertEquals("\"user\"\n", path.readText())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun defaultsSourceUsesCopyDefaultsByDefault() {
        val directory =
            Files.createTempDirectory("yamlconfig-missing-file-test")

        try {
            val path = directory.resolve("config.yml")

            val defaults = """
            # default config
            "default"
        """.trimIndent() + "\n"

            val config = yamlConfig(
                engine = engine,
                userSource = PathYamlSource(path),
                schema = str(),
                defaultsSource = StringYamlInput(
                    defaults,
                    "defaults",
                ),
            )

            val session = config.load()

            assertEquals("default", session.value)
            assertTrue(path.exists())
            assertEquals(defaults, path.readText())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun noDefaultsSourceDoesNotCreateFileByDefault() {
        val directory =
            Files.createTempDirectory("yamlconfig-missing-file-test")

        try {
            val path = directory.resolve("config.yml")

            val config = yamlConfig(
                engine = engine,
                userSource = PathYamlSource(path),
                schema = str(),
            )

            assertFails {
                config.load()
            }

            assertFalse(path.exists())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun copyDefaultsCreatesMissingParentDirectories() {
        val directory =
            Files.createTempDirectory("yamlconfig-missing-file-test")

        try {
            val path = directory.resolve("nested/config.yml")

            val defaults = """
            # default config
            "default"
        """.trimIndent() + "\n"

            val config = yamlConfig(
                engine = engine,
                userSource = PathYamlSource(path),
                schema = str(),
                defaultsSource = StringYamlInput(
                    defaults,
                    "defaults",
                ),
                missingFilePolicy =
                    YamlMissingFilePolicy.COPY_DEFAULTS,
            )

            val session = config.load()

            assertEquals("default", session.value)
            assertTrue(path.exists())
            assertEquals(defaults, path.readText())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun createEmptyCreatesMissingParentDirectories() {
        val directory =
            Files.createTempDirectory("yamlconfig-missing-file-test")

        try {
            val path = directory.resolve("nested/config.yml")

            val config = yamlConfig(
                engine = engine,
                userSource = PathYamlSource(path),
                schema = str(),
                defaultsSource = StringYamlInput(
                    "\"default\"\n",
                    "defaults",
                ),
                missingFilePolicy =
                    YamlMissingFilePolicy.CREATE_EMPTY,
            )

            val session = config.load()

            assertEquals("default", session.value)
            assertTrue(path.exists())
            assertEquals("", path.readText())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun copyDefaultsDoesNotOverwriteExistingFile() {
        val directory =
            Files.createTempDirectory("yamlconfig-missing-file-test")

        try {
            val path = directory.resolve("config.yml")
            val userYaml = "\"user\"\n"

            path.writeText(userYaml)

            val config = yamlConfig(
                engine = engine,
                userSource = PathYamlSource(path),
                schema = str(),
                defaultsSource = StringYamlInput(
                    "\"default\"\n",
                    "defaults",
                ),
                missingFilePolicy =
                    YamlMissingFilePolicy.COPY_DEFAULTS,
            )

            val session = config.load()

            assertEquals("user", session.value)
            assertEquals(userYaml, path.readText())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun createEmptyDoesNotOverwriteExistingFile() {
        val directory =
            Files.createTempDirectory("yamlconfig-missing-file-test")

        try {
            val path = directory.resolve("config.yml")
            val userYaml = "\"user\"\n"

            path.writeText(userYaml)

            val config = yamlConfig(
                engine = engine,
                userSource = PathYamlSource(path),
                schema = str(),
                defaultsSource = StringYamlInput(
                    "\"default\"\n",
                    "defaults",
                ),
                missingFilePolicy =
                    YamlMissingFilePolicy.CREATE_EMPTY,
            )

            val session = config.load()

            assertEquals("user", session.value)
            assertEquals(userYaml, path.readText())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}