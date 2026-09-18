package com.github.inkm3.yamlconfig.schema.objectmapping

import com.github.inkm3.yamlconfig.schema.YamlSchema
import kotlin.reflect.KMutableProperty1

public class YamlObjectSchemaBuilder<T : Any> internal constructor() {

    private val fields = linkedMapOf<String, YamlObjectField<T>>()

    public fun <V> field(name: String, property: KMutableProperty1<T, V>, schema: YamlSchema<V>): Unit {
        addField(name, property, schema, false)
    }

    public fun <V> field(property: KMutableProperty1<T, V>, schema: YamlSchema<V>): Unit {
        field(property.name, property, schema)
    }

    public fun <V> optionalField(name: String, property: KMutableProperty1<T, V>, schema: YamlSchema<V>): Unit {
        addField(name, property, schema, true)
    }

    private fun <V> addField(name: String, property: KMutableProperty1<T, V>, schema: YamlSchema<V>, optional: Boolean): Unit {
        require(name !in fields) { "Duplicate field name: $name" }

        fields[name] = DefaultYamlObjectField(name, property, schema, optional)
    }

    internal fun build(factory: () -> T): YamlObjectSchema<T> {
        return YamlObjectSchema(factory, fields.values.toList());
    }
}