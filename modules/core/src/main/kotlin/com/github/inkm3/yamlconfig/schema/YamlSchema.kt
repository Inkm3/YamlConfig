package com.github.inkm3.yamlconfig.schema

import com.github.inkm3.yamlconfig.context.YamlReadContext
import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.path.YamlPath
import com.github.inkm3.yamlconfig.schema.internal.YamlSchemaSupport

/**
 * Defines a bidirectional mapping between a logical YAML value and [T].
 *
 * Implementations must satisfy the following round-trip contract:
 *
 * ```
 * equivalent(
 *     value,
 *     decode(
 *         encode(value),
 *         YamlPath.root(),
 *     ),
 * ) == true
 * ```
 *
 * [decode] and [encode] should be deterministic and must not mutate
 * their input values.
 *
 * If [T] contains mutable state, decoding the result of [encode]
 * must produce an independent value suitable for use as a snapshot.
 *
 * [equivalent] is used for change detection and default-value
 * comparison during saving. Implementations should therefore define
 * an equivalence relation that is reflexive, symmetric and transitive.
 */
public abstract class YamlSchema<T> protected constructor() {

    /**
     * Decodes [node] into a value.
     *
     * [path] identifies the location of [node] and should be used when
     * reporting schema errors.
     *
     * @throws com.github.inkm3.yamlconfig.exception.YamlSchemaException if [node] cannot be represented by
     * this schema.
     */
    public abstract fun decode(node: YamlNode, path: YamlPath): T

    /**
     * Encodes [value] into a logical YAML node.
     *
     * The returned node must be decodable by this schema according to
     * the round-trip contract of [YamlSchema].
     */
    public abstract fun encode(value: T): YamlNode

    /**
     * Returns whether [first] and [second] represent the same logical
     * value for this schema.
     *
     * This method is used by the save system to detect changes and
     * compare values with defaults.
     */
    public open fun equivalent(first: T, second: T): Boolean {
        return first == second
    }

    internal open fun read(context: YamlReadContext, path: YamlPath): T {
        val node = YamlSchemaSupport.requireFound(
            context.lookup(path),
            "required value"
        )

        return decode(node, path)
    }
}