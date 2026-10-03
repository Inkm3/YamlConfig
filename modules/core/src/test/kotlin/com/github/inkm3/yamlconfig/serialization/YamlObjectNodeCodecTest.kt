package com.github.inkm3.yamlconfig.serialization

import com.github.inkm3.yamlconfig.serialization.CodecFixtures.integer
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.mapping
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.nullNode
import com.github.inkm3.yamlconfig.serialization.CodecFixtures.string
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class YamlObjectNodeCodecTest {
    @Serializable
    private data class Config(
        @SerialName("server-name") val name: String,
        val port: Int = 25565,
        val message: String? = "default",
    )

    @Serializable
    private data class RequiredNullable(val message: String?)

    @Serializable
    private data class RequiredDefault(@Required val port: Int = 25565)

    @Serializable
    private data class Parent(val database: Database = Database(host = "prod"))

    @Serializable
    private data class Database(val host: String = "localhost", val port: Int = 3306)

    @Serializable
    private object Singleton

    @Serializable
    private data class Dependent(val base: Int = 10, val derived: Int = base + 1)

    private val format = YamlSerialization.Default

    @Test
    fun immutableObjectEncodesWithSerialNamesAndDefaults() {
        val value = Config("lobby")
        val node = mapping("server-name" to string("lobby"), "port" to integer(25565), "message" to string("default"))
        assertEquals(node, format.encodeToNode(value))
        assertEquals(value, format.decodeFromNode<Config>(node))
    }

    @Test
    fun fieldsMayBeReadInInputOrderRatherThanDeclarationOrder() {
        val node = mapping("message" to nullNode(), "port" to integer(30000), "server-name" to string("lobby"))
        assertEquals(Config("lobby", 30000, null), format.decodeFromNode<Config>(node))
    }

    @Test
    fun absentOptionalFieldUsesConstructorDefault() {
        assertEquals(Config("lobby"), format.decodeFromNode<Config>(mapping("server-name" to string("lobby"))))
    }

    @Test
    fun requiredFieldCannotBeSuppliedByInventingAValue() {
        assertFailsWith<SerializationException> { format.decodeFromNode<Config>(mapping()) }
        assertFailsWith<SerializationException> { format.decodeFromNode<RequiredNullable>(mapping()) }
        assertFailsWith<SerializationException> { format.decodeFromNode<RequiredDefault>(mapping()) }
    }

    @Test
    fun explicitNullDoesNotUseTheConstructorDefault() {
        val value = format.decodeFromNode<Config>(mapping("server-name" to string("lobby"), "message" to nullNode()))
        assertNull(value.message)
    }

    @Test
    fun missingParentDefaultAndPresentEmptyChildHaveDifferentMeanings() {
        assertEquals(Parent(Database("prod", 3306)), format.decodeFromNode<Parent>(mapping()))
        assertEquals(Parent(Database("localhost", 3306)), format.decodeFromNode<Parent>(mapping("database" to mapping())))
    }

    @Test
    fun dependentConstructorDefaultsUseTheProvidedSiblingValue() {
        assertEquals(Dependent(100, 101), format.decodeFromNode<Dependent>(mapping("base" to integer(100))))
    }

    @Test
    fun singletonObjectUsesEmptyMapping() {
        assertEquals(mapping(), format.encodeToNode(Singleton))
        assertEquals(Singleton, format.decodeFromNode<Singleton>(mapping()))
    }

    @Test
    fun serialNameDoesNotAlsoAcceptTheKotlinPropertyName() {
        assertFailsWith<SerializationException> { format.decodeFromNode<Config>(mapping("name" to string("lobby"))) }
    }
}
