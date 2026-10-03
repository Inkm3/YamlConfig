package javaapi

import com.github.inkm3.yamlconfig.serialization.YamlSerialization
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

@Serializable
data class JavaServerConfig(
    @SerialName("server-name") val name: String = "server",
    val port: Int = 25565,
    val hosts: List<String> = emptyList(),
)

@Serializable
data class JavaMutableConfig(var port: Int = 1)

data class JavaEndpoint(val value: String)

@Serializable
data class JavaContextualConfig(@Contextual val endpoint: JavaEndpoint)

object JavaApiSerializers {
    @JvmStatic
    fun strings(): KSerializer<List<String>> = ListSerializer(String.serializer())

    @JvmStatic
    fun contextualFormat(): YamlSerialization = YamlSerialization(
        serializersModule = SerializersModule {
            contextual(JavaEndpoint::class, object : KSerializer<JavaEndpoint> {
                override val descriptor = PrimitiveSerialDescriptor("javaapi.Endpoint", PrimitiveKind.STRING)
                override fun serialize(encoder: Encoder, value: JavaEndpoint) = encoder.encodeString(value.value)
                override fun deserialize(decoder: Decoder): JavaEndpoint = JavaEndpoint(decoder.decodeString())
            })
        },
        encodeDefaults = false,
        ignoreUnknownKeys = false,
    )
}
