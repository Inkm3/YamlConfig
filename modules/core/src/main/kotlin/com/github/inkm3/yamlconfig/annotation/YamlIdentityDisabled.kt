package com.github.inkm3.yamlconfig.annotation

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Disables identity matching for instances of this class in Lists. Exact-value
 * moves and legacy positional updates remain enabled. Cannot be combined with
 * [YamlIdentity] properties. Applies to this class, not all its nested types.
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class YamlIdentityDisabled
