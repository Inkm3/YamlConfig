package com.github.inkm3.yamlconfig.path

import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.node.YamlScalarKind
import java.util.*

public class YamlPath private constructor(
    segments: List<Segment>,
) {

    public val segments: List<Segment> = Collections.unmodifiableList(segments)

    public val isRoot: Boolean get() = segments.isEmpty()

    public fun child(key: YamlMapKey): YamlPath {
        return YamlPath(segments + Segment.Key(key))
    }

    public fun child(key: String): YamlPath {
        return child(YamlMapKey(key, YamlScalarKind.STRING))
    }

    public fun child(index: Int): YamlPath {
        require(index >= 0) {
            "Index must not be negative: $index"
        }

        return YamlPath(segments + Segment.Index(index))
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other is YamlPath && segments == other.segments
    }

    override fun hashCode(): Int {
        return segments.hashCode()
    }

    override fun toString(): String {
        if (segments.isEmpty()) {
            return "$"
        }

        return buildString {
            append("$")

            for (segment in segments) {
                when (segment) {
                    is Segment.Key -> {
                        appendKey(segment.key)
                    }

                    is Segment.Index -> {
                        append("[${segment.index}]")
                    }
                }
            }
        }
    }

    private fun isSimpleStringKey(value: String): Boolean {
        if (value.isEmpty()) {
            return false
        }

        if (!value.first().isLetter()
            && value.first() != '_') {
            return false
        }

        return value.drop(1).all { char ->
            char.isLetterOrDigit() || char == '_' || char == '-'
        }
    }

    private fun StringBuilder.appendKey(key: YamlMapKey) {
        when (key.kind) {
            YamlScalarKind.STRING -> {
                if (isSimpleStringKey(key.value)) {
                    append('.')
                    appendEscapedString(key.value)
                } else {
                    append("{\"")
                    appendEscapedString(key.value)
                    append("\"}")
                }
            }

            YamlScalarKind.BOOLEAN,
            YamlScalarKind.INTEGER,
            YamlScalarKind.FLOAT -> {
                append("{${key.value}}")
            }

            YamlScalarKind.NULL -> {
                append("{null}")
            }
        }
    }

    private fun StringBuilder.appendEscapedString(value: String) {
        for (char in value) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }

    public sealed interface Segment {

        public data class Key(
            public val key: YamlMapKey,
        ): Segment

        public data class Index(
            public val index: Int,
        ): Segment
    }

    public companion object {
        private val ROOT = YamlPath(emptyList())

        public fun root(): YamlPath {
            return ROOT
        }
    }
}