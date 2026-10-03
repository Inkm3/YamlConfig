package com.github.inkm3.yamlconfig.save.serialized.identity

import com.github.inkm3.yamlconfig.annotation.YamlIdentity
import com.github.inkm3.yamlconfig.annotation.YamlIdentityDisabled
import com.github.inkm3.yamlconfig.node.YamlMapKey
import com.github.inkm3.yamlconfig.save.internal.serialized.identity.YamlIdentityField
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class YamlIdentityFieldTest {
    @Serializable private data class Item(val id: String, val value: Int)
    @Serializable private data class OptionalFirst(val value: Int = 1, val name: String)
    @Serializable private data class ComplexFirst(val data: List<Int>, val name: String)
    @Serializable private data class AllOptional(val name: String = "a", val value: Int = 1)
    @Serializable private data class Explicit(val name: String, @YamlIdentity @SerialName("entry-id") val id: Int = 1)
    @Serializable @YamlIdentityDisabled private data class Disabled(val id: String)
    @Serializable private data class Multiple(@YamlIdentity val a: String, @YamlIdentity val b: String)
    @Serializable private data class Complex(@YamlIdentity val id: List<Int>)
    @Serializable @YamlIdentityDisabled private data class Conflict(@YamlIdentity val id: String)
    @Serializable private data class RequiredOptional(@Required val id: Int = 1)
    @Serializable @JvmInline private value class Id(val value: String)
    @Serializable private data class InlineFirst(val id: Id, val name: String)
    @Serializable private data class OptionalComplex(val values: List<Int> = emptyList())

    @Test fun firstRequiredScalarIsTheConvention() {
        assertEquals(YamlMapKey("id"), YamlIdentityField.resolve(serializer<Item>().descriptor)?.key)
    }
    @Test fun optionalPropertiesAreSkipped() {
        assertEquals(YamlMapKey("name"), YamlIdentityField.resolve(serializer<OptionalFirst>().descriptor)?.key)
    }
    @Test fun requiredComplexAndInlinePropertiesAreSkipped() {
        assertEquals(YamlMapKey("name"), YamlIdentityField.resolve(serializer<ComplexFirst>().descriptor)?.key)
        assertEquals(YamlMapKey("name"), YamlIdentityField.resolve(serializer<InlineFirst>().descriptor)?.key)
    }
    @Test fun noCandidateLeavesLegacyMatchingAvailable() {
        assertNull(YamlIdentityField.resolve(serializer<AllOptional>().descriptor))
        assertNull(YamlIdentityField.resolve(serializer<OptionalComplex>().descriptor))
        assertNull(YamlIdentityField.resolve(serializer<String>().descriptor))
        assertNull(YamlIdentityField.resolve(serializer<Map<String, Int>>().descriptor))
        assertNull(YamlIdentityField.resolve(serializer<Id>().descriptor))
    }
    @Test fun explicitOptionalFieldOverridesConventionAndUsesSerialName() {
        assertEquals(YamlMapKey("entry-id"), YamlIdentityField.resolve(serializer<Explicit>().descriptor)?.key)
    }
    @Test fun disabledClassDoesNotSelectItsRequiredScalar() {
        assertNull(YamlIdentityField.resolve(serializer<Disabled>().descriptor))
    }
    @Test fun conflictingAnnotationsFailRatherThanGuessing() {
        assertFailsWith<SerializationException> { YamlIdentityField.resolve(serializer<Multiple>().descriptor) }
        assertFailsWith<SerializationException> { YamlIdentityField.resolve(serializer<Conflict>().descriptor) }
    }
    @Test fun explicitlyUnsupportedFieldFailsRatherThanFallingBack() {
        assertFailsWith<SerializationException> { YamlIdentityField.resolve(serializer<Complex>().descriptor) }
    }
    @Test fun descriptorRequirednessNotSourceSyntaxDeterminesImplicitCandidate() {
        assertEquals(YamlMapKey("id"), YamlIdentityField.resolve(serializer<RequiredOptional>().descriptor)?.key)
    }
}
