package com.github.inkm3.yamlconfig.snakeyaml.serialization

import com.github.inkm3.yamlconfig.exception.YamlConfigException
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import com.github.inkm3.yamlconfig.source.PathYamlSource
import com.github.inkm3.yamlconfig.source.YamlMissingFilePolicy
import com.github.inkm3.yamlconfig.yamlConfig
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnakeYamlSerializedFilePolicyTest {
    private val engine = SnakeYamlEngine()

    private fun withSource(block: (Path, PathYamlSource) -> Unit) {
        val directory = Files.createTempDirectory("yaml-serializer-save-")
        try {
            val path = directory.resolve("config.yml")
            block(path, PathYamlSource(path))
        } finally {
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }

    @Test fun missingFileIsNotCreatedOnLoadButChangedSaveCreatesIt() = withSource { path, source ->
        val config = yamlConfig<SnakeSavedConfig>(engine, source)
        val session = config.load()
        assertFalse(Files.exists(path))
        session.save()
        assertFalse(Files.exists(path))
        session.value = session.value.copy(port = 30000)
        session.save()
        assertTrue(Files.exists(path))
        assertEquals(30000, config.load().value.port)
    }

    @Test fun configuredDefaultsAreCopiedExactlyByDefault() = withSource { path, source ->
        val text = "# copied default\nport: 30000\n"
        val config = yamlConfig<SnakeSavedConfig>(engine, source, defaultsSource = StringYamlInput(text))
        assertEquals(30000, config.load().value.port)
        assertEquals(text, Files.readString(path))
    }

    @Test fun explicitDoNotCreateStillLoadsAndLaterSavesDefaults() = withSource { path, source ->
        val config = yamlConfig<SnakeSavedConfig>(
            engine,
            source,
            defaultsSource = StringYamlInput("port: 30000\n"),
            missingFilePolicy = YamlMissingFilePolicy.DO_NOT_CREATE,
        )
        val session = config.load()
        assertEquals(30000, session.value.port)
        assertFalse(Files.exists(path))
        session.value = session.value.copy(port = 40000)
        session.save()
        assertEquals(40000, config.load().value.port)
    }

    @Test fun createEmptyDoesNotCopyDefaultsButUsesThemForLoad() = withSource { path, source ->
        val config = yamlConfig<SnakeSavedConfig>(
            engine,
            source,
            defaultsSource = StringYamlInput("port: 30000\n"),
            missingFilePolicy = YamlMissingFilePolicy.CREATE_EMPTY,
        )
        val session = config.load()
        assertEquals(30000, session.value.port)
        assertEquals("", Files.readString(path))
        session.save()
        assertEquals("", Files.readString(path))
    }

    @Test fun existingUserFileIsNeverOverwrittenByCopyDefaultsPolicy() = withSource { path, source ->
        val text = "port: 40000 # user\n"
        Files.writeString(path, text)
        val config = yamlConfig<SnakeSavedConfig>(engine, source, defaultsSource = StringYamlInput("port: 30000\n"))
        assertEquals(40000, config.load().value.port)
        assertEquals(text, Files.readString(path))
    }

    @Test fun existingEmptyFileIsNotOverwrittenByCopyDefaultsPolicy() = withSource { path, source ->
        Files.writeString(path, "")
        val config = yamlConfig<SnakeSavedConfig>(engine, source, defaultsSource = StringYamlInput("port: 30000\n"))
        assertEquals(30000, config.load().value.port)
        assertEquals("", Files.readString(path))
    }

    @Test fun copyDefaultsWithoutDefaultsFailsOnlyWhenFileIsMissing() = withSource { path, source ->
        val config = yamlConfig<SnakeSavedConfig>(
            engine,
            source,
            missingFilePolicy = YamlMissingFilePolicy.COPY_DEFAULTS,
        )
        assertFailsWith<YamlConfigException> { config.load() }
        assertFalse(Files.exists(path))

        Files.writeString(path, "port: 40000\n")
        assertEquals(40000, config.load().value.port)
    }

    @Test fun createPoliciesCreateMissingParentDirectories() {
        val directory = Files.createTempDirectory("yaml-serializer-parent-")
        try {
            for (policy in listOf(YamlMissingFilePolicy.COPY_DEFAULTS, YamlMissingFilePolicy.CREATE_EMPTY)) {
                val path = directory.resolve(policy.name.lowercase()).resolve("config.yml")
                val source = PathYamlSource(path)
                val config = yamlConfig<SnakeSavedConfig>(
                    engine,
                    source,
                    defaultsSource = StringYamlInput("port: 30000\n"),
                    missingFilePolicy = policy,
                )
                assertEquals(30000, config.load().value.port)
                assertTrue(Files.exists(path))
                if (policy == YamlMissingFilePolicy.COPY_DEFAULTS) {
                    assertEquals("port: 30000\n", Files.readString(path))
                } else {
                    assertEquals("", Files.readString(path))
                }
            }
        } finally {
            Files.walk(directory).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
    }
}
