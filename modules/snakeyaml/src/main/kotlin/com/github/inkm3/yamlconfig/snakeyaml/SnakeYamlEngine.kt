package com.github.inkm3.yamlconfig.snakeyaml

import com.github.inkm3.yamlconfig.exception.YamlParseException
import com.github.inkm3.yamlconfig.exception.YamlWriteException
import com.github.inkm3.yamlconfig.snakeyaml.document.SnakeYamlDocument
import com.github.inkm3.yamlconfig.snakeyaml.document.SnakeYamlEditor
import com.github.inkm3.yamlconfig.snakeyaml.internal.SnakeYamlDecodeException
import com.github.inkm3.yamlconfig.snakeyaml.internal.SnakeYamlNodeDecoder
import com.github.inkm3.yamlconfig.snakeyaml.internal.SnakeYamlNodeValidator
import com.github.inkm3.yamlconfig.source.YamlInput
import com.github.inkm3.yamlconfig.source.YamlOutput
import com.github.inkm3.yamlconfig.spi.YamlDocument
import com.github.inkm3.yamlconfig.spi.YamlEditor
import com.github.inkm3.yamlconfig.spi.YamlEngine
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.lowlevel.Compose
import org.snakeyaml.engine.v2.api.lowlevel.Present
import org.snakeyaml.engine.v2.api.lowlevel.Serialize
import org.snakeyaml.engine.v2.exceptions.YamlEngineException
import org.snakeyaml.engine.v2.schema.CoreSchema
import java.io.IOException
import java.io.UncheckedIOException

public class SnakeYamlEngine public constructor(): YamlEngine {

    override fun parse(input: YamlInput): YamlDocument {
        try {
            val settings = LoadSettings.builder()
                .setLabel(input.description)
                .setParseComments(true)
                .setAllowDuplicateKeys(false)
                .setSchema(CoreSchema())
                .build()

            val nativeRoot = input.openReader().use { reader ->
                Compose(settings)
                    .composeReader(reader)
                    .orElse(null)
            }

            val logicalRoot = nativeRoot?.let { root ->
                SnakeYamlNodeValidator.validate(root)
                SnakeYamlNodeDecoder.decode(root)
            }

            return SnakeYamlDocument(nativeRoot, logicalRoot)
        } catch (exception: YamlEngineException) {
            throw YamlParseException(
                input.description,
                buildString {
                    append("Failed to parse YAML from ${input.description}")
                    exception.message?.let { detail -> append(": $detail") }
                },
                exception,
            )
        } catch (exception: IOException) {
            throw YamlParseException(
                input.description,
                "Failed to read YAML from ${input.description}",
                exception,
            )
        } catch (exception: UncheckedIOException) {
            throw YamlParseException(
                input.description,
                "Failed to read YAML from ${input.description}",
                exception,
            )
        } catch (exception: SnakeYamlDecodeException) {
            throw YamlParseException(
                input.description,
                "Unsupported YAML structure: ${exception.message}",
                exception,
            )
        }

    }

    override fun write(editor: YamlEditor, output: YamlOutput): Unit {
        require(editor is SnakeYamlEditor) {
            "Editor must be created by SnakeYamlEngine"
        }

        try {
            output.beginWrite().use { transaction ->
                val root = editor.nativeRoot
                if (root != null) {
                    val settings = DumpSettings.builder()
                        .setDumpComments(true)
                        .setSchema(CoreSchema())
                        .build()

                    val events = Serialize(settings)
                        .serializeOne(root)

                    val yaml = Present(settings)
                        .emitToString(events.iterator())

                    transaction.writer.write(yaml)
                }

                transaction.commit()
            }
        } catch (exception: YamlEngineException) {
            throw YamlWriteException(
                output.description,
                buildString {
                    append("Failed to write YAML to ${output.description}")
                    exception.message?.let { detail -> append(": $detail") }
                },
                exception,
            )
        } catch (exception: IOException) {
            throw YamlWriteException(
                output.description,
                "Failed to write YAML to ${output.description}",
                exception,
            )
        } catch (exception: UncheckedIOException) {
            throw YamlWriteException(
                output.description,
                "Failed to write YAML to ${output.description}",
                exception,
            )
        }
    }
}