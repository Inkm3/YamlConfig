package com.github.inkm3.yamlconfig.schema

import com.github.inkm3.yamlconfig.schema.collection.YamlListSchema
import com.github.inkm3.yamlconfig.schema.collection.YamlMapSchema
import com.github.inkm3.yamlconfig.schema.objectmapping.YamlObjectSchemaBuilder
import com.github.inkm3.yamlconfig.schema.scalar.*
import com.github.inkm3.yamlconfig.schema.wrapper.YamlNullableSchema

public fun str(): YamlKeySchema<String> = YamlStringSchema
public fun int(): YamlKeySchema<Int> = YamlIntSchema
public fun boolean(): YamlKeySchema<Boolean> = YamlBooleanSchema
public fun long(): YamlKeySchema<Long> = YamlLongSchema
public fun float(): YamlKeySchema<Float> = YamlFloatSchema
public fun double(): YamlKeySchema<Double> = YamlDoubleSchema


public fun <T> nullable(schema: YamlSchema<T>): YamlSchema<T?> {
    return YamlNullableSchema(schema)
}

public fun <T> list(schema: YamlSchema<T>): YamlSchema<List<T>> {
    return YamlListSchema(schema)
}
public fun <K, V> map(keySchema: YamlKeySchema<K>, valueSchema: YamlSchema<V>): YamlSchema<Map<K, V>> {
    return YamlMapSchema(keySchema, valueSchema)
}

/**
 * Creates an object schema using [factory].
 *
 * [factory] may be invoked multiple times while loading, saving,
 * encoding, or creating snapshots.
 *
 * Each invocation must return an independent object whose initial
 * property values are logically equivalent to those produced by
 * every other invocation. The factory should not have externally
 * visible side effects.
 */
public fun <T : Any> obj(factory: () -> T, block: YamlObjectSchemaBuilder<T>.() -> Unit): YamlSchema<T> {
    val builder = YamlObjectSchemaBuilder<T>()
    builder.block()
    return builder.build(factory)
}

/**
 * Root-oriented alias of [obj].
 */
public fun <T : Any> yamlObject(factory: () -> T, block: YamlObjectSchemaBuilder<T>.() -> Unit): YamlSchema<T> {
    return obj(factory, block)
}