package com.github.inkm3.yamlconfig.save.serialized

import com.github.inkm3.yamlconfig.node.YamlNode
import com.github.inkm3.yamlconfig.source.YamlOutput
import com.github.inkm3.yamlconfig.spi.YamlEditor
import com.github.inkm3.yamlconfig.spi.YamlEngine
import com.github.inkm3.yamlconfig.testsupport.TestYamlEngine
import com.github.inkm3.yamlconfig.testsupport.TestYamlSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SavedDatabase(val host: String = "localhost", val port: Int = 3306)

@Serializable
internal data class SavedConfig(
    @SerialName("server-name") val name: String = "lobby",
    val port: Int = 25565,
    val database: SavedDatabase = SavedDatabase(host = "prod"),
    val servers: List<String> = emptyList(),
    val limits: Map<String, Int> = emptyMap(),
    val message: String? = "hello",
)

internal class SaveFixture(root: YamlNode?) {
    val source = TestYamlSource(root)
    val delegate = TestYamlEngine()
    var writes = 0
    val engine = object : YamlEngine by delegate {
        override fun write(editor: YamlEditor, output: YamlOutput) {
            writes++
            delegate.write(editor, output)
        }
    }
}
