package com.github.inkm3.yamlconfig.snakeyaml.internal

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import org.snakeyaml.engine.v2.common.ScalarStyle

internal object SnakeYamlScalarStyle {

    internal fun valueStyle(kind: YamlScalarKind): ScalarStyle {
        return when (kind) {
            YamlScalarKind.STRING ->  ScalarStyle.DOUBLE_QUOTED
            YamlScalarKind.BOOLEAN,
            YamlScalarKind.INTEGER,
            YamlScalarKind.FLOAT,
            YamlScalarKind.NULL -> ScalarStyle.PLAIN
        }
    }

    internal fun keyStyle(key: YamlMapKey): ScalarStyle {
        return when {
            key.kind != YamlScalarKind.STRING -> ScalarStyle.PLAIN
            isSafePlainStringKey(key.value) -> ScalarStyle.PLAIN
            else -> ScalarStyle.DOUBLE_QUOTED
        }
    }

    private fun isSafePlainStringKey(value: String): Boolean {
        if (value.isEmpty()) {
            return false
        }

        val first = value.first()
        if (
            first != '_' &&
            first !in 'a'..'z' &&
            first !in 'A'..'Z'
        ) {
            return false
        }

        if (
            value.drop(1).any { char ->
                char != '_' &&
                char != '-' &&
                char !in 'a'..'z' &&
                char !in 'A'..'Z' &&
                char !in '0'..'9'
            }
        ) {
            return false
        }

        return when (value.lowercase()) {
            "true", "false",
            "null",
            "yes",  "no",
            "on",   "off",
            "y",    "n" -> false

            else -> true
        }
    }
}