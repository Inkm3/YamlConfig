package com.github.inkm3.yamlconfig.snakeyaml.serialization

import com.github.inkm3.yamlconfig.exception.YamlWriteException
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.MemoryYamlSource
import com.github.inkm3.yamlconfig.snakeyaml.testsupport.StringYamlInput
import com.github.inkm3.yamlconfig.source.YamlSource
import com.github.inkm3.yamlconfig.source.YamlWriteTransaction
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
class SnakeYamlOmittedFailureTest {
    @Serializable private data class Config(
        @EncodeDefault(EncodeDefault.Mode.NEVER) val port: Int = 25565,
        val base: Int = 10, val derived: Int = base + 1,
    )
    @Serializable private data class Always(@EncodeDefault(EncodeDefault.Mode.ALWAYS) val port: Int = 25565)

    @Test fun failedCommitRetriesTheSamePreservingPlanFromThePreviousBaseline() {
        for (mode in YamlSaveMode.entries) {
            val original = "port: +25565 # keep\nbase: 100\n"
            val backing = MemoryYamlSource(original)
            var fail = true
            val source = object : YamlSource by backing {
                override fun beginWrite(): YamlWriteTransaction {
                    val delegate = backing.beginWrite()
                    return object : YamlWriteTransaction by delegate {
                        override fun commit() {
                            if (fail) throw IOException("not published")
                            delegate.commit()
                        }
                    }
                }
            }
            val config = yamlConfig<Config>(SnakeYamlEngine(), source,
                defaultsSource = StringYamlInput("port: 30000\n"), saveMode = mode)
            val session = config.load()
            session.value = session.value.copy(base = 10)
            assertFailsWith<YamlWriteException> { session.save() }
            assertEquals(original, backing.text)
            assertEquals(0, backing.commitCount)
            fail = false
            session.save()
            assertEquals(Config(25565, 10, 101), config.load().value)
            assertTrue(backing.text.contains("port: +25565 # keep"), backing.text)
            session.save()
            assertEquals(1, backing.commitCount)
        }
    }

    @Test fun unavailableDefaultValueDoesNotLeadToAnInventedOverride() {
        val original = "port: 40000 # original\n"
        val source = MemoryYamlSource(original)
        val config = yamlConfig<Config>(SnakeYamlEngine(), source,
            defaultsSource = StringYamlInput("port: 30000\n"))
        val session = config.load()
        session.value = Config()
        assertFailsWith<SerializationException> { session.save() }
        assertEquals(original, source.text)
        assertEquals(0, source.commitCount)
        session.value = Config(port = 50000)
        session.save()
        assertEquals(Config(port = 50000), config.load().value)
    }

    @Test fun standardAlwaysProvidesTheRequiredValueWithoutAnIndependentYamlAnnotation() {
        for (mode in YamlSaveMode.entries) {
            val source = MemoryYamlSource("port: 40000 # keep-comment\n")
            val config = yamlConfig<Always>(SnakeYamlEngine(), source,
                defaultsSource = StringYamlInput("port: 30000\n"), saveMode = mode)
            val session = config.load()
            session.value = Always()
            session.save()
            assertEquals(Always(), config.load().value)
            assertTrue(source.text.contains("port: 25565 # keep-comment"), source.text)
        }
    }
}
