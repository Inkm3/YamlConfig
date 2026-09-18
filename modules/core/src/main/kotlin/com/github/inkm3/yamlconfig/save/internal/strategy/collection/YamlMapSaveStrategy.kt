package com.github.inkm3.yamlconfig.save.internal.strategy.collection

import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.path.YamlNodeLookupResult
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.internal.YamlSaveContext
import com.github.inkm3.yamlconfig.save.internal.YamlSaveDispatcher
import com.github.inkm3.yamlconfig.save.internal.YamlSaveValue
import com.github.inkm3.yamlconfig.save.internal.action.YamlSaveActionResolver
import com.github.inkm3.yamlconfig.save.internal.action.YamlWriteAction
import com.github.inkm3.yamlconfig.save.internal.strategy.YamlSaveStrategy
import com.github.inkm3.yamlconfig.schema.collection.YamlMapSchema
import com.github.inkm3.yamlconfig.spi.editor.YamlMappingEditor

internal class YamlMapSaveStrategy<K,V>(
    private val schema: YamlMapSchema<K, V>,
): YamlSaveStrategy<Map<K, V>> {

    override fun save(
        context: YamlSaveContext,
        path: YamlPath,
        current: Map<K, V>,
        baseline: Map<K, V>,
        fallbackDefault: YamlSaveValue<Map<K, V>>
    ) {
        when (YamlSaveActionResolver.resolve(
            schema,
            context,
            path,
            current,
            baseline,
            fallbackDefault
        )) {
            YamlWriteAction.KEEP -> return
            YamlWriteAction.SET -> Unit
            YamlWriteAction.REMOVE -> {
                context.writer.remove(path)
                return
            }
        }

        val userMapping = findUserMapping(context, path)
        if (userMapping == null) {
            context.writer.set(path, schema.encode(current))
            return
        }

        saveStructurally(
            context,
            path,
            current,
            baseline,
            userMapping,
        )
    }

    private fun findUserMapping(context: YamlSaveContext, path: YamlPath): YamlMappingNode? {
        return when (val result = context.lookupUser(path)) {
            is YamlNodeLookupResult.Found -> result.node as? YamlMappingNode
            else -> null
        }
    }

    private fun decodeUserKeys(mapping: YamlMappingNode, path: YamlPath): Map<K, YamlMapKey> {
        val result = LinkedHashMap<K, YamlMapKey>()
        for (yamlKey in mapping.entries.keys) {
            val entryPath = path.child(yamlKey)
            val key = schema.keySchema.decodeKey(yamlKey, entryPath)
            if (result.containsKey(key)) {
                throw YamlSchemaException(
                    entryPath,
                    "Duplicate mapping key after decoding",
                )
            }

            result[key] = yamlKey
        }

        return result
    }

    private fun saveStructurally(
        context: YamlSaveContext,
        path: YamlPath,
        current: Map<K, V>,
        baseline: Map<K, V>,
        userMapping: YamlMappingNode,
    ): Unit {
        val userKeys = decodeUserKeys(userMapping, path)
        val childContext = context.withoutYamlDefaults()
        val mappingEditor = context.writer.mapping(path)

        removeEntries(
            mappingEditor,
            current,
            baseline,
            userKeys,
        )

        saveExistingEntries(
            childContext,
            path,
            current,
            baseline,
            userKeys,
        )

        addEntries(
            mappingEditor,
            current,
            baseline,
        )
    }

    private fun removeEntries(
        editor: YamlMappingEditor,
        current: Map<K, V>,
        baseline: Map<K, V>,
        userKeys: Map<K, YamlMapKey>,
    ): Unit {
        for (key in baseline.keys) {
            if (current.containsKey(key)) {
                continue
            }

            val yamlKey = userKeys[key]
                ?: schema.keySchema.encodeKey(key)

            editor.remove(yamlKey)
        }
    }

    private fun saveExistingEntries(
        context: YamlSaveContext,
        path: YamlPath,
        current: Map<K, V>,
        baseline: Map<K, V>,
        userKeys: Map<K, YamlMapKey>,
    ): Unit {
        for ((key, currentValue) in current) {
            if (!baseline.containsKey(key)) {
                continue
            }

            val yamlKey = userKeys[key]
                ?: schema.keySchema.encodeKey(key)

            val baselineValue = baseline.getValue(key)

            YamlSaveDispatcher.save(
                schema.valueSchema,
                context,
                path.child(yamlKey),
                currentValue,
                baselineValue,
                YamlSaveValue.Missing,
            )
        }
    }

    private fun addEntries(
        editor: YamlMappingEditor,
        current: Map<K, V>,
        baseline: Map<K, V>,
    ): Unit {
        for ((key, value) in current) {
            if (baseline.containsKey(key)) {
                continue
            }

            val yamlKey = schema.keySchema.encodeKey(key)

            editor.insert(yamlKey, schema.valueSchema.encode(value))
        }
    }

}