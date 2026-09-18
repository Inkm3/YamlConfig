package com.github.inkm3.yamlconfig.save.internal

import com.github.inkm3.yamlconfig.context.YamlWriteContext
import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlNodeLookupResult
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.path.YamlPathResolver
import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.schema.YamlSchema

internal class YamlSaveContext(
    internal val writer: YamlWriteContext,
    internal val mode: YamlSaveMode,
    private val defaultsRoot: YamlNode?,
) {

    internal fun lookupDefault(path: YamlPath): YamlNodeLookupResult {
        return YamlPathResolver.lookup(defaultsRoot, path)
    }

    internal fun lookupUser(path: YamlPath): YamlNodeLookupResult {
        return writer.lookup(path)
    }

    internal fun withoutYamlDefaults(): YamlSaveContext {
        if (defaultsRoot == null) {
            return this
        }

        return YamlSaveContext(
            writer,
            mode,
            null,
        )
    }

    internal fun <T> resolveComparableDefault(
        schema: YamlSchema<T>,
        path: YamlPath,
        fallbackDefault: YamlSaveValue<T>
    ): YamlSaveValue<T> {
        return when (val result = lookupDefault(path)) {
            is YamlNodeLookupResult.Found -> {
                try {
                    YamlSaveValue.Present(schema.decode(result.node, path))
                } catch (_: YamlSchemaException) {
                    YamlSaveValue.Missing
                }
            }

            is YamlNodeLookupResult.Missing -> fallbackDefault

            is YamlNodeLookupResult.ExpectedMapping,
            is YamlNodeLookupResult.ExpectedSequence,
            is YamlNodeLookupResult.IndexOutOfBounds -> YamlSaveValue.Missing

        }

    }
}