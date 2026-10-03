package com.github.inkm3.yamlconfig.serialization.internal

import com.github.inkm3.yamlconfig.path.YamlPath
import kotlinx.serialization.descriptors.SerialDescriptor

/** Do not silently encode unsigned bit patterns as negative signed numbers. */
internal fun requireSupportedInline(descriptor: SerialDescriptor, path: YamlPath) {
    when (descriptor.serialName) {
        "kotlin.UByte", "kotlin.UShort", "kotlin.UInt", "kotlin.ULong" ->
            failYaml(path, "Unsigned serialization is not supported: ${descriptor.serialName}")
    }
}
