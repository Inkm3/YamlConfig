package com.github.inkm3.yamlconfig.schema.objectmapping

import com.github.inkm3.yamlconfig.context.YamlReadContext
import com.github.inkm3.yamlconfig.exception.YamlSchemaException
import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlMappingNode
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import com.github.inkm3.yamlconfig.path.YamlNodeLookupResult
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.internal.YamlSaveContext
import com.github.inkm3.yamlconfig.save.internal.YamlSaveDispatcher
import com.github.inkm3.yamlconfig.save.internal.YamlSaveValue
import com.github.inkm3.yamlconfig.save.internal.map
import com.github.inkm3.yamlconfig.schema.YamlSchema
import kotlin.reflect.KMutableProperty1

internal interface YamlObjectField<T : Any> {
    val name: String

    fun read(context: YamlReadContext, parentPath: YamlPath, target: T): Unit

    fun decode(mapping: YamlMappingNode, parentPath: YamlPath, target: T): Unit
    fun encode(source: T, initial: T): Pair<YamlMapKey, YamlNode>?

    fun equivalent(first: T, second: T): Boolean

    fun save(
        context: YamlSaveContext,
        parentPath: YamlPath,
        current: T,
        baseline: T,
        fallbackDefault: YamlSaveValue<T>,
        initial: T,
    ): Unit
}

internal class DefaultYamlObjectField<T : Any, V>(
    override val name: String,
    private val property: KMutableProperty1<T, V>,
    private val schema: YamlSchema<V>,
    private val optional: Boolean,
): YamlObjectField<T> {
    private val key = YamlMapKey(name, YamlScalarKind.STRING)

    override fun read(context: YamlReadContext, parentPath: YamlPath, target: T): Unit {
        val path = parentPath.child(key)
        when (val result = context.lookup(path)) {
            is YamlNodeLookupResult.Missing -> {
                if (optional) {
                    return
                }

                throw YamlSchemaException(
                    result.path,
                    "Missing required value",
                )
            }

            else -> {
                property.set(target, schema.read(context, path))
            }
        }
    }

    override fun decode(mapping: YamlMappingNode, parentPath: YamlPath, target: T): Unit {
        val path = parentPath.child(key)
        val node = mapping[key]
        if (node == null) {
            if (optional) {
                return
            }

            throw YamlSchemaException(path, "Missing required value")
        }

        property.set(target, schema.decode(node, path))
    }

    override fun encode(source: T, initial: T): Pair<YamlMapKey, YamlNode>? {
        val value = property.get(source)

        if (optional && schema.equivalent(value, property.get(initial))) {
            return null
        }

        return key to schema.encode(value)
    }

    override fun equivalent(first: T, second: T): Boolean {
        return schema.equivalent(property.get(first), property.get(second))
    }

    override fun save(
        context: YamlSaveContext,
        parentPath: YamlPath,
        current: T,
        baseline: T,
        fallbackDefault: YamlSaveValue<T>,
        initial: T,
    ): Unit {
        val path = parentPath.child(key)
        val fieldDefault = resolveFallbackDefault(context, path, fallbackDefault, initial)

        YamlSaveDispatcher.save(
            schema,
            context,
            path,
            property.get(current),
            property.get(baseline),
            fieldDefault,
        )
    }

    private fun resolveFallbackDefault(
        context: YamlSaveContext,
        path: YamlPath,
        parentFallback: YamlSaveValue<T>,
        initial: T,
    ): YamlSaveValue<V> {
        val yamlDefault = context.lookupDefault(path)
        if (yamlDefault !is YamlNodeLookupResult.Missing) {
            return YamlSaveValue.Missing
        }

        val inherited = parentFallback.map(property::get)
        if (inherited is YamlSaveValue.Present) {
            return inherited
        }

        return if (optional) {
            YamlSaveValue.Present(property.get(initial))
        } else {
            YamlSaveValue.Missing
        }
    }
}