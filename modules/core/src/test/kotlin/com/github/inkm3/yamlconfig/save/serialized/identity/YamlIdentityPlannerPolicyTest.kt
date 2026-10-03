package com.github.inkm3.yamlconfig.save.serialized.identity

import com.github.inkm3.yamlconfig.annotation.YamlIdentity
import com.github.inkm3.yamlconfig.annotation.YamlIdentityDisabled
import com.github.inkm3.yamlconfig.save.serialized.structure.StructuralPlannerFixtures.apply
import com.github.inkm3.yamlconfig.save.serialized.structure.StructuralPlannerFixtures.plan
import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import com.github.inkm3.yamlconfig.testsupport.i
import com.github.inkm3.yamlconfig.testsupport.n
import com.github.inkm3.yamlconfig.testsupport.s
import com.github.inkm3.yamlconfig.testsupport.sequenceOf
import com.github.inkm3.yamlconfig.testsupport.stringMappingOf
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test

@OptIn(ExperimentalSerializationApi::class)
class YamlIdentityPlannerPolicyTest {
    @Serializable private data class Optional(val name: String = "a", val value: Int = 1)
    @Serializable @YamlIdentityDisabled private data class Disabled(val name: String, val value: Int = 1)
    @Serializable private data class Nullable(val name: String?, val value: Int = 1)
    @Serializable private data class Explicit(val name: String, @YamlIdentity @SerialName("key") val id: Int = 0)
    @Serializable private data class Hidden(@YamlIdentity @EncodeDefault(EncodeDefault.Mode.NEVER) val id: Int = 1,
        val value: Int = 1)

    @Test fun noCandidateKeepsLegacyPositionalUpdate() {
        val user = sequenceOf(stringMappingOf("name" to s("a"), "extra" to s("keep")))
        apply(user, plan(listOf(Optional()), listOf(Optional("b")), user),
            sequenceOf(stringMappingOf("name" to s("b"), "extra" to s("keep"))))
    }
    @Test fun disabledIdentityKeepsLegacyPositionalUpdate() {
        val user = sequenceOf(stringMappingOf("name" to s("a"), "extra" to s("keep")))
        apply(user, plan(listOf(Disabled("a")), listOf(Disabled("b")), user),
            sequenceOf(stringMappingOf("name" to s("b"), "extra" to s("keep"))))
    }
    @Test fun nullableIdentityWithNullValueDoesNotCopyMetadataAcrossChanges() {
        val user = sequenceOf(stringMappingOf("name" to n(), "extra" to s("unknown-owner")))
        val after = listOf(Nullable(null, 2))
        apply(user, plan(listOf(Nullable(null)), after, user), YamlSerialization.Default.encodeToNode(after))
    }
    @Test fun nullableIdentityStillWorksForNonNullValues() {
        val a = stringMappingOf("name" to s("a"), "extra" to s("a"))
        val b = stringMappingOf("name" to s("b"), "extra" to s("b"))
        val user = sequenceOf(a, b)
        apply(user, plan(listOf(Nullable("a"), Nullable("b")), listOf(Nullable("b", 2), Nullable("a")), user),
            sequenceOf(stringMappingOf("name" to s("b"), "value" to i(2), "extra" to s("b")), a))
    }
    @Test fun explicitSerialNameOverridesFirstRequiredName() {
        val a = stringMappingOf("name" to s("a"), "key" to i(1), "extra" to s("a"))
        val b = stringMappingOf("name" to s("b"), "key" to i(2), "extra" to s("b"))
        val user = sequenceOf(a, b)
        apply(user, plan(listOf(Explicit("a", 1), Explicit("b", 2)),
            listOf(Explicit("renamed", 2), Explicit("a", 1)), user),
            sequenceOf(stringMappingOf("name" to s("renamed"), "key" to i(2), "extra" to s("b")), a))
    }
    @Test fun omittedIdentityIsNotInventedFromOtherFields() {
        val user = sequenceOf(stringMappingOf("value" to i(1), "extra" to s("old")))
        val after = listOf(Hidden(value = 2))
        apply(user, plan(listOf(Hidden()), after, user), YamlSerialization.Default.encodeToNode(after))
    }
}
