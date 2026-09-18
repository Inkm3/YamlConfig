package com.github.inkm3.yamlconfig.save.internal

import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.save.internal.strategy.YamlAtomicSaveStrategy
import com.github.inkm3.yamlconfig.save.internal.strategy.YamlNullableSaveStrategy
import com.github.inkm3.yamlconfig.save.internal.strategy.YamlObjectSaveStrategy
import com.github.inkm3.yamlconfig.save.internal.strategy.YamlSaveStrategy
import com.github.inkm3.yamlconfig.save.internal.strategy.collection.YamlListSaveStrategy
import com.github.inkm3.yamlconfig.save.internal.strategy.collection.YamlMapSaveStrategy
import com.github.inkm3.yamlconfig.schema.YamlSchema
import com.github.inkm3.yamlconfig.schema.collection.YamlListSchema
import com.github.inkm3.yamlconfig.schema.collection.YamlMapSchema
import com.github.inkm3.yamlconfig.schema.objectmapping.YamlObjectSchema
import com.github.inkm3.yamlconfig.schema.wrapper.YamlNullableSchema

internal object YamlSaveDispatcher {

    internal fun <T> save(
        schema: YamlSchema<T>,
        context: YamlSaveContext,
        path: YamlPath,
        current: T,
        baseline: T,
        fallbackDefault: YamlSaveValue<T>,
    ): Unit {
        strategy(schema).save(
            context,
            path,
            current,
            baseline,
            fallbackDefault,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> strategy(schema: YamlSchema<T>): YamlSaveStrategy<T> {
        return when (schema) {
            is YamlObjectSchema<*> -> {
                val objectSchema = schema as YamlObjectSchema<Any>
                YamlObjectSaveStrategy(
                    objectSchema.factory,
                    objectSchema.fields,
                ) as YamlSaveStrategy<T>
            }

            is YamlListSchema<*> -> {
                val listSchema = schema as YamlListSchema<Any?>

                YamlListSaveStrategy(
                    listSchema,
                ) as YamlSaveStrategy<T>
            }

            is YamlMapSchema<*, *> -> {
                val mapSchema = schema as YamlMapSchema<Any?, Any?>
                YamlMapSaveStrategy(
                    mapSchema
                ) as YamlSaveStrategy<T>
            }

            is YamlNullableSchema<*> -> {
                val nullableSchema = schema as YamlNullableSchema<Any?>
                YamlNullableSaveStrategy(
                    nullableSchema,
                ) as YamlSaveStrategy<T>
            }

            else -> YamlAtomicSaveStrategy(schema)
        }
    }

}