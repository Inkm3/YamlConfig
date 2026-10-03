package com.github.inkm3.yamlconfig.snakeyaml.internal

import com.github.inkm3.yamlconfig.node.YamlScalarKind
import org.snakeyaml.engine.v2.nodes.Tag

internal object SnakeYamlScalarCodec {

    internal fun decodeKind(tag: Tag): YamlScalarKind {
        return when (tag) {
            Tag.STR -> YamlScalarKind.STRING
            Tag.BOOL -> YamlScalarKind.BOOLEAN
            Tag.INT -> YamlScalarKind.INTEGER
            Tag.FLOAT -> YamlScalarKind.FLOAT
            Tag.NULL -> YamlScalarKind.NULL

            else -> throw SnakeYamlDecodeException(
                "Unsupported YAML scalar tag: $tag",
            )
        }
    }

    internal fun encodeTag(kind: YamlScalarKind): Tag {
        return when (kind) {
            YamlScalarKind.STRING -> Tag.STR
            YamlScalarKind.BOOLEAN -> Tag.BOOL
            YamlScalarKind.INTEGER -> Tag.INT
            YamlScalarKind.FLOAT -> Tag.FLOAT
            YamlScalarKind.NULL -> Tag.NULL
        }
    }
}