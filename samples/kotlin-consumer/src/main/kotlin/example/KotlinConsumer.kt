package example

import com.github.inkm3.yamlconfig.save.YamlSaveMode
import com.github.inkm3.yamlconfig.snakeyaml.SnakeYamlEngine
import com.github.inkm3.yamlconfig.source.PathYamlSource
import com.github.inkm3.yamlconfig.source.YamlInput
import com.github.inkm3.yamlconfig.source.YamlMissingFilePolicy
import com.github.inkm3.yamlconfig.yamlConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.StringReader
import java.nio.file.Files

@Serializable
data class ServerConfig(
    @SerialName("server-name") val name: String = "server",
    val port: Int = 25565,
    val servers: List<String> = emptyList(),
)

fun main() {
    val directory = Files.createTempDirectory("yamlconfig-kotlin-consumer-")
    val file = directory.resolve("config.yml")
    try {
        Files.writeString(file, "port: +25565 # user\nplugin-option: keep\n")
        val defaults = object : YamlInput {
            override val description: String = "consumer-defaults"
            override fun openReader(): StringReader = StringReader("server-name: lobby\nport: 30000\n")
        }
        val config = yamlConfig<ServerConfig>(
            engine = SnakeYamlEngine(),
            userSource = PathYamlSource(file),
            defaultsSource = defaults,
            missingFilePolicy = YamlMissingFilePolicy.DO_NOT_CREATE,
            saveMode = YamlSaveMode.PRESERVE_OVERRIDES,
        )
        val session = config.load()
        check(session.value == ServerConfig("lobby", 25565))
        session.value = session.value.copy(port = 40000, servers = listOf("creative"))
        session.save()
        check(config.load().value == session.value)
        val saved = Files.readString(file)
        check("# user" in saved && "plugin-option: keep" in saved)
        session.save()
        check(Files.readString(file) == saved)
        println("PUBLISHED_KOTLIN_CONSUMER_OK")
    } finally {
        Files.deleteIfExists(file)
        Files.deleteIfExists(directory)
    }
}
