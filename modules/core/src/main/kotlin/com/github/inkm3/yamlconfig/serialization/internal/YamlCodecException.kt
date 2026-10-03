package com.github.inkm3.yamlconfig.serialization.internal

import com.github.inkm3.yamlconfig.path.YamlPath
import kotlinx.serialization.SerializationException

internal class YamlCodecException(
    path: YamlPath,
    message: String,
    cause: Throwable? = null,
) : SerializationException("$message at $path", cause)

internal fun failYaml(path: YamlPath, message: String): Nothing =
    throw YamlCodecException(path, message)

/** Attach the most specific available path once, preserving the original cause. */
internal inline fun <T> atYamlPath(path: YamlPath, block: () -> T): T = try {
    block()
} catch (error: YamlCodecException) {
    throw error
} catch (error: SerializationException) {
    throw YamlCodecException(path, error.message ?: "Serialization failed", error)
}
