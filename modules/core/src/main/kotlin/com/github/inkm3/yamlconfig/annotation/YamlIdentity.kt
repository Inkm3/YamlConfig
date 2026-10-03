package com.github.inkm3.yamlconfig.annotation

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Selects one serialized scalar property as the identity of an object in a List.
 * Overrides the implicit first-required-scalar convention; optional properties
 * are allowed explicitly. Values must be non-null and unique in BOTH lists.
 * Identity changes mean replacement, not rename. Does not affect encoding/loading.
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
public annotation class YamlIdentity
