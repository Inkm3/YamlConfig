package com.github.inkm3.yamlconfig.source.internal

import com.github.inkm3.yamlconfig.exception.YamlConfigException
import com.github.inkm3.yamlconfig.exception.YamlWriteException
import com.github.inkm3.yamlconfig.source.YamlInput
import com.github.inkm3.yamlconfig.source.YamlMissingFilePolicy
import com.github.inkm3.yamlconfig.source.YamlSource
import java.io.IOException
import java.io.UncheckedIOException
import java.io.Writer

internal object YamlMissingFileHandler {

    internal fun handle(
        userSource: YamlSource,
        defaultsSource: YamlInput?,
        policy: YamlMissingFilePolicy,
    ) {
        if (userSource.exists()) {
            return
        }

        when (policy) {
            YamlMissingFilePolicy.DO_NOT_CREATE -> Unit
            YamlMissingFilePolicy.CREATE_EMPTY -> createEmpty(userSource)
            YamlMissingFilePolicy.COPY_DEFAULTS -> {
                val defaults = defaultsSource
                    ?: throw YamlConfigException(
                        "Cannot create ${userSource.description} by copying " +
                        "defaults: no defaults source is configured",
                    )

                copyDefaults(
                    userSource,
                    defaults
                )
            }

        }
    }

    private fun createEmpty(userSource: YamlSource) {
        write(
            userSource,
            "Failed to create empty YAML at ${userSource.description}",
        ) {
        }
    }

    private fun copyDefaults(
        userSource: YamlSource,
        defaultsSource: YamlInput,
    ) {
        write(
            userSource,
            "Failed to create ${userSource.description} " +
            "from ${defaultsSource.description}",
        ) { writer ->
            defaultsSource.openReader().use { reader ->
                reader.copyTo(writer)
            }

        }
    }

    private inline fun write(
        userSource: YamlSource,
        message: String,
        block: (Writer) -> Unit,
    ): Unit {
        try {
            userSource.beginWrite().use { transaction ->
                block(transaction.writer)
                transaction.commit()
            }
        } catch (exception: IOException) {
            throw YamlWriteException(
                userSource.description,
                message,
                exception,
            )
        } catch (exception: UncheckedIOException) {
            throw YamlWriteException(
                userSource.description,
                message,
                exception,
            )
        }
    }
}